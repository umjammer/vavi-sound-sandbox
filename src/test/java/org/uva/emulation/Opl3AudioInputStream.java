/*
 * http://opl3.cozendey.com/
 */

package org.uva.emulation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.util.Debug;


/**
 * Opl3AudioInputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/23 umjammer initial version <br>
 */
public class Opl3AudioInputStream extends AudioInputStream {

    /** decoder database */
    private enum FileType {
        MID(Opl3Encoding.MID, new MidPlayer()),
        DRO1(Opl3Encoding.DRO1, new DroPlayer(true)),
        DRO2(Opl3Encoding.DRO2, new Dro2Player(true));
        AudioFormat.Encoding encoding;
        Opl3Player player;
        FileType(AudioFormat.Encoding encoding, Opl3Player player) {
            this.encoding = encoding;
            this.player = player;
        }
        static Opl3Player getPlayer(AudioFormat.Encoding encoding) {
Debug.println("encoding: " + encoding);
            return Arrays.stream(values()).filter(e -> e.encoding == encoding).findFirst().get().player;
        }
    }

    /** decode */
    public Opl3AudioInputStream(InputStream stream, AudioFormat format, long length, AudioFormat sourceFormat) throws IOException {
        super(getInputStream3(stream, sourceFormat, (int) length), format, AudioSystem.NOT_SPECIFIED);
    }

    /** output engine */
    private static InputStream getInputStream3(InputStream is, AudioFormat format, int length) throws IOException {
        return new OutputEngineInputStream(new Opl3OutputEngine(is, format, length));
    }

    /** */
    private static class Opl3OutputEngine implements OutputEngine {

        /** */
        private Opl3Player player;

        /** */
        private DataOutputStream out;

        /** */
        private float sampleRate;

        /** */
        public Opl3OutputEngine(InputStream is, AudioFormat format, int length) throws IOException {
            byte[] buf = new byte[length != AudioSystem.NOT_SPECIFIED ? length : is.available()];
Debug.println("buf: " + buf.length);
            DataInputStream dis = new DataInputStream(is);
            dis.readFully(buf);

            player = FileType.getPlayer(format.getEncoding());
            player.load(buf);

            sampleRate = format.getSampleRate();
        }

        /** */
        public void initialize(OutputStream out) throws IOException {
            if (this.out != null) {
                throw new IOException("Already initialized");
            } else {
                this.out = new DataOutputStream(out);
            }
        }

        /** */
        public void execute() throws IOException {
            if (out == null) {
                throw new IOException("Not yet initialized");
            } else {
                if (player.update()) {
                    double sec = 1.0 / player.getRefresh();

                    byte[] buf = player.read(4 * (int) (sampleRate * sec));
                    out.write(buf);
                } else {
                    for (int wait = 0; wait < 30; ++wait) {
                        double sec = 0.1;

                        byte[] buf = player.read(4 * (int) (sampleRate * sec));
                        out.write(buf);
                    }
                    out.close();
                }
            }
        }

        /** */
        public void finish() throws IOException {
        }
    }

    /** blocking deque */
    private static InputStream getInputStream(InputStream is, AudioFormat format, int length) throws IOException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            byte[] buf = new byte[length != AudioSystem.NOT_SPECIFIED ? length : is.available()];
Debug.println("buf: " + buf.length);
            DataInputStream dis = new DataInputStream(is);
            dis.readFully(buf);

            Opl3Player player = FileType.getPlayer(format.getEncoding());
            player.load(buf);

            final BlockingDeque<Byte> buffer = new LinkedBlockingDeque<>();

            AtomicBoolean done = new AtomicBoolean(false);
            executor.submit(() -> {
                write(player, buffer, format.getSampleRate());
Debug.println("write: done");
                done.getAndSet(true);
            });
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new UnsupportedOperationException("read()I");
                }
                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    if (done.get()) {
                        if (buffer.isEmpty()) {
Debug.println("read: eof");
                            return -1;
                        } else {
Debug.println("read: drain");
                            len = Math.min(len, buffer.size());
                        }
                    }
                    int i = off;
                    try {
//Debug.println("read: " + len + ", " + buffer.size());
                        for (; i < len; i++) {
                            b[i] = buffer.take();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return i - off;
                }
            };
        } finally {
            executor.shutdownNow();
Debug.println("shutdown: " + executor.isTerminated() + ", " + executor.isShutdown());
        }
    }

    /** */
    private static void write(Opl3Player player, BlockingDeque<Byte> buffer, float sampleRate) {
        while (player.update()) {
            double sec = 1.0 / player.getRefresh();

            byte[] buf = player.read(4 * (int) (sampleRate * sec));
            try {
                for (byte b : buf) {
                    buffer.put(b);
                }
//Debug.println("write: " + buf.length);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int wait = 0; wait < 30; ++wait) {
            double sec = 0.1;

            byte[] buf = player.read(4 * (int) (sampleRate * sec));
            try {
                for (byte b : buf) {
                    buffer.put(b);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /** baos (works) */
    private static InputStream getInputStream2(InputStream is, AudioFormat format, int length) throws IOException {
            byte[] buf = new byte[length != AudioSystem.NOT_SPECIFIED ? length : is.available()];
Debug.println("buf: " + buf.length);
            DataInputStream dis = new DataInputStream(is);
            dis.readFully(buf);

            Opl3Player player = FileType.getPlayer(format.getEncoding());
            player.load(buf);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            write(player, buffer, format.getSampleRate());
Debug.println("write: done");
            return new ByteArrayInputStream(buffer.toByteArray());
    }

    /** */
    private static void write(Opl3Player player, ByteArrayOutputStream buffer, float sampleRate) throws IOException {
        while (player.update()) {
            double sec = 1.0 / player.getRefresh();

            byte[] buf = player.read(4 * (int) (sampleRate * sec));
            buffer.write(buf);
        }

        for (int wait = 0; wait < 30; ++wait) {
            double sec = 0.1;

            byte[] buf = player.read(4 * (int) (sampleRate * sec));
            buffer.write(buf);
        }
    }
}
