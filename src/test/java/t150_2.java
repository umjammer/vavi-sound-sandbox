/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import vavi.sound.adpcm.ccitt.G721InputStream;

import static vavi.sound.SoundUtil.volume;


/**
 * Play G721 ADPCM.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030714 nsano initial version <br>
 */
public class t150_2 {

    /**
     * usage: java t150_2 g721_file [sampleRate]
     */
    public static void main(String[] args) throws Exception {

        int sampleRate = 16000;
        if (args.length == 2) {
            sampleRate = Integer.parseInt(args[1]);
System.err.println("sampleRate: " + sampleRate);
        }

        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println(format);

        InputStream is = new G721InputStream(new BufferedInputStream(new FileInputStream(args[0])), byteOrder);
// OutputStream os = new BufferedOutputStream(new FileOutputStream(args[1]));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.addLineListener(new LineListener() {
            public void update(LineEvent ev) {
                if (LineEvent.Type.STOP == ev.getType()) {
                    System.exit(0);
                }
            }
        });
        volume(line, .2d);
        line.start();
        byte[] buf = new byte[1024];
        int l = 0;
// System.err.println("before: " + is.available());
        while (is.available() > 0) {
            l = is.read(buf, 0, 1024);
            line.write(buf, 0, l);
// System.err.println(l + ", " + is.available());
// os.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
// os.close();
        is.close();
    }
}

/* */
