/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.opl3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.sound.opl3.Opl3Player;
import vavi.sound.opl3.Opl3Player.FileType;

import static java.lang.System.getLogger;


/**
 * Opl3AudioInputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/23 umjammer initial version <br>
 * @see "http://opl3.cozendey.com/"
 */
public class Opl3ToPcmAudioInputStream extends AudioInputStream {

    private static final Logger logger = getLogger(Opl3ToPcmAudioInputStream.class.getName());

    /** decode */
    public Opl3ToPcmAudioInputStream(InputStream stream, AudioFormat format, long length, AudioFormat sourceFormat) throws IOException {
        super(new OutputEngineInputStream(new Opl3OutputEngine(stream, sourceFormat)), format, length);
    }

    /** */
    private static class Opl3OutputEngine implements OutputEngine {

        /** */
        private final Opl3Player player;

        /** */
        private OutputStream out;

        /** */
        private final float sampleRate;

        /** */
        public Opl3OutputEngine(InputStream is, AudioFormat format) throws IOException {
            player = FileType.getPlayer(format.getEncoding());
            player.load(is);
            player.setProperties(format.properties());

            sampleRate = format.getSampleRate();
        }

        @Override
        public void initialize(OutputStream out) throws IOException {
            if (this.out != null) {
                throw new IOException("Already initialized");
            } else {
                this.out = out;
            }
logger.log(Level.DEBUG, "engin start");
        }

        @Override
        public void execute() throws IOException {
            if (out == null) {
                throw new IOException("Not yet initialized");
            } else {
                if (player.update()) {
                    double sec = 1.0 / player.getRefresh();
logger.log(Level.TRACE, "engine bytes: " + (int) (sampleRate * sec) + ", " + player.getRefresh());

                    byte[] buf = player.read(4 * (int) (sampleRate * sec));
                    out.write(buf, 0, buf.length);
                } else {
                    for (int wait = 0; wait < 30; ++wait) {
                        double sec = 0.1;

                        byte[] buf = player.read(4 * (int) (sampleRate * sec));
                        out.write(buf, 0, buf.length);
                    }
logger.log(Level.DEBUG, "engine last");
                    out.close();
                }
            }
        }

        @Override
        public void finish() throws IOException {
logger.log(Level.TRACE, "finish");
        }
    }
}
