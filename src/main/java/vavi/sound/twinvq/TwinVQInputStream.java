/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import vavi.util.Debug;

import static vavi.sound.SoundUtil.volume;


/**
 * TwinVQInputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070202 initial version <br>
 */
public class TwinVQInputStream extends FilterInputStream {

    /** このクラスで取得するストリームのバイトオーダー */
    private ByteOrder byteOrder;

    /**
     */
    public TwinVQInputStream(InputStream in,
                             int samplesPerBlock,
                             int channels,
                             int blockSize)
        throws IOException {

        this(in,
             samplesPerBlock,
             channels,
             blockSize,
             ByteOrder.BIG_ENDIAN);
    }

    /**
     */
    public TwinVQInputStream(InputStream in,
                             int samplesPerBlock,
                             int channels,
                             int blockSize,
                             ByteOrder byteOrder)
        throws IOException {

        super(new PipedInputStream());

        this.byteOrder = byteOrder;
Debug.println("byteOrder: " + this.byteOrder);

//Debug.println("samplesPerBlock: " + samplesPerBlock);
//Debug.println("channels: " + channels);
//Debug.println("blockSize: " + blockSize);

        //

        TwinVQ decoder = TwinVQ.getInstance();

        InputStream is = new BufferedInputStream(in);

        byte[] packet = new byte[blockSize];
        int[] samples = new int[channels * samplesPerBlock];

        //

        @SuppressWarnings("resource") PipedOutputStream pos =
            new PipedOutputStream((PipedInputStream) this.in);

        Thread thread = new Thread(new Runnable() {
            /** */
            public void run() {

                DataOutputStream os = null;

                try {
                    // big endian
                    os = new DataOutputStream(pos);

                    int done = 0;
                    while (done < -1) {

                        int l = 0;
                        while (l < packet.length && is.available() > 0) {
                            l += is.read(packet, l, packet.length - l);
                        }

                        int samplesThisBlock = samplesPerBlock;
//Debug.println("samplesThisBlock: " + samplesThisBlock + ", " + l);

                        TwinVQ.Index index = new TwinVQ.Index();
                        decoder.TvqDecodeFrame(index , null);

                        for (int i = 0; i < samplesThisBlock; i++) {
                            if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                                os.writeShort(samples[i]);
                            } else {
                                os.write( samples[i] & 0x00ff);
                                os.write((samples[i] & 0xff00) >> 8);
                            }
                        }
                        done += samplesThisBlock;
//Debug.println("done: " + done);
                    }
                } catch (IOException e) {
Debug.printStackTrace(e);
                } finally {
                    try {
                        os.flush();
                        os.close();
                    } catch (IOException e) {
Debug.println(e);
                    }
                }
            }
        });

        thread.start();
    }

    /** */
    private int available;

    /** */
    public int available() throws IOException {
        return available;
    }

    /**
     */
    public int read() throws IOException {
        available--;
        return in.read();
    }

    /** */
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException("byte[]");
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                 ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException("off: " + off + ", len: " + len);
        } else if (len == 0) {
            return 0;
        }

        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte) c;

        int i = 1;
        try {
            for (; i < len ; i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                if (b != null) {
                    b[off + i] = (byte) c;
                }
            }
        } catch (IOException e) {
e.printStackTrace(System.err);
        }
        return i;
    }


    //-------------------------------------------------------------------------

    /**
     * Play TwinVQ.
     *
     * @param args 0:ima wave, 1:output pcm, 2:test or not, use "test"
     */
    public static void main(String[] args) throws Exception {

        boolean isTest = args[2].equals("test");
        InputStream in = new BufferedInputStream(new FileInputStream(args[0]));

        //----

        int sampleRate = 44100;
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

        AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println(audioFormat);

        InputStream is = new TwinVQInputStream(in,
                                            4,
                                            2,
                                            4,
                                            byteOrder);
OutputStream os =
 new BufferedOutputStream(new FileOutputStream(args[1]));

        int bufferSize = 2048;

        DataLine.Info info =
            new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line =
            (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.addLineListener(new LineListener() {
            public void update(LineEvent ev) {
Debug.println(ev.getType());
                if (LineEvent.Type.STOP == ev.getType()) {
                    if (!isTest) {
                        System.exit(0);
                    }
                }
            }
        });
        line.start();
        byte[] buf = new byte[bufferSize];
        int l = 0;
        volume(line, .2d);

        while (is.available() > 0) {
            l = is.read(buf, 0, bufferSize);
            line.write(buf, 0, l);
os.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
os.close();
        is.close();
    }
}

/* */
