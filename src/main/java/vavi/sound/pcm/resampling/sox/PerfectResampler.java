/*
 * Copyright (c) 2008 robs@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package vavi.sound.pcm.resampling.sox;

import java.util.Arrays;
import java.util.logging.Level;

import vavi.util.Debug;
import vavi.util.I0Bessel;
import vavi.util.SplitRadixFft;


/**
 * change sample rate
 *
 * Based upon the techniques described in `The Quest For The Perfect Resampler'
 * by Laurent De Soras
 *
 * @see "http://ldesoras.free.fr/doc/articles/resampler-en.pdf"
 * @see "https://github.com/rbouqueau/SoX/blob/8025dd7861959189fc0abaade8d5be47244034da/src/rate.c"
 */
public class PerfectResampler {

    /** */
    private final void coef(double[] coef_p, int interp_order, int fir_len, int phase_num, int coef_interp_num, int fir_coef_num, double value) {
        coef_p[(fir_len) * ((interp_order) + 1) * (phase_num) + ((interp_order) + 1) * (fir_coef_num) + (interp_order - coef_interp_num)] = value;
    }

    /** */
    private final double coef(double[] coef_p, int interp_order, int fir_len, int phase_num, int coef_interp_num, int fir_coef_num) {
        return coef_p[(fir_len) * ((interp_order) + 1) * (phase_num) + ((interp_order) + 1) * (fir_coef_num) + (interp_order - coef_interp_num)];
    }

    /** */
    private final void coef_coef1(int x, double value, double[] result, int interp_order, int num_coefs, int i, int j) {
        coef(result, interp_order, num_coefs, j, x, num_coefs - 1 - i, value);
    }

    /** */
    private double[] prepare_coefs(final double[] coefs, int num_coefs, int num_phases, int interp_order, int multiplier) {
        int length = num_coefs * num_phases;
        double[] result = new double[length * (interp_order + 1)];
        double fm1 = coefs[0];
        double f1 = 0;
        double f2 = 0;

        for (int i = num_coefs - 1; i >= 0; --i) {
            for (int j = num_phases - 1; j >= 0; --j) {
                double f0 = fm1;
                double b = 0;
                double c = 0;
                double d = 0; // = 0 to kill compiler warning
                int pos = i * num_phases + j - 1;
Debug.printf(Level.FINE, "coefs:%d, index:%d\n", coefs.length, pos - 1);
                fm1 = (pos > 0 ? coefs[pos - 1] : 0) * multiplier;
                switch (interp_order) {
                case 1:
                    b = f1 - f0;
                    break;
                case 2:
                    b = f1 - (.5 * (f2 + f0) - f1) - f0;
                    c = .5 * (f2 + f0) - f1;
                    break;
                case 3:
                    c = .5 * (f1 + fm1) - f0;
                    d = (1 / 6.) * (f2 - f1 + fm1 - f0 - 4 * c);
                    b = f1 - f0 - d - c;
                    break;
                default:
                    if (interp_order != 0) {
                        assert false;
                    }
                }
                coef_coef1(0, f0, result, interp_order, num_coefs, i, j);
                if (interp_order > 0) {
                    coef_coef1(1, b, result, interp_order, num_coefs, i, j);
                }
                if (interp_order > 1) {
                    coef_coef1(2, c, result, interp_order, num_coefs, i, j);
                }
                if (interp_order > 2) {
                    coef_coef1(3, d, result, interp_order, num_coefs, i, j);
                }
                f2 = f1;
                f1 = f0;
            }
        }
        return result;
    }

    /** not half-band as in symmetric about Fn/2 (Fs/4) */
    static class HalfBand {
        int dft_length;
        int num_taps;
        int[] post_peak = new int[1];
        double[] coefs;
    }

    /** Data that are shared between channels and filters */
    static class RateShared {
        double[] poly_fir_coefs;
        /** [0]: halve; [1]: down/up: halve/double */
        HalfBand[] half_band = { new HalfBand(), new HalfBand() };
        /** For Ooura fft */
        double[] sin_cos_table;
        /** ditto */
        int[] bit_rev_table;
    }

    /** */
    interface StageFunction {
        void exec(Stage stage, Fifo fifo);
    }

    /** */
    static class Stage {
        static final double MULT32 = 65536. * 65536.;
        //
        class Union {
            int integer;
            int fraction;
            long all() {
                return integer << 32 | (fraction & 0xffff);
//                return fraction << 32 | (integer & 0xffff);
            }
            void all(long v) {
                integer = (int) ((v >> 32) & 0xffff);
                fraction = (int) v & 0xffff;
//                fraction = (int) ((v >> 32) & 0xffff);
//                integer = (int) v & 0xffff;
            }
        }
        RateShared shared;
        Fifo fifo;
        /** Number of past samples to store */
        int pre;
        /** pre + number of future samples to store */
        int pre_post;
        /** Number of zero samples to pre-load the fifo */
        int preload;
        /** Which of the 2 half-band filters to use */
        int which;
        /** For poly_fir & spline: */
        StageFunction fn;
        /** 32bit.32bit fixed point arithmetic */
        Union at, step;
        /** For step: > 1 for rational; 1 otherwise */
        int divisor;
        double out_in_ratio;
        Stage() {
            shared = new RateShared();
            at = new Union();
            step = new Union();
        }
    }

    /** */
    private final int stage_occupancy(Stage s) {
        return Math.max(0, s.fifo.occupancy() - s.pre_post);
    }

    /** */
    private final int stage_read_p(Stage s) {
        return s.fifo.read_ptr() + s.pre;
    }

    /** */
    StageFunction cubic_spline = new StageFunction() {
        public void exec(Stage stage, Fifo output_fifo) {
            int i;
            int num_in = stage_occupancy(stage);
            int max_num_out = (int) (1 + num_in * stage.out_in_ratio);
            final int inputP = stage_read_p(stage);
            int outputP = output_fifo.reserve(max_num_out);

            for (i = 0; stage.at.integer < num_in; ++i, stage.at.all(stage.at.all() + stage.step.all())) {
                int s = inputP + stage.at.integer; // input
                double[] input = stage.fifo.data;
                double[] output = output_fifo.data;
                double x = stage.at.fraction * (1 / Stage.MULT32);
                double b = .5 * (input[s + 1] + input[s - 1]) - input[s];
                double a = (1 / 6.) * (input[s + 2] - input[s + 1] + input[s - 1] - input[s] - 4 * b);
                double c = input[s + 1] - input[s] - a - b;
                output[outputP + i] = ((a * x + b) * x + c) * x + input[s];
            }
            assert max_num_out - i >= 0;
            output_fifo.trim_by(max_num_out - i);
            stage.fifo.read(stage.at.integer, null);
            stage.at.integer = 0;
        }
    };

    /** */
    StageFunction half_sample = new StageFunction() {
        public void exec(Stage stage, Fifo output_fifo) {
            double[] output;
            int i, j;
            int num_in = Math.max(0, stage.fifo.occupancy());
            final RateShared s = stage.shared;
            final HalfBand f = s.half_band[stage.which];
            final int overlap = f.num_taps - 1;

            while (num_in >= f.dft_length) {
                final int inputP = stage.fifo.read_ptr();
                stage.fifo.read(f.dft_length - overlap, null);
                final double[] input = new double[inputP + f.dft_length - overlap];
                num_in -= f.dft_length - overlap;

                int outputP = output_fifo.reserve(f.dft_length);
                output = output_fifo.data;
                output_fifo.trim_by((f.dft_length + overlap) >> 1);
//Debug.printf("%d, %d, %d, %d, %d\n", input.length, inputP, output.length, outputP, f.dft_length);
                System.arraycopy(input, inputP, output, outputP, Math.min(f.dft_length, input.length - inputP)); // TODO added min

double[] o = new double[f.dft_length];
System.arraycopy(output, outputP, o, 0, f.dft_length);
if (s.bit_rev_table == null) {
 s.bit_rev_table = new int[dft_br_len(f.dft_length)];
 s.sin_cos_table = new double[dft_sc_len(f.dft_length)];
}
//Debug.printf("%d, %s\n", f.dft_length, s.bit_rev_table.length);

                SplitRadixFft.rdft(f.dft_length, 1, o, s.bit_rev_table, s.sin_cos_table);
                o[0] *= f.coefs[0];
                o[1] *= f.coefs[1];
                for (i = 2; i < f.dft_length; i += 2) {
                    double tmp = o[i];
                    o[i] = f.coefs[i] * tmp - f.coefs[i + 1] * o[i + 1];
                    o[i + 1] = f.coefs[i + 1] * tmp + f.coefs[i] * o[i + 1];
                }
                SplitRadixFft.rdft(f.dft_length, -1, o, s.bit_rev_table, s.sin_cos_table);

                for (j = 1, i = 2; i < f.dft_length - overlap; ++j, i += 2) {
                    o[j] = o[i];
                }

System.arraycopy(o, 0, output, outputP, f.dft_length);
            }
        }
    };

    /** */
    StageFunction double_sample = new StageFunction() {
        public void exec(Stage stage, Fifo output_fifo) {
            double[] output;
            int i, j;
            int num_in = Math.max(0, stage.fifo.occupancy());
            final RateShared s = stage.shared;
            final HalfBand f = s.half_band[1];
            final int overlap = f.num_taps - 1;

            while (num_in > f.dft_length >> 1) {
                final int inputP = stage.fifo.read_ptr();
                double[] input = stage.fifo.data;
                stage.fifo.read((f.dft_length - overlap) >> 1, null);
                num_in -= (f.dft_length - overlap) >> 1;

                int outputP = output_fifo.reserve(f.dft_length);
                output = output_fifo.data;
                output_fifo.trim_by(overlap);
                for (j = i = 0; i < f.dft_length; ++j, i += 2) {
                    output[outputP + i] = input[inputP + j];
                    output[outputP + i + 1] = 0;
                }

double[] o = new double[f.dft_length];
System.arraycopy(output, outputP, o, 0, f.dft_length);
if (s.bit_rev_table == null) {
 s.bit_rev_table = new int[dft_br_len(f.dft_length)];
 s.sin_cos_table = new double[dft_sc_len(f.dft_length)];
}
Debug.printf("%d, %s\n", f.dft_length, s.bit_rev_table);

                SplitRadixFft.rdft(f.dft_length, 1, o, s.bit_rev_table, s.sin_cos_table);
                o[0] *= f.coefs[0];
                o[1] *= f.coefs[1];
                for (i = 2; i < f.dft_length; i += 2) {
                    double tmp = o[i];
                    o[i] = f.coefs[i] * tmp - f.coefs[i + 1] * o[i + 1];
                    o[i + 1] = f.coefs[i + 1] * tmp + f.coefs[i] * o[i + 1];
                }
                SplitRadixFft.rdft(f.dft_length, -1, o, s.bit_rev_table, s.sin_cos_table);

System.arraycopy(o, 0, output, outputP, f.dft_length);
            }
        }
    };

    /** */
    private double[] make_lpf(int num_taps, double Fc, double beta, double scale) {
        double[] h = new double[num_taps];
        double sum = 0;
        int i, m = num_taps - 1;
        assert Fc >= 0 && Fc <= 1;
        for (i = 0; i <= m / 2; ++i) {
            double x = Math.PI * (i - .5 * m), y = 2. * i / m - 1;
            h[i] = x != 0 ? Math.sin(Fc * x) / x : Fc;
            sum += h[i] *= I0Bessel.value(beta * Math.sqrt(1 - y * y));
            if (m - i != i) {
                sum += h[m - i] = h[i];
            }
        }
        for (i = 0; i < num_taps; ++i) {
            h[i] *= scale / sum;
        }
        return h;
    }

    static final double TO_6dB = .5869;
    static final double TO_3dB = (2 / 3.) * (.5 + TO_6dB);
    static final double MAX_TBW0 = 36.;
    static final double MAX_TBW0A = MAX_TBW0 / (1 + TO_3dB);
    static final double MAX_TBW3 = Math.floor(MAX_TBW0 * TO_3dB);
    static final double MAX_TBW3A = Math.floor(MAX_TBW0A * TO_3dB);

    /**
     * End of pass-band; ~= 0.01dB point Start of stop-band Nyquist freq; e.g.
     * 0.5, 1, PI Stop-band attenuation in dB (Single phase.) 0: value will be
     * estimated Number of phases; 0 for single-phase
     */
    private double[] design_lpf(double Fp,
                                double Fc,
                                double Fn,
                                boolean allow_aliasing,
                                double att,
                                int[] num_taps,
                                int k) {
        double tr_bw, beta;

        if (allow_aliasing) {
            Fc += (Fc - Fp) * TO_3dB;
        }
        Fp /= Fn;
        Fc /= Fn; // Normalise to Fn = 1
        tr_bw = TO_6dB * (Fc - Fp); // Transition band-width: 6dB to stop points

        if (num_taps[0] == 0) { // TODO this could be cleaner, esp. for k != 0
            double n160 = (.0425 * att - 1.4) / tr_bw; // Half order for att = 160
            int n = (int) (n160 * (16.556 / (att - 39.6) + .8625) + .5); // For att [80,160)
            num_taps[0] = k != 0 ? 2 * n : 2 * (n + (n & 1)) + 1; // =1 %4 (0 phase 1/2 band)
        }
        assert att >= 80;
        beta = att < 100 ? .1102 * (att - 8.7) : .1117 * att - 1.11;
        if (k != 0) {
            num_taps[0] = num_taps[0] * k - 1;
        } else {
            k = 1;
        }
        return make_lpf(num_taps[0], (Fc - tr_bw) / k, beta, k);
    }

    /** */
    private final int dft_br_len(int l) {
        return 2 + (1 << (int) (Math.log(l / 2 + .5) / Math.log(2.)) / 2);
    }

    /** */
    private final int dft_sc_len(int l) {
        return l / 2;
    }

    /** */
    private void fir_to_phase(RateShared rateShared,
                              double[][] h,
                              int[] len,
                              int[] post_len,
                              double phase0) {
        double[] work;
        double phase = (phase0 > 50 ? 100 - phase0 : phase0) / 50;
        int work_len, begin, end;
        int peak = 0;
        int i = len[0];

        for (work_len = 32; i > 1; work_len <<= 1, i >>= 1) {
            ;
        }
        if (rateShared.bit_rev_table == null) {
            rateShared.bit_rev_table = new int[dft_br_len(2 * work_len)];
            rateShared.sin_cos_table = new double[dft_sc_len(2 * work_len)];
        }
        work = new double[work_len];
        for (i = 0; i < len[0]; ++i) {
            work[i] = h[0][i];
        }

        SplitRadixFft.rdft(work_len, 1, work, rateShared.bit_rev_table, rateShared.sin_cos_table); // Cepstrum:
        work[0] = Math.log(Math.abs(work[0]));
        work[1] = Math.log(Math.abs(work[1]));
        for (i = 2; i < work_len; i += 2) {
            work[i] = Math.log(Math.sqrt(Math.pow(work[i], 2) + Math.pow(work[i + 1], 2)));
            work[i + 1] = 0;
        }
        SplitRadixFft.rdft(work_len, -1, work, rateShared.bit_rev_table, rateShared.sin_cos_table);
        for (i = 0; i < work_len; ++i) {
            work[i] *= 2. / work_len;
        }
        for (i = 1; i < work_len / 2; ++i) { // Window to reject acausal components
            work[i] *= 2;
            work[i + work_len / 2] = 0;
        }
        SplitRadixFft.rdft(work_len, 1, work, rateShared.bit_rev_table, rateShared.sin_cos_table);
        // Some DFTs require phase unwrapping here, but rdft seems not to.

        for (i = 2; i < work_len; i += 2) {
            // Interpolate between linear & min phase
            work[i + 1] = phase * Math.PI * .5 * i + (1 - phase) * work[i + 1];
        }

        work[0] = Math.exp(work[0]);
        work[1] = Math.exp(work[1]);
        for (i = 2; i < work_len; i += 2) {
            double x = Math.exp(work[i]);
            work[i] = x * Math.cos(work[i + 1]);
            work[i + 1] = x * Math.sin(work[i + 1]);
        }
        SplitRadixFft.rdft(work_len, -1, work, rateShared.bit_rev_table, rateShared.sin_cos_table);
        for (i = 0; i < work_len; ++i) {
            work[i] *= 2. / work_len;
        }

        for (i = 1; i < work_len; ++i) {
            if (work[i] > work[peak]) { // Find peak.
                peak = i; // N.B. peak > 0
            }
        }

        if (phase == 0) {
            begin = 0;
        } else if (phase == 1) {
            begin = 1 + (work_len - len[0]) / 2;
        } else {
            if (peak < work_len / 4) { // Low phases can wrap impulse, so unwrap:
                System.arraycopy(work, 0, work, work_len / 4, work_len / 2);
                System.arraycopy(work, work_len * 3 / 4, work, 0, work_len / 4);
                peak += work_len / 4;
            }
            begin = (int) ((.997 - (2 - phase) * .22) * len[0] + .5);
            end = (int) ((.997 + (0 - phase) * .22) * len[0] + .5);
            begin = peak - begin - (begin & 1);
            end = peak + 1 + end + (end & 1);
            len[0] = end - begin;
            h[0] = new double[len[0]];
        }
        for (i = 0; i < len[0]; ++i) {
            h[0][i] = work[begin + (phase0 > 50 ? len[0] - 1 - i : i)];
        }
        post_len[0] = begin + len[0] - (peak + 1);
    }

    /** */
    private final int range_limit(int x, int lower, int upper) {
        return Math.min(Math.max(x, lower), upper);
    }

    /** Set to 4 x nearest power of 2 */
    private int set_dft_length(int num_taps) {
        int result, n = num_taps;
        for (result = 8; n > 2; result <<= 1, n >>= 1) {
            ;
        }
        result = range_limit(result, 4096, 131072);
        assert (num_taps * 2 < result);
        return result;
    }

    /** */
    private void half_band_filter_init(RateShared rateShared,
                                       /* unsigned */int which,
                                       int[] num_taps,
                                       final double h[],
                                       double Fp,
                                       double atten,
                                       int multiplier,
                                       double phase,
                                       boolean allow_aliasing) {
        HalfBand f = rateShared.half_band[which];
        int dft_length, i;

        if (f.num_taps != 0) {
            return;
        }
        if (h != null) {
            dft_length = set_dft_length(num_taps[0]);
            f.coefs = new double[dft_length];
            for (i = 0; i < num_taps[0]; ++i) {
                f.coefs[(i + dft_length - num_taps[0] + 1) & (dft_length - 1)] = h[Math.abs(num_taps[0] / 2 - i)] / dft_length * 2 * multiplier;
            }
            f.post_peak[0] = num_taps[0] / 2;
        } else {
            // Empirical adjustment to negate att degradation with intermediate phase
            double att = phase != 0 && phase != 50 && phase != 100 ? atten * (34. / 33) : atten;

            double[][] h_ = new double[1][];
            h_[0] = design_lpf(Fp, 1., 2., allow_aliasing, att, num_taps, 0);

            if (phase != 50) {
                fir_to_phase(rateShared, h_, num_taps, f.post_peak, phase);
            } else {
                f.post_peak[0] = num_taps[0] / 2;
            }

            dft_length = set_dft_length(num_taps[0]);
            f.coefs = new double[dft_length];
            for (i = 0; i < num_taps[0]; ++i) {
                f.coefs[(i + dft_length - num_taps[0] + 1) & (dft_length - 1)] = h_[0][i] / dft_length * 2 * multiplier;
            }
        }
        assert (num_taps[0] & 1) != 0;
        f.num_taps = num_taps[0];
        f.dft_length = dft_length;
Debug.printf("fir_len=%d dft_length=%d Fp=%f atten=%f mult=%d\n", num_taps[0], dft_length, Fp, atten, multiplier);
        if (rateShared.bit_rev_table == null) {
            rateShared.bit_rev_table = new int[dft_br_len(dft_length)];
            rateShared.sin_cos_table = new double[dft_sc_len(dft_length)];
        }
        SplitRadixFft.rdft(dft_length, 1, f.coefs, rateShared.bit_rev_table, rateShared.sin_cos_table);
    }

    /** */
    static class Rate {
        double factor;
        int samples_in;
        int samples_out;
        int level;
        int input_stage_num;
        int output_stage_num;
        boolean upsample;
        Stage[] stages;
    }

    /** */
    enum Quality {
        Default(-1),
        Quick(0),
        Low(1),
        Medium(2),
        High(3),
        Very(4),
        Ultra(5);
        int value;
        Quality(int value) {
            this.value = value;
        }
    }

    /** */
    static class Filter {
        int[] len = new int[1];
        double[] h;
        double bw, a;
        Filter(int len, double[] h, double bw, double a) {
            this.len[0] = len;
            this.h = h;
            this.bw = bw;
            this.a = a;
        }
    }

    /** */
    static class PolyFir1 {
        int phase_bits;
        StageFunction fn;
        PolyFir1(int phase_bits, StageFunction fn) {
            this.phase_bits = phase_bits;
            this.fn = fn;
        }
    }

    /** */
    static class PolyFir {
        int num_coefs;
        double pass;
        double stop;
        double att;
        PolyFir1[] interp = new PolyFir1[4];
        PolyFir(int num_coefs, double pass, double stop, double att, PolyFir1[] interp) {
            this.num_coefs = num_coefs;
            this.pass = pass;
            this.stop = stop;
            this.att = att;
            this.interp = interp;
        }
    }

    /** */
    static final double[] half_fir_coefs_25 = {
        4.9866643051942178e-001, 3.1333582318860204e-001, 1.2567743716165585e-003,
       -9.2035726038137103e-002, -1.0507348255277846e-003, 4.2764945027796687e-002,
        7.7661461450703555e-004, -2.0673365323361139e-002, -5.0429677622613805e-004,
        9.4223774565849357e-003, 2.8491539998284476e-004, -3.8562347294894628e-003,
       -1.3803431143314762e-004, 1.3634218103234187e-003, 5.6110366313398705e-005,
       -3.9872042837864422e-004, -1.8501044952475473e-005, 9.0580351350892191e-005,
        4.6764104835321042e-006, -1.4284332593063177e-005, -8.1340436298087893e-007,
        1.1833367010222812e-006, 7.3979325233687461e-008,
    };

    /** */
    static final double[] half_fir_coefs_low = {
        4.2759802493108773e-001, 3.0939308096100709e-001, 6.9285325719540158e-002,
        -8.0642059355533674e-002, -6.0528749718348158e-002, 2.5228940037788555e-002,
        4.7756850372993369e-002, 8.7463256642532057e-004, -3.3208422093026498e-002,
        -1.3425983316344854e-002, 1.9188320662637096e-002, 1.7478840713827052e-002,
        -7.5527851809344612e-003, -1.6145235261724403e-002, -6.3013968965413430e-004,
        1.1965551091184719e-002, 5.1714613100614501e-003, -6.9898749683755968e-003,
        -6.6150222806158742e-003, 2.6394681964090937e-003, 5.9365183404658526e-003,
        3.5567920638016650e-004, -4.2031898513566123e-003, -1.8738555289555877e-003,
        2.2991238738122328e-003, 2.2058519188488186e-003, -7.7796582498205363e-004,
        -1.8212814627239918e-003, -1.4964619042558244e-004, 1.1706370821176716e-003,
        5.3082071395224866e-004, -5.6771020453353900e-004, -5.4472363026668942e-004,
        1.5914542178505357e-004, 3.8911127354338085e-004, 4.2076035174603683e-005,
        -2.1015548483049000e-004, -9.5381290156278399e-005, 8.0903081108059553e-005,
        7.5812875822003258e-005, -1.5004304266040688e-005, -3.9149443482028750e-005,
        -6.0893901283459912e-006, 1.4040363940567877e-005, 4.9834316581482789e-006,
    };

    /**
     * rate half fir
     *
     * Down-sample by a factor of 2 using a FIR with odd length (LEN).
     * Input must be preceded and followed by LEN >> 1 samples.
     */
    abstract class RateHalfFir implements StageFunction {
        abstract double[] COEFS();
        abstract int CONVOLVE();
        public void exec(Stage p, Fifo output_fifo) {
            int inputP = stage_read_p(p);
            final double[] input = p.fifo.data;
            int num_out = (stage_occupancy(p) + 1) / 2;
            int outputP = output_fifo.reserve(num_out);
            double[] output = output_fifo.data;

            for (int i = 0; i < num_out; ++i, inputP += 2) {
                int j = 1;
                double sum = input[inputP + 0] * COEFS()[0];
                for (int k = 0; k < CONVOLVE(); k ++) {
                    sum += (input[inputP - j] + input[inputP + j]) * COEFS()[j];
                    ++j;
                }
                assert j == COEFS().length;
                output[outputP + i] = sum;
            }
            p.fifo.read(2 * num_out, null);
        }
    }

    /**
     * rate poly fir0
     *
     * Up-sample by rational step in (0,1) using a poly-phase FIR, length LEN.
     * Input must be preceded by LEN >> 1 samples.
     * Input must be followed by (LEN-1) >> 1 samples.
     */
    abstract class RatePolyFir0 implements StageFunction {
        abstract int FIR_LENGTH();
        abstract int CONVOLVE();
        public void exec(Stage p, Fifo output_fifo) {
            final int inputP = stage_read_p(p);
            double[] input = p.fifo.data;
            int i;
            int num_in = stage_occupancy(p);
            int max_num_out = (int) (1 + num_in * p.out_in_ratio);
Debug.printf("%d, %d, %.2f\n", num_in, max_num_out, p.out_in_ratio);
            int outputP = output_fifo.reserve(max_num_out);
            double[] output = output_fifo.data;

//Debug.printf("%d, %d\n", output.length, outputP);
            for (i = 0; p.at.integer < num_in * p.divisor; ++i, p.at.integer += p.step.integer) {
                int quot = p.at.integer / p.divisor;
                int rem = p.at.integer % p.divisor;
                final int atP = inputP + quot; // input
                double sum = 0;
                int j = 0;
                for (int k = 0; k < CONVOLVE(); k++) {
                    sum += (coef(p.shared.poly_fir_coefs, 0, FIR_LENGTH(), rem, 0, j)) * input[atP + j];
                    ++j;
                }
                assert j == FIR_LENGTH();
                output[outputP + i] = sum;
            }
            assert max_num_out - i >= 0;
            output_fifo.trim_by(max_num_out - i);
            int quot = p.at.integer / p.divisor;
            p.fifo.read(quot, null);
            p.at.integer -= quot * p.divisor;
        }
    }

    /**
     * rate poly fir
     *
     * Up-sample by step in (0,1) using a poly-phase FIR with length LEN.
     * Input must be preceded by LEN >> 1 samples.
     * Input must be followed by (LEN-1) >> 1 samples.
     */
    abstract class RatePolyFir implements StageFunction {
        abstract int CONVOLVE();
        abstract int COEF_INTERP();
        abstract int FIR_LENGTH();
        abstract int PHASE_BITS();
        public void exec(Stage p, Fifo output_fifo) {
            int inputP = stage_read_p(p);
            final double[] input = p.fifo.data;
            int i;
            int num_in = stage_occupancy(p);
            int max_num_out = (int) (1 + num_in * p.out_in_ratio);
            int outputP = output_fifo.reserve(max_num_out);
            double[] output = output_fifo.data;

            for (i = 0; p.at.integer < num_in; ++i, p.at.all(p.at.all() + p.step.all())) {
                final int atP = p.at.integer; // input
                int fraction = p.at.fraction;
                int phase = fraction >> (32 - PHASE_BITS()); // high-order bits
                double x = 0;
                double sum = 0;
                if (COEF_INTERP() > 0) { // low-order bits, scaled to [0,1)
                    x = (fraction << PHASE_BITS()) * (1 / Stage.MULT32);
                } else {
                    sum = 0;
                }
                int j = 0;
                for (int k = 0; k < CONVOLVE(); k++) {
                    if (COEF_INTERP() == 0) {
                        double a = coef(p.shared.poly_fir_coefs, COEF_INTERP(), FIR_LENGTH(), phase, 0, j);
                        sum += a * input[inputP + atP + j];
                        ++j;
                    } else if (COEF_INTERP() == 1) {
                        double a = coef(p.shared.poly_fir_coefs, COEF_INTERP(), FIR_LENGTH(), phase, 0, j);
                        double b = coef(p.shared.poly_fir_coefs, COEF_INTERP(), FIR_LENGTH(), phase, 1, j);
                        sum += (b * x + a) * input[inputP + atP + j];
                        ++j;
                    } else if (COEF_INTERP() == 2) {
                        double a = coef(p.shared.poly_fir_coefs, COEF_INTERP(), FIR_LENGTH(), phase, 0, j);
                        double b = coef(p.shared.poly_fir_coefs, COEF_INTERP(), FIR_LENGTH(), phase, 1, j);
                        double c = coef(p.shared.poly_fir_coefs, COEF_INTERP(), FIR_LENGTH(), phase, 2, j);
                        sum += ((c * x + b) * x + a) * input[inputP + atP + j];
                        ++j;
                    } else if (COEF_INTERP() == 3) {
                        double a = coef(p.shared.poly_fir_coefs, COEF_INTERP(), FIR_LENGTH(), phase, 0, j);
                        double b = coef(p.shared.poly_fir_coefs, COEF_INTERP(), FIR_LENGTH(), phase, 1, j);
                        double c = coef(p.shared.poly_fir_coefs, COEF_INTERP(), FIR_LENGTH(), phase, 2, j);
                        double d = coef(p.shared.poly_fir_coefs, COEF_INTERP(), FIR_LENGTH(), phase, 3, j);
                        sum += (((d * x + c) * x + b) * x + a) * input[inputP + atP + j];
                        ++j;
                    } else {
                        assert false: "COEF_INTERP";
                    }
                }
                assert j == FIR_LENGTH();
                output[outputP + i] = sum;
            }
            assert max_num_out - i >= 0;
            output_fifo.trim_by(max_num_out - i);
            p.fifo.read(p.at.integer, null);
            p.at.integer = 0;
        }
    }

    // assert_static(!((array_length(COEFS)- 1) & 1), HALF_FIR_LENGTH_25 );
    StageFunction half_sample_25 = new RateHalfFir() {
        // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
        int CONVOLVE() { return 22; }
        double[] COEFS() { return half_fir_coefs_25; }
    };

    // assert_static(!((array_length(COEFS)- 1) & 1), HALF_FIR_LENGTH_low);
    StageFunction half_sample_low = new RateHalfFir() {
        // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
        int CONVOLVE() { return 44; }
        double[] COEFS() { return half_fir_coefs_low; }
    };

    static final int d100_l = 16;
    // poly_fir_convolve_d100 _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    StageFunction d100_0 = new RatePolyFir0() {
        int FIR_LENGTH() { return d100_l; }
        int CONVOLVE() { return 16; }
    };

    StageFunction d100_1 = new RatePolyFir() {
        int COEF_INTERP() { return 1; }
        int PHASE_BITS() { return 9; }
        int FIR_LENGTH() { return d100_l; }
        int CONVOLVE() { return 16; }
    };

    static final int d100_1_b = 9;
    StageFunction d100_2 = new RatePolyFir() {
        int COEF_INTERP() { return 2; }
        int PHASE_BITS() { return 7; }
        int FIR_LENGTH() { return d100_l; }
        int CONVOLVE() { return 16; }
    };

    static final int d100_2_b = 7;
    StageFunction d100_3 = new RatePolyFir() {
        int COEF_INTERP() { return 3; }
        int PHASE_BITS() { return 6; }
        int FIR_LENGTH() { return d100_l; }
        int CONVOLVE() { return 16; }
    };

    static final int d100_3_b = 6;
    static final int d120_l = 30;
    // poly_fir_convolve_d120 _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    StageFunction d120_0 = new RatePolyFir0() {
        int FIR_LENGTH() { return d120_l; }
        int CONVOLVE() { return 30; }
    };

    StageFunction d120_1 = new RatePolyFir() {
        int COEF_INTERP() { return 1; }
        int PHASE_BITS() { return 10; }
        int FIR_LENGTH() { return d120_l; }
        int CONVOLVE() { return 30; }
    };

    static final int d120_1_b = 10;
    StageFunction d120_2 = new RatePolyFir() {
        int COEF_INTERP() { return 2; }
        int PHASE_BITS() { return 9; }
        int FIR_LENGTH() { return d120_l; }
        int CONVOLVE() { return 30; }
    };

    static final int  d120_2_b = 9;
    StageFunction d120_3 = new RatePolyFir() {
        int COEF_INTERP() { return 3; }
        int PHASE_BITS() { return 7; }
        int FIR_LENGTH() { return d120_l; }
        int CONVOLVE() {  return 30; }
    };

    static final int d120_3_b = 7;
    static final int d150_l = 38;
//    poly_fir_convolve_d150 _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    StageFunction d150_0 = new RatePolyFir0() {
        int FIR_LENGTH() { return d150_l; }
        int CONVOLVE() { return 38; }
    };

    StageFunction d150_1 = new RatePolyFir() {
        int COEF_INTERP() { return 1; }
        int PHASE_BITS() { return 12; }
        int FIR_LENGTH() { return d150_l; }
        int CONVOLVE() { return 38; }
    };

    static final int d150_1_b = 12;
    StageFunction d150_2 = new RatePolyFir() {
        int COEF_INTERP() { return 2; }
        int PHASE_BITS() { return 10; }
        int FIR_LENGTH() { return d150_l; }
        int CONVOLVE() { return 38; }
    };

    static final int d150_2_b = 10;
    StageFunction d150_3 = new RatePolyFir() {
        int COEF_INTERP() { return 3; }
        int PHASE_BITS() { return 8; }
        int FIR_LENGTH() { return d150_l; }
        int CONVOLVE() { return 38; }
    };

    static final int d150_3_b = 8;
    static final int U100_l = 42;
    // poly_fir_convolve_U100 _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    StageFunction U100_0 = new RatePolyFir0() {
        int FIR_LENGTH() { return U100_l; }
        int CONVOLVE() { return 42; }
    };

    StageFunction U100_1 = new RatePolyFir() {
        int COEF_INTERP() { return 1; }
        int PHASE_BITS() { return 10; }
        int FIR_LENGTH() { return U100_l; }
        int CONVOLVE() { return 42; }
    };

    static final int U100_1_b = 10;
    StageFunction U100_2 = new RatePolyFir() {
        int COEF_INTERP() { return 2; }
        int PHASE_BITS() { return 8; }
        int FIR_LENGTH() { return U100_l; }
        int CONVOLVE() { return 42; }
    };

    static final int U100_2_b = 8;
    StageFunction U100_3 = new RatePolyFir() {
        int COEF_INTERP() { return 3; }
        int PHASE_BITS() { return 6; }
        int FIR_LENGTH() { return U100_l; }
        int CONVOLVE() { return 42; }
    };

    static final int U100_3_b = 6;
    static final int u100_l = 10;
//     poly_fir_convolve_u100 _ _ _ _ _ _ _ _ _ _
    StageFunction u100_0 = new RatePolyFir0() {
        int FIR_LENGTH() { return u100_l; }
        int CONVOLVE() { return 10; }
    };

    StageFunction u100_1 = new RatePolyFir() {
        int COEF_INTERP() { return 1; }
        int PHASE_BITS() { return 9; }
        int FIR_LENGTH() { return u100_l; }
        int CONVOLVE() { return 10; }
    };

    static final int u100_1_b = 9;
    StageFunction u100_2 = new RatePolyFir() {
        int COEF_INTERP() { return 2; }
        int PHASE_BITS() { return 7; }
        int FIR_LENGTH() { return u100_l; }
        int CONVOLVE() { return 10; }
    };

    static final int u100_2_b = 7;
    StageFunction u100_3 = new RatePolyFir() {
        int COEF_INTERP() { return 3; }
        int PHASE_BITS() { return 6; }
        int FIR_LENGTH() { return u100_l; }
        int CONVOLVE() { return 10; }
    };

    static final int u100_3_b = 6;
    static final int u120_l = 14;
    // poly_fir_convolve_u120 _ _ _ _ _ _ _ _ _ _ _ _ _ _
    StageFunction u120_0 = new RatePolyFir0() {
        int FIR_LENGTH() { return u120_l; }
        int CONVOLVE() { return 14; }
    };

    StageFunction u120_1 = new RatePolyFir() {
        int COEF_INTERP() { return 1; }
        int PHASE_BITS() { return 10; }
        int FIR_LENGTH() { return u120_l; }
        int CONVOLVE() { return 14; }
    };

    static final int u120_1_b = 10;
    StageFunction u120_2 = new RatePolyFir() {
        int COEF_INTERP() { return 2; }
        int PHASE_BITS() { return 8; }
        int FIR_LENGTH() { return u120_l; }
        int CONVOLVE() { return 14; }
    };

    static final int u120_2_b = 8;
    StageFunction u120_3 = new RatePolyFir() {
        int COEF_INTERP() { return 3; }
        int PHASE_BITS() { return 6; }
        int FIR_LENGTH() { return u120_l; }
        int CONVOLVE() { return 14; }
    };

    static final int u120_3_b = 6;
    static final int u150_l = 20;
    // poly_fir_convolve_u150 _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    StageFunction u150_0 = new RatePolyFir0() {
        int FIR_LENGTH() { return u150_l; }
        int CONVOLVE() { return 20; }
    };

    StageFunction u150_1 = new RatePolyFir() {
        int COEF_INTERP() { return 1; }
        int PHASE_BITS() { return 11; }
        int FIR_LENGTH() { return u150_l; }
        int CONVOLVE() { return 20; }
    };

    static final int  u150_1_b = 11;
    StageFunction u150_2 = new RatePolyFir() {
        int COEF_INTERP() { return 2; }
        int PHASE_BITS() { return 9; }
        int FIR_LENGTH() { return u150_l; }
        int CONVOLVE() { return 20; }
    };

    static final int u150_2_b = 9;
    StageFunction u150_3 = new RatePolyFir() {
        int COEF_INTERP() { return 3; }
        int PHASE_BITS() { return 7; }
        int FIR_LENGTH() { return u150_l; }
        int CONVOLVE() { return 20; }
    };

    static final int u150_3_b = 7;

    /** */
    final PolyFir[] poly_firs = {
        new PolyFir(d100_l, .75, 1.5, 108, new PolyFir1[] { new PolyFir1(0, d100_0), new PolyFir1(d100_1_b, d100_1), new PolyFir1(d100_2_b, d100_2), new PolyFir1(d100_3_b, d100_3) }),
        new PolyFir(d120_l, 1, 1.5, 133, new PolyFir1[] { new PolyFir1(0, d120_0), new PolyFir1(d120_1_b, d120_1), new PolyFir1(d120_2_b, d120_2), new PolyFir1(d120_3_b, d120_3) }),
        new PolyFir(d150_l, 1, 1.5, 165, new PolyFir1[] { new PolyFir1(0, d150_0), new PolyFir1(d150_1_b, d150_1), new PolyFir1(d150_2_b, d150_2), new PolyFir1(d150_3_b, d150_3) }),
        new PolyFir(U100_l, .724, 1, 105, new PolyFir1[] { new PolyFir1(0, U100_0), new PolyFir1(U100_1_b, U100_1), new PolyFir1(U100_2_b, U100_2), new PolyFir1(U100_3_b, U100_3) }),
        new PolyFir(u100_l, .3, 1.5, 107, new PolyFir1[] { new PolyFir1(0, u100_0), new PolyFir1(u100_1_b, u100_1), new PolyFir1(u100_2_b, u100_2), new PolyFir1(u100_3_b, u100_3) }),
        new PolyFir(u120_l, .5, 1.5, 125, new PolyFir1[] { new PolyFir1(0, u120_0), new PolyFir1(u120_1_b, u120_1), new PolyFir1(u120_2_b, u120_2), new PolyFir1(u120_3_b, u120_3) }),
        new PolyFir(u150_l, .5, 1.5, 174, new PolyFir1[] { new PolyFir1(0, u150_0), new PolyFir1(u150_1_b, u150_1), new PolyFir1(u150_2_b, u150_2), new PolyFir1(u150_3_b, u150_3) })
    };

    /** */
    private void rate_init(Rate rate,
                           RateShared shared,
                           double factor,
                           Quality quality,
                           int interp_order,
                           double phase,
                           double bandwidth,
                           boolean allow_aliasing) {
        int i;
        int mult;
        int divisor = 1;

        assert (factor > 0);
        rate.factor = factor;
        if (quality.value < Quality.Quick.value || quality.value > Quality.Ultra.value) {
            quality = Quality.High;
        }
        if (quality != Quality.Quick) {
            final int max_divisor = 2048; // Keep coef table size ~< 500kb
            final double epsilon = 4 / Stage.MULT32; // Scaled to half this at max_divisor
            rate.upsample = rate.factor < 1;
            for (i = (int) factor, rate.level = 0; i != 0; ++rate.level, i >>= 1) {
                ; // log base 2
            }
            factor /= 1 << ((rate.level + (rate.upsample ? 1 : 0) == 0) ? 1 : 0);
            for (i = 2; i <= max_divisor && divisor == 1; ++i) {
                double try_d = factor * i;
                int try_ = (int) (try_d + .5);
                if (Math.abs(try_ - try_d) < try_ * epsilon * (1 - (.5 / max_divisor) * i)) {
                    if (try_ == i) { // Rounded to 1:1?
                        factor = 1;
                        divisor = 2;
                        rate.upsample = false;
                    } else {
                        factor = try_;
                        divisor = i;
                    }
                }
            }
        }
        rate.stages = new Stage[rate.level + 4];
for (int x = rate.level + 1 + 1 + 1; x < rate.level + 4; x++) {
    rate.stages[x] = new Stage();
}
        for (i = 0; i <= rate.level + 1 + 1; ++i) {
            rate.stages[i] = new Stage();
            rate.stages[i].shared = shared;
        }
        rate.stages[rate.level + 1].step.all((long) (factor * Stage.MULT32 + .5));
        rate.stages[rate.level + 1].out_in_ratio = Stage.MULT32 * divisor / rate.stages[rate.level + 1].step.all();
Debug.printf("out_in_ratio: %.2f, divisor: %d, step.all: %d", rate.stages[rate.level + 1].out_in_ratio, divisor, rate.stages[rate.level + 1].step.all());

        if (divisor != 1) {
            assert rate.stages[rate.level + 1].step.fraction == 0;
        } else if (quality != Quality.Quick) {
            assert rate.stages[rate.level + 1].step.integer == 0;
        }
Debug.printf("i/o=%f; %.9f:%d @ level %d\n", rate.factor, factor, divisor, rate.level);

        mult = 1 + (rate.upsample ? 1 : 0); // Compensate for zero-stuffing in double_sample
        rate.input_stage_num = -(rate.upsample ? 1 : 0);
        rate.output_stage_num = rate.level;
        if (quality == Quality.Quick) {
            ++rate.output_stage_num;
            rate.stages[rate.level + 1].fn = cubic_spline;
            rate.stages[rate.level + 1].pre_post = Math.max(3, rate.stages[rate.level + 1].step.integer);
            rate.stages[rate.level + 1].preload = rate.stages[rate.level + 1].pre = 1;
        } else if (rate.stages[rate.level + 1].out_in_ratio != 2 || (rate.upsample && quality == Quality.Low)) {
            final PolyFir f;
            final PolyFir1 f1;
            int n = 4 * (rate.upsample ? 1: 0) + range_limit(quality.value, Quality.Medium.value, Quality.Very.value) - Quality.Medium.value;
            if (interp_order < 0) {
                interp_order = quality.value > Quality.High.value ? 1 : 0;
            }
            interp_order = divisor == 1 ? 1 + interp_order : 0;
            rate.stages[rate.level + 1].divisor = divisor;
            rate.output_stage_num += 2;
            if (rate.upsample && quality == Quality.Low) {
                mult = 1;
                ++rate.input_stage_num;
                --rate.output_stage_num;
                --n;
            }
            f = poly_firs[n];
            f1 = f.interp[interp_order];
            if (rate.stages[rate.level + 1].shared.poly_fir_coefs == null) {
                int[] num_taps = { 0 };
                int phases = divisor == 1 ? (1 << f1.phase_bits) : divisor;
                double[] coefs = design_lpf(f.pass, f.stop, 1., false, f.att, num_taps, phases);
                assert num_taps[0] == f.num_coefs * phases - 1;
                rate.stages[rate.level + 1].shared.poly_fir_coefs = prepare_coefs(coefs, f.num_coefs, phases, interp_order, mult);
Debug.printf("fir_len=%d phases=%d coef_interp=%d mult=%d size=%d\n", f.num_coefs, phases, interp_order, mult, 0/*sigfigs3((num_taps[0] + 1) * (interp_order + 1) * 8)*/); // * 8 double
            }
            rate.stages[rate.level + 1].fn = f1.fn;
            rate.stages[rate.level + 1].pre_post = f.num_coefs - 1;
            rate.stages[rate.level + 1].pre = 0;
            rate.stages[rate.level + 1].preload = rate.stages[rate.level + 1].pre_post >> 1;
            mult = 1;
        }
        if (quality.value > Quality.Low.value) {
            /* static */final Filter[] filters = {
                new Filter(2 * half_fir_coefs_low.length - 1, half_fir_coefs_low, 0, 0),
                new Filter(0, null, .986, 1109),
                new Filter(0, null, .986, 125),
                new Filter(0, null, .986, 170),
                new Filter(0, null, .996, 170),
            };
            final Filter f = filters[quality.value - Quality.Low.value];
            double att = allow_aliasing ? (34. / 33) * f.a : f.a;
            double bw = bandwidth != 0 ? 1 - (1 - bandwidth / 100) / TO_3dB : f.bw;
            double min = 1 - (allow_aliasing ? MAX_TBW0A : MAX_TBW0) / 100;
            assert (quality.value - Quality.Low.value < filters.length);
            half_band_filter_init(shared, rate.upsample ? 1 : 0, f.len, f.h, bw, att, mult, phase, allow_aliasing);
            if (rate.upsample) {
                rate.stages[-1].fn = double_sample; // Finish off setting up pre-stage
                rate.stages[-1].preload = shared.half_band[1].post_peak[0] >> 1;
                // Start setting up post-stage; TODO don't use dft for short filters
                if ((1 - rate.factor) / (1 - bw) > 2) {
                    half_band_filter_init(shared, 0, new int[] { 0 }, null, Math.max(rate.factor, min), att, 1, phase, allow_aliasing);
                } else {
                    shared.half_band[0] = shared.half_band[1];
                }
            } else if (rate.level > 0 && rate.output_stage_num > rate.level) {
                double pass = bw * divisor / factor / 2;
                if ((1 - pass) / (1 - bw) > 2) {
                    half_band_filter_init(shared, 1, new int[] { 0 }, null, Math.max(pass, min), att, 1, phase, allow_aliasing);
                }
            }
            rate.stages[rate.level + 1 + 1].fn = half_sample;
            rate.stages[rate.level + 1 + 1].preload = shared.half_band[0].post_peak[0];
        } else if (quality == Quality.Low && !rate.upsample) { // dft is slower here, so
            rate.stages[rate.level + 1 + 1].fn = half_sample_low; // use normal convolution
            rate.stages[rate.level + 1 + 1].pre_post = 2 * (half_fir_coefs_low.length - 1);
            rate.stages[rate.level + 1 + 1].preload = rate.stages[rate.level + 1 + 1].pre = rate.stages[rate.level + 1 + 1].pre_post >> 1;
        }
        if (rate.level > 0) {
            Stage s = rate.stages[rate.level + 1 - 1];
            if (shared.half_band[1].num_taps != 0) {
                s.fn = half_sample;
                s.preload = shared.half_band[1].post_peak[0];
                s.which = 1;
            } else {
                s = rate.stages[rate.level + 1 + 1];
            }
        }
        for (i = rate.input_stage_num; i <= rate.output_stage_num; ++i) {
            Stage s = rate.stages[i + 1];
            if (i >= 0 && i < rate.level - 1) {
                s.fn = half_sample_25;
                s.pre_post = 2 * (half_fir_coefs_25.length - 1);
                s.preload = s.pre = s.pre_post >> 1;
            }
            s.fifo = new Fifo();
            int p = s.fifo.reserve(s.preload);
            Arrays.fill(s.fifo.data, p, p + s.preload, 0);
            if (i < rate.output_stage_num) {
Debug.printf("stage=%-3dpre_post=%-3dpre=%-3dpreload=%d\n", i, s.pre_post, s.pre, s.preload);
            }
        }
    }

    /** */
    private void rate_process(Rate p) {
        int stage = p.input_stage_num; // p.stages
        for (int i = p.input_stage_num; i < p.output_stage_num; ++i, ++stage) {
            p.stages[stage + 1].fn.exec(p.stages[stage + 1], p.stages[stage + 1 + 1].fifo);
        }
    }

    /** */
    private int rate_input(Rate p, final double[] samples, int n) {
        p.samples_in += n;
//Debug.println("write: " + n);
        return p.stages[p.input_stage_num + 1].fifo.write(n, samples);
    }

    /** */
    private final int rate_output(Rate p, double[] samples, int[] n) {
        Fifo fifo = p.stages[p.output_stage_num + 1].fifo;
        n[0] = Math.min(n[0], fifo.occupancy());
        p.samples_out += n[0];
        return fifo.read(n[0], samples);
    }

    /** */
    private void rate_flush(Rate p) {
        Fifo fifo = p.stages[p.output_stage_num + 1].fifo;
        int samples_out = (int) (p.samples_in / p.factor + .5);
        int remaining = samples_out - p.samples_out;
        double[] buff = new double[1024];

        if (remaining > 0) {
            while (fifo.occupancy() < remaining) {
                rate_input(p, buff, 1024);
                rate_process(p);
            }
            fifo.trim_to(remaining);
            p.samples_in = 0;
        }
    }

    // SoX Wrapper

    /** */
    static class Priv {
        float out_rate;
        Quality quality;
        double coef_interp, phase, bandwidth;
        boolean allow_aliasing;
        Rate rate;
        RateShared shared;
        int shared_ptr;
        Priv() {
            rate = new Rate();
            shared = new RateShared();
        }
    }

    /** */
    private Priv priv;

    /**
     * "+i:b:p:MILaqlmhvu"
     *
     * @param coef_interp 1 ~ 3
     * @param phase 0 ~ 100 'M': 0, 'I': 25, 'L': 50
     * @param bandwidth 100 - MAX_TBW3 ~ 99.7
     * @param allow_aliasing
     * @param quality
     */
    public PerfectResampler(int coef_interp, int phase, double bandwidth, boolean allow_aliasing, int quality, float in_rate, float out_rate) {
        this.priv = new Priv();
        priv.quality = Quality.Default;
        priv.phase = 25;
        priv.shared_ptr = 0; // p.shared

        priv.allow_aliasing = allow_aliasing;
        priv.quality = Quality.values()[quality + 1];

        if (priv.quality.value < 2 && (priv.bandwidth != 0 || priv.phase != 25 || priv.allow_aliasing)) {
            throw new IllegalArgumentException("override options not allowed with this quality level");
        }

        if (priv.bandwidth != 0 && priv.bandwidth < 100 - MAX_TBW3A && priv.allow_aliasing) {
            throw new IllegalArgumentException("minimum allowed bandwidth with aliasing is " + (100 - MAX_TBW3A));
        }

        priv.out_rate = out_rate;

        if (in_rate == priv.out_rate) {
            throw new IllegalArgumentException("same rate");
        }

        rate_init(priv.rate, priv.shared, (double) in_rate / out_rate, priv.quality, (int) priv.coef_interp - 1, priv.phase, priv.bandwidth, priv.allow_aliasing);
    }

    /**
     *
     * @param isamp [out]
     * @param osamp [out]
     */
    public void flow(final int[] ibuf, int[] obuf, int[] isamp, int[] osamp) {
        int i;
        int[] odone = { osamp[0] };

        int sP = rate_output(priv.rate, null, odone);
Debug.println("odone: " + odone[0]);
        final double[] s = priv.rate.stages[priv.rate.output_stage_num + 1].fifo.data;
        int obufP = 0;
        for (i = 0; i < odone[0]; ++i) {
            obuf[obufP++] = (int) Math.round(s[sP++]);
        }

        if (isamp[0] != 0 && odone[0] < osamp[0]) {
            int tP = rate_input(priv.rate, null, isamp[0]);
            double[] t = priv.rate.stages[priv.rate.input_stage_num + 1].fifo.data;
            int ibufP = 0;
            for (i = isamp[0]; i != 0; --i) {
                t[tP++] = ibuf[ibufP++];
            }
            rate_process(priv.rate);
        } else {
            isamp[0] = 0;
        }
        osamp[0] = odone[0];
    }

    /**
     *
     * @param osamp [out]
     */
    public void drain(int[] obuf, int[] osamp) {
        final int[] isamp = { 0 };
        rate_flush(priv.rate);
        flow(null, obuf, isamp, osamp);
    }

    // "rate",
    // "[-q|-l|-m|-h|-v] [-p PHASE|-M|-I|-L] [-b BANDWIDTH] [-a] [RATE[k]]" +
    // "\n\n\tQuality\t\tPhase\tBW %   Rej dB\tTypical Use" +
    // "\n -q\tquick & dirty\tLin.\tn/a  ~30 @ Fs/4\tplayback on ancient hardware"
    // +
    // "\n -l\tlow\t\t\"\t80\t100\tplayback on old hardware" +
    // "\n -m\tmedium\t\tInt.\t99\t100\taudio playback" +
    // "\n -h\thigh\t\t\"\t99\t125\t16-bit master (use with dither)" +
    // "\n -v\tvery high\t\"\t99\t175\t24-bit master" +
    // "\n\nOverrides (for -m, -h, -v):" +
    // "\n -p 0-100\t0=minimum, 25=intermediate, 50=linear, 100=maximum" +
    // "\n -M/I/L\t\tphase=min./int./lin." +
    // "\n -b 74-99.7\t%" +
    // "\n -a\t\tallow aliasing"
}

/* */
