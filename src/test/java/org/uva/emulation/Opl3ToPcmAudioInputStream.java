/*
 * http://opl3.cozendey.com/
 */

package org.uva.emulation;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import org.uva.emulation.Opl3Player.FileType;

import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Opl3AudioInputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/23 umjammer initial version <br>
 */
public class Opl3ToPcmAudioInputStream extends AudioInputStream {

    /** decode */
    public Opl3ToPcmAudioInputStream(InputStream stream, AudioFormat format, long length, AudioFormat sourceFormat) throws IOException {
        super(new OutputEngineInputStream(new Opl3OutputEngine(stream, sourceFormat, (int) length)), format, length);
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
            byte[] buf = new byte[length > 0 ? length : is.available()];
Debug.println(Level.FINE, "buf: " + buf.length + ", " + length + ", " + is.available() + ", " + is);
            int l = 0;
            while (true) {
                int r = is.read(buf, l, buf.length - l);
                if (r < 0) {
                    break;
                }
            }
Debug.println(Level.FINE, "buf:\n" + StringUtil.getDump(buf, 128));
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
//Debug.println("sec: " + sec);

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
//Debug.println("finish");
        }
    }
}
