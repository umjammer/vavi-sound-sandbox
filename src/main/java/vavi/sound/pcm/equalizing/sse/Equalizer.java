/*
 * This program(except FFT part) is distributed under LGPL.
 * See LGPL.txt for details.
 *
 * FFT part is a routine made by Mr.Ooura.
 * This routine is a freeware. Contact
 * Mr.Ooura for details of distributing licenses.
 */

package vavi.sound.pcm.equalizing.sse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import vavi.io.LittleEndianDataOutputStream;
import vavi.util.Debug;
import vavi.util.SplitRadixFft;


/**
 * Shibatch Super Equalizer.
 *
 * @author <a href="shibatch@users.sourceforge.net">Naoki Shibata</a>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060207 nsano port to java <br>
 */
class Equalizer {

    /** */
    static class Parameter implements Comparable<Parameter> {
        /** */
        boolean left, right;
        /** */
        double lower, upper, gain, gain2;

        /** */
        Parameter() {
            left = right = true;
            lower = upper = gain = 0;
        }

        /** */
        public String toString() {
            return String.format("%gHz to %gHz, %gdB %c%c", lower, upper, gain, left ? 'L' : ' ', right ? 'R' : ' ');
        }

        /** */
        @Override
        public int compareTo(Parameter o) {
            return (int) (lower - o.lower);
        }
    }

    /** */
    private static final int M = 15;

    /** */
    private static int RINT(double x) {
        return (int) (x >= 0 ? x + 0.5 : x - 0.5);
    }

    /** */
    private static final int DITHERLEN = 65536;

    // play -c 2 -r 44100 -fs -sw

    /** */
    private final double[] fact = new double[M + 1];

    /** */
    private final double aa = 96;

    /** */
    private final double iza;

    /** */
    private double[] lires, lires1, lires2, rires, rires1, rires2, irest;

    /** */
    private double[] fsamples;

    /** */
    private final double[] ditherbuf;

    /** */
    private int ditherptr = 0;

    /** */
    private volatile int chg_ires, cur_ires;

    /** */
    private final int winlen;
    private final int tabsize;
    private int nbufsamples;

    @SuppressWarnings("unused")
    private final int winlenbit;

    /** */
    private int[] inbuf;

    /** */
    private double[] outbuf;

    /** */
    private int maxamp;

    /** */
    boolean enable = true;

    /** */
    int dither = 0;

    /** */
    private static final int NCH = 2;

    /** */
    private static Double[] bands;

    /* */
    static {
        try {
            Properties props = new Properties();
            props.load(Equalizer.class.getResourceAsStream("/vavi/sound/pcm/equalizing/sse/Equalizer.properties"));
            int c = 0;
            List<Double> bandList = new ArrayList<>();
            while (true) {
                String bandString = props.getProperty("band." + c);
                if (bandString == null) {
System.err.println("property band." + c + " not found, break");
                    break;
                }
                bandList.add(Double.parseDouble(bandString));
                c++;
            }
            bands = bandList.toArray(new Double[0]);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    /** */
    public static int getBandsCount() {
        return bands.length;
    }

    /** */
    public static double getBand(int index) {
        return bands[index];
    }

    /** */
    private static double alpha(double a) {
        if (a <= 21) {
            return 0;
        }
        if (a <= 50) {
            return 0.5842 * Math.pow(a - 21, 0.4) + 0.07886 * (a - 21);
        }
        return 0.1102 * (a - 8.7);
    }

    /** */
    private double izero(double x) {
        double ret = 1;

        for (int m = 1; m <= M; m++) {
            double t = Math.pow(x / 2, m) / fact[m];
            ret += t * t;
        }

        return ret;
    }

    /**
     *
     * @param wb window length bits ???
     */
    Equalizer(int wb) {

        winlen = (1 << (wb - 1)) - 1;
        winlenbit = wb;
        tabsize = 1 << wb;
//System.err.println("tablesize: " + tabsize);

        lires1 = new double[tabsize];
        lires2 = new double[tabsize];
        rires1 = new double[tabsize];
        rires2 = new double[tabsize];
        irest = new double[tabsize];
        fsamples = new double[tabsize];
        inbuf = new int[winlen * NCH];
        outbuf = new double[tabsize * NCH];
        ditherbuf = new double[DITHERLEN];

        lires = lires1;
        rires = rires1;
        cur_ires = 1;
        chg_ires = 1;

        for (int i = 0; i < DITHERLEN; i++) {
            ditherbuf[i] = Math.random() - 0.5;
        }

        for (int i = 0; i <= M; i++) {
            fact[i] = 1;
            for (int j = 1; j <= i; j++) {
                fact[i] *= j;
            }
        }

        iza = izero(alpha(aa));
    }

    /** -(N - 1) / 2 <= n <= (N - 1) / 2 */
    private double win(double n, int N) {
        return izero(alpha(aa) * Math.sqrt(1 - 4 * n * n / ((N - 1) * (N - 1)))) / iza;
    }

    /** */
    private static double sinc(double x) {
        return x == 0 ? 1 : Math.sin(x) / x;
    }

    /** */
    private static double hn_lpf(int n, double f, double fs) {
        double t = 1 / fs;
        double omega = 2 * Math.PI * f;
        return 2 * f * t * sinc(n * omega * t);
    }

    /** */
    private static double hn_imp(int n) {
        return n == 0 ? 1.0 : 0.0;
    }

    /**
     * @param param2 TODO index 0 に何か意味あり？？？
     */
    private double hn(int n, List<Parameter> param2, double fs) {
        double ret, lhn;

        lhn = hn_lpf(n, param2.get(0).upper, fs);
        ret = param2.get(0).gain * lhn;

        Parameter e = null;
        for (int i = 1; i < param2.size(); i++) {
            e = param2.get(i);
            if (e.upper < fs / 2) {
                break;
            }
            double lhn2 = hn_lpf(n, e.upper, fs);
            ret += e.gain * (lhn2 - lhn);
            lhn = lhn2;
        }

        ret += e.gain * (hn_imp(n) - lhn);

        return ret;
    }

    /**
     *
     * @param bc
     * @param param input
     * @param param2 output
     * @param fs frequency in Hz
     * @param ch channel #
     */
    static void process_param(double[] bc, List<Parameter> param, List<Parameter> param2, double fs, int ch) {
        Parameter p = null, e2;

        // set gain for each band
        for (int i = 0; i <= bands.length; i++) {
            p = new Parameter();
            p.lower = i == 0 ? 0 : bands[i - 1];
            p.upper = i == bands.length ? fs : bands[i];
            p.gain = bc[i];
            param2.add(p);
Debug.println("0: ch: " + ch + ": [" + i + "]: " + p);
        }

        //
        for (int i = 0; i < param.size(); i++) {
//Debug.println("1: ch: " + ch + ": [" + i + "]");
            Parameter e = param.get(i);
            if ((ch == 0 && !e.left) || (ch == 1 && !e.right)) {
//Debug.println("ch " + ch + ": ignore: unmatched channel");
                continue;
            }
            if (e.lower >= e.upper) {
Debug.println("ch " + ch + ": lower >= upper: " + e.lower + ", " + e.upper);
                continue;
            }

            Iterator<Parameter> pi = param2.iterator();
            while (pi.hasNext()) {
                p = pi.next();
                if (p.upper > e.lower) {
//Debug.println("ch " + ch + ": p.upper > e.lower: " + p.upper + ", " + e.lower);
                    break;
                }
            }

            while (pi.hasNext() && p.lower < e.upper) {
                if (e.lower <= p.lower && p.upper <= e.upper) {
                    p.gain *= Math.pow(10, e.gain / 20);
Debug.println("1.5.1: gain: " + p.gain);
                    p = pi.next();
                    continue;
                }
                if (p.lower < e.lower && e.upper < p.upper) {
                    e2 = new Parameter();
                    e2.lower = e.upper;
                    e2.upper = p.upper;
                    e2.gain = p.gain;
Debug.println("1.5.2: gain: " + p.gain);
                    param2.add(i + 1, e2);

                    e2 = new Parameter();
                    e2.lower = e.lower;
                    e2.upper = e.upper;
                    e2.gain = p.gain * Math.pow(10, e.gain / 20);
Debug.println("1.5.3: gain: " + p.gain);
                    param2.add(i + 1, e2);

                    p.upper = e.lower;

                    p = pi.next();
                    p = pi.next();
                    p = pi.next();
                    continue;
                }
                if (p.lower < e.lower) {
                    e2 = new Parameter();
                    e2.lower = e.lower;
                    e2.upper = p.upper;
                    e2.gain = p.gain * Math.pow(10, e.gain / 20);
Debug.println("1.5.4: gain: " + p.gain);
                    param2.add(i + 1, e2);

                    p.upper = e.lower;

                    p = pi.next();
                    p = pi.next();
                    continue;
                }
                if (e.upper < p.upper) {
                    e2 = new Parameter();
                    e2.lower = e.upper;
                    e2.upper = p.upper;
                    e2.gain = p.gain;
                    param2.add(i + 1, e2);

                    p.upper = e.upper;
                    p.gain = p.gain * Math.pow(10, e.gain / 20);
Debug.println("1.5.5: gain: " + p.gain);

                    p = pi.next();
                    p = pi.next();
                    continue;
                }
                assert false : "impossible";
            }
        }
int i = 0;
for (Parameter pp : param2) {
 Debug.println("2: ch: " + ch + ": [" + (i++) + "]: " + pp);
}
    }

    /**
     *
     * @param bc
     * @param param input
     * @param param2 output
     * @param fs frequency in Hz
     * @param ch channel #
     */
    static void process_param2(double[] bc, List<Parameter> param, List<Parameter> param2, double fs, int ch) {
        Parameter p = null, e2;

        // set gain for each band
        for (int i = 0; i <= bands.length; i++) {
            p = new Parameter();
            p.lower = i == 0 ? 0 : bands[i - 1];
            p.upper = i == bands.length ? fs : bands[i];
            p.gain = bc[i];
            param2.add(p);
Debug.println("0: ch: " + ch + ": [" + i + "]: " + p);
        }

        //
        for (int i = 1; i < param.size() - 1; i++) {
            p = param.get(i);
            e2 = param2.get(i - 1);
            e2.upper = p.upper;
            e2.lower = p.lower;
            e2.gain = p.gain;
        }
int i = 0;
for (Parameter pp : param2) {
 Debug.println("2: ch: " + ch + ": [" + (i++) + "]: " + pp);
}
    }

    /**
     *
     * @param lbc left
     * @param rbc right
     * @param param
     * @param fs frequency in Hz
     */
    void equ_makeTable(double[] lbc, double[] rbc, List<Parameter> param, double fs) {
        int i, cires = cur_ires;
        double[] nires;

        if (fs <= 0) {
            throw new IllegalArgumentException("fs is <= 0");
        }

        // L

        List<Parameter> param2 = new ArrayList<>();

        process_param(lbc, param, param2, fs, 0);

        for (i = 0; i < winlen; i++) {
            irest[i] = hn(i - winlen / 2, param2, fs) * win(i - winlen / 2f, winlen);
        }

        for (; i < tabsize; i++) {
            irest[i] = 0;
        }

        rfft(tabsize, 1, irest);

        nires = cires == 1 ? lires2 : lires1;

        for (i = 0; i < tabsize; i++) {
            nires[i] = irest[i];
        }

        // R

        param2.clear();

        process_param(rbc, param, param2, fs, 1);

        for (i = 0; i < winlen; i++) {
            irest[i] = hn(i - winlen / 2, param2, fs) * win(i - winlen / 2f, winlen);
        }

        for (; i < tabsize; i++) {
            irest[i] = 0;
        }

        rfft(tabsize, 1, irest);

        nires = cires == 1 ? rires2 : rires1;

        for (i = 0; i < tabsize; i++) {
            nires[i] = irest[i];
        }

        //

        chg_ires = cires == 1 ? 2 : 1;
    }

    /** */
    void equ_quit() {
        lires1 = null;
        lires2 = null;
        rires1 = null;
        rires2 = null;
        irest = null;
        fsamples = null;
        inbuf = null;
        outbuf = null;

        rfft(0, 0, null);
    }

    /** */
    void equ_clearbuf(int bps, int srate) {
        nbufsamples = 0;
        for (int i = 0; i < tabsize * NCH; i++) {
            outbuf[i] = 0;
        }
    }

    /** */
    private double hm1 = 0;
//    /** */
//    private double hm2 = 0;

    /**
     * @param buf PCM sample.  8, 16 and 24 bits are available.
     * @param nsamples
     * @param nch number of channels
     * @param bps bits per sample
     */
    int equ_modifySamples(byte[] buf, int nsamples, int nch, int bps) {
        int i, p, ch;
        double[] ires;
        int amax = (1 << (bps - 1)) - 1;
        int amin = -(1 << (bps - 1));

        if (chg_ires != 0) {
            cur_ires = chg_ires;
            lires = cur_ires == 1 ? lires1 : lires2;
            rires = cur_ires == 1 ? rires1 : rires2;
            chg_ires = 0;
        }

        p = 0;
//Debug.println("bps: " + bps);
        while (nbufsamples + nsamples >= winlen) {
            switch (bps) {
            case 8:
                for (i = 0; i < (winlen - nbufsamples) * nch; i++) {
                    inbuf[nbufsamples * nch + i] = (buf[i + p * nch] & 0xff) - 0x80;
                    double s = outbuf[nbufsamples * nch + i];
                    if (dither != 0) {
                        double u;
                        s -= hm1;
                        u = s;
                        s += ditherbuf[(ditherptr++) & (DITHERLEN - 1)];
                        if (s < amin) {
                            s = amin;
                        }
                        if (amax < s) {
                            s = amax;
                        }
                        s = RINT(s);
                        hm1 = s - u;
                        buf[i + p * nch] = (byte) (s + 0x80);
                    } else {
                        if (s < amin) {
                            s = amin;
                        }
                        if (amax < s) {
                            s = amax;
                        }
                        buf[i + p * nch] = (byte) (RINT(s) + 0x80);
                    }
                }
                for (i = winlen * nch; i < tabsize * nch; i++) {
                    outbuf[i - winlen * nch] = outbuf[i];
                }

                break;

            case 16:
                for (i = 0; i < (winlen - nbufsamples) * nch; i++) {
                    inbuf[nbufsamples * nch + i] = readShort(buf, i + p * nch);
                    double s = outbuf[nbufsamples * nch + i];
                    if (dither != 0) {
                        double u;
                        s -= hm1;
                        u = s;
                        s += ditherbuf[(ditherptr++) & (DITHERLEN - 1)];
                        if (s < amin) {
                            s = amin;
                        }
                        if (amax < s) {
                            s = amax;
                        }
                        s = RINT(s);
                        hm1 = s - u;
                        writeShort(buf, i + p * nch, (int) s);
                    } else {
                        if (s < amin) {
                            s = amin;
                        }
                        if (amax < s) {
                            s = amax;
                        }
                        writeShort(buf, i + p * nch, RINT(s));
                    }
                }
                for (i = winlen * nch; i < tabsize * nch; i++) {
                    outbuf[i - winlen * nch] = outbuf[i];
                }

                break;

            case 24:
                for (i = 0; i < (winlen - nbufsamples) * nch; i++) {
                    inbuf[nbufsamples * nch + i] = (buf[(i + p * nch) * 3] & 0xff) + ((buf[(i + p * nch) * 3 + 1] & 0xff) << 8) + ((buf[(i + p * nch) * 3 + 2] & 0xff) << 16);

                    double s = outbuf[nbufsamples * nch + i];
//                    if (dither != 0) {
//                        s += ditherbuf[(ditherptr++) & (DITHERLEN - 1)];
//                    }
                    if (s < amin) {
                        s = amin;
                    }
                    if (amax < s) {
                        s = amax;
                    }
                    int s2 = RINT(s);
                    buf[(i + p * nch) * 3] = (byte) (s2 & 0xff);
                    s2 >>= 8;
                    buf[(i + p * nch) * 3 + 1] = (byte) (s2 & 0xff);
                    s2 >>= 8;
                    buf[(i + p * nch) * 3 + 2] = (byte) (s2 & 0xff);
                }
                for (i = winlen * nch; i < tabsize * nch; i++) {
                    outbuf[i - winlen * nch] = outbuf[i];
                }

                break;

            default:
                assert false;
            }

            p += winlen - nbufsamples;
            nsamples -= winlen - nbufsamples;
            nbufsamples = 0;

            for (ch = 0; ch < nch; ch++) {
                ires = ch == 0 ? lires : rires;

                if (bps == 24) {
                    for (i = 0; i < winlen; i++) {
                        fsamples[i] = inbuf[nch * i + ch];
                    }
                } else {
                    for (i = 0; i < winlen; i++) {
                        fsamples[i] = inbuf[nch * i + ch];
                    }
                }

                for (i = winlen; i < tabsize; i++) {
                    fsamples[i] = 0;
                }

                if (enable) {
                    rfft(tabsize, 1, fsamples);

                    fsamples[0] = ires[0] * fsamples[0];
                    fsamples[1] = ires[1] * fsamples[1];

                    for (i = 1; i < tabsize / 2; i++) {
                        double re, im;

                        re = ires[i * 2] * fsamples[i * 2] - ires[i * 2 + 1] * fsamples[i * 2 + 1];
                        im = ires[i * 2 + 1] * fsamples[i * 2] + ires[i * 2] * fsamples[i * 2 + 1];

                        fsamples[i * 2] = re;
                        fsamples[i * 2 + 1] = im;
                    }

                    rfft(tabsize, -1, fsamples);
                } else {
                    for (i = winlen - 1 + winlen / 2; i >= winlen / 2; i--) {
                        fsamples[i] = fsamples[i - winlen / 2] * tabsize / 2;
                    }
                    for (; i >= 0; i--) {
                        fsamples[i] = 0;
                    }
                }

                for (i = 0; i < winlen; i++) {
                    outbuf[i * nch + ch] += fsamples[i] / tabsize * 2;
                }

                for (i = winlen; i < tabsize; i++) {
                    outbuf[i * nch + ch] = fsamples[i] / tabsize * 2;
                }
            }
        }

        switch (bps) {
        case 8:
            for (i = 0; i < nsamples * nch; i++) {
                inbuf[nbufsamples * nch + i] = (buf[i + p * nch] & 0xff) - 0x80;
                double s = outbuf[nbufsamples * nch + i];
                if (dither != 0) {
                    double u;
                    s -= hm1;
                    u = s;
                    s += ditherbuf[(ditherptr++) & (DITHERLEN - 1)];
                    if (s < amin) {
                        s = amin;
                    }
                    if (amax < s) {
                        s = amax;
                    }
                    s = RINT(s);
                    hm1 = s - u;
                    buf[i + p * nch] = (byte) (s + 0x80);
                } else {
                    if (s < amin) {
                        s = amin;
                    }
                    if (amax < s) {
                        s = amax;
                    }
                    buf[i + p * nch] = (byte) (RINT(s) + 0x80);
                }
            }
            break;

        case 16:
            for (i = 0; i < nsamples * nch; i++) {
                inbuf[nbufsamples * nch + i] = readShort(buf, i + p * nch);
                double s = outbuf[nbufsamples * nch + i];
                if (dither != 0) {
                    double u;
                    s -= hm1;
                    u = s;
                    s += ditherbuf[(ditherptr++) & (DITHERLEN - 1)];
                    if (s < amin) {
                        s = amin;
                    }
                    if (amax < s) {
                        s = amax;
                    }
                    s = RINT(s);
                    hm1 = s - u;
                    writeShort(buf, i + p * nch, (int) s);
                } else {
                    if (s < amin) {
                        s = amin;
                    }
                    if (amax < s) {
                        s = amax;
                    }
                    writeShort(buf, i + p * nch, RINT(s));
                }
            }
            break;

        case 24:
            for (i = 0; i < nsamples * nch; i++) {
                inbuf[nbufsamples * nch + i] = (buf[(i + p * nch) * 3] & 0xff) + ((buf[(i + p * nch) * 3 + 1] & 0xff) << 8) + ((buf[(i + p * nch) * 3 + 2] & 0xff) << 16);

                double s = outbuf[nbufsamples * nch + i];
//                if (dither != 0) {
//                    s += ditherbuf[(ditherptr++) & (DITHERLEN - 1)];
//                }
                if (s < amin) {
                    s = amin;
                }
                if (amax < s) {
                    s = amax;
                }
                int s2 = RINT(s);
                buf[(i + p * nch) * 3] = (byte) (s2 & 0xff);
                s2 >>= 8;
                buf[(i + p * nch) * 3 + 1] = (byte) (s2 & 0xff);
                s2 >>= 8;
                buf[(i + p * nch) * 3 + 2] = (byte) (s2 & 0xff);
            }
            break;

        default:
            assert false;
        }

        p += nsamples;
        nbufsamples += nsamples;

        return p;
    }

    /**
     *
     * @param n table size
     * @param isign
     * @param x
     */
    private static void rfft(int n, int isign, double[] x) {
        int ipsize = 0, wsize = 0;
        int[] ip = null;
        double[] w = null;
        int newipsize, newwsize;

        if (n == 0) {
            ip = null;
            ipsize = 0;
            w = null;
            wsize = 0;
//System.err.println("n is zero");
//new Exception().printStackTrace(System.err);
            return;
        }

        newipsize = (int) (2 + Math.sqrt(n / 2d));
        if (newipsize > ipsize) {
            ipsize = newipsize;
            ip = new int[ipsize];
            ip[0] = 0;
        }

        newwsize = n / 2;
        if (newwsize > wsize) {
            wsize = newwsize;
            w = new double[wsize];
        }

        SplitRadixFft.rdft(n, isign, x, ip, w);
    }

    /** when bps = 16 */
    private static void writeShort(byte[] buffer, int offset, int value) {
        // assume little endian
        buffer[offset * 2    ] = (byte)  (value       & 0xff);
        buffer[offset * 2 + 1] = (byte) ((value >> 8) & 0xff);
    }

    /** when bps = 16 */
    private static int readShort(byte[] buffer, int offset) {
        // assume little endian
        int v =  (buffer[offset * 2    ] & 0xff) |
                ((buffer[offset * 2 + 1] & 0xff) << 8);
        if ((v & 0x8000) != 0) { // PCM signed
            v -= 0x10000;
        }
        return v;
    }

    /** */
    static void usage() {
        System.err.println("java in out preamp");
    }

    /**
     * @param argv 0: in, 1: out, 2: preamp
     */
    public static void main(String[] argv) throws Exception {
System.setOut(new PrintStream(System.getProperty("dev.null"))); // fuckin' j-ogg
        // max 0 ~ 96 min, [0] is preamp
        int[] lslpos = new int[19], rslpos = new int[19];

        Properties props = new Properties();
        props.load(Equalizer.class.getResourceAsStream("/vavi/sound/pcm/equalizing/sse/local.properties"));

        for (int i = 0; i < 18; i++) {
            lslpos[i + 1] = Integer.parseInt(props.getProperty("lslpos." + i));
            rslpos[i + 1] = Integer.parseInt(props.getProperty("rslpos." + i));
        }

        //----

        // min0 ~ 1.0 max ???
        double[] lbands = new double[19];
        double[] rbands = new double[19];
        lbands[18] = Math.pow(10, 0 / -20.0);
        rbands[18] = Math.pow(10, 0 / -20.0);
        List<Parameter> params = new ArrayList<>();

        double lpreamp = lslpos[0] == 96 ? 0 : Math.pow(10, lslpos[0] / -20.0);
        double rpreamp = rslpos[0] == 96 ? 0 : Math.pow(10, rslpos[0] / -20.0);

Debug.println("---- init ----");
        for (int i = 0; i < 18; i++) {
            //
            Parameter param = new Parameter();
            lbands[i] = lslpos[i + 1] == 96 ? 0 : lpreamp * Math.pow(10, lslpos[i + 1] / -20.0);
            param.left = true;
            param.right = false;
            param.gain = lbands[i];
            param.lower = bands[i];
            param.upper = i == bands.length - 1 ? -1 : bands[i + 1];
Debug.println(param);
            params.add(param);
            //
            rbands[i] = rslpos[i + 1] == 96 ? 0 : rpreamp * Math.pow(10, rslpos[i + 1] / -20.0);
            param.left = false;
            param.right = true;
            param.gain = rbands[i];
            param.lower = bands[i];
            param.upper = i == bands.length - 1 ? -1 :  bands[i + 1];
Debug.println(param);
            params.add(param);
        }
Debug.println("---- init ----");

        //----

        InputStream fpi = null;
        RandomAccessFile fpo = null;
        byte[] buf = new byte[576 * 2 * 2];

        Equalizer equ = new Equalizer(14);
        equ.equ_makeTable(lbands, rbands, params, 44100);

        int argc = argv.length;
        if (argc != 2 && argc != 3) {
            Equalizer.usage();
            return;
        }

        try {
            fpi = Files.newInputStream(Paths.get(argv[0]));
            fpo = new RandomAccessFile(argv[1], "rw");
        } catch (Exception e) {
            System.err.println(e);
            Equalizer.usage();
            return;
        }

        // generate wav header
        LittleEndianDataOutputStream leos = new LittleEndianDataOutputStream(new RAOutputStream(fpo));
        {
            short word;
            int dword;

            fpo.writeBytes("RIFF");
            dword = 0;
            leos.writeInt(dword);

            fpo.writeBytes("WAVEfmt ");
            dword = 16;
            leos.writeInt(dword);
            word = 1;
            leos.writeShort(word); // format category, PCM
            word = 2;
            leos.writeShort(word); // channels
            dword = 44100;
            leos.writeInt(dword); // sampling rate
            dword = 44100 * 2 * 2;
            leos.writeInt(dword); // bytes per sec
            word = 4;
            leos.writeShort(word); // block alignment
            word = 16;
            leos.writeShort(word); // ???

            fpo.writeBytes("data");
            dword = 0;
            leos.writeInt(dword);
        }

        int preamp = 65536;

        if (argc == 3) {
            preamp = 32767 * 65536 / Integer.parseInt(argv[2]);
            System.err.printf("preamp = %d\n", preamp);
        }

        while (true) {
            int n, m;

            n = fpi.read(buf, 0, 576 * 2 * 2);
            if (n <= 0) {
                break;
            }
            m = equ.equ_modifySamples(buf, n / 4, 2, 16);
            fpo.write(buf, 0, 4 * m);
        }

        {
            int dword;
            int len = (int) fpo.getFilePointer();

            fpo.seek(4);
            dword = len - 8;
            leos.writeInt(dword);

            fpo.seek(40);
            dword = len - 44;
            leos.writeInt(dword);
        }
        leos.close();

        if (equ.maxamp != 0) {
            System.err.printf("maxamp = %d\n", equ.maxamp);
        }

        equ.equ_quit();
    }

    /** */
    private static final class RAOutputStream extends OutputStream {
        RandomAccessFile raf;
        public RAOutputStream(RandomAccessFile raf) throws IOException {
            this.raf = raf;
        }
        /** */
        @Override
        public void write(int b) throws IOException {
            raf.write(b);
        }
    }
}

/* */
