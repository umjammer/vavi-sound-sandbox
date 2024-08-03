/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import vavi.io.InputEngine;
import vavi.io.InputEngineOutputStream;

import vavix.io.AdvancedPipedInputStream;


/**
 * Converts an ROCOCOA bitstream into a PCM 16bits/sample audio stream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050722 nsano initial version <br>
 */
public class Rococoa2PcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     *
     * @param in     the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public Rococoa2PcmAudioInputStream(InputStream in, AudioFormat format, long length) {
        super(init(in), format, length);
    }

    /**  */
    static InputStream init(InputStream in) {
        AdvancedPipedInputStream source = new AdvancedPipedInputStream();
        AdvancedPipedInputStream.OutputStreamEx sink = source.getOutputStream();
        new Thread() {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(8192);

            @Override
            public void run() {
                try {
                    OutputStream out = new InputEngineOutputStream(new TempFileInputEngine(sink));
                    ReadableByteChannel rbc = Channels.newChannel(in);
                    WritableByteChannel wbc = Channels.newChannel(out);
                    while (true) {
                        buffer.clear();
                        if (rbc.read(buffer) < 0) {
                            break;
                        }
                        buffer.flip();
                        wbc.write(buffer);
                    }
                    sink.close();
                } catch (IOException ex) {
                    try {
                        sink.setException(ex);
                    } catch (IOException ignored) {
                    }
                }
            }
        }.start();
        return source;
    }

    /**  */
    static class TempFileInputEngine implements InputEngine {

        InputStream in;
        OutputStream out;
        Path file;

        TempFileInputEngine(OutputStream out) throws IOException {
            this.out = out;
            this.file = Files.createTempFile("vavi", ".rococoa"); // TODO delete on exit
            this.out = Files.newOutputStream(file);
        }

        @Override
        public void initialize(InputStream in) throws IOException {
            if (this.in != null) {
                throw new IOException("Already initialized");
            } else {
                this.in = in;
            }
        }

        final ByteBuffer buffer = ByteBuffer.allocateDirect(8192);

        @Override
        public void execute() throws IOException {
            if (in == null) {
                throw new IOException("Not yet initialized");
            } else {
                ReadableByteChannel rbc = Channels.newChannel(in);
                WritableByteChannel wbc = Channels.newChannel(out);
                buffer.clear();

                // TODO AVAudioConverter

                if (rbc.read(buffer) < 0) {
                    in.close();
                } else {
                    buffer.flip();
                    wbc.write(buffer);
                }
            }
        }

        @Override
        public void finish() throws IOException {
            out.flush();
            out.close();
        }
    }
}
