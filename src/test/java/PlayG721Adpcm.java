/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.sound.adpcm.ccitt.G721InputStream;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.SoundUtil.volume;


/**
 * Play G721 ADPCM.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030714 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class PlayG721Adpcm {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    String g721 = "src/test/resources/test.g721";

    int sampleRate = 16000;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    /**
     * usage: java PlayG721Adpcm g721_file [sampleRate]
     */
    public static void main(String[] args) throws Exception {
        PlayG721Adpcm app = new PlayG721Adpcm();
        app.setup();
        app.g721 = args[0];
        if (args.length == 2) {
            app.sampleRate = Integer.parseInt(args[1]);
System.err.println("sampleRate: " + app.sampleRate);
        }
        app.test0();
    }

    @Test
    void test0() throws Exception {
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
Debug.println(format);

        InputStream is = new G721InputStream(new BufferedInputStream(Files.newInputStream(Paths.get(g721))), byteOrder);
//OutputStream os = new BufferedOutputStream(new FileOutputStream(args[1]));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.addLineListener(ev -> {
Debug.println(ev.getType());
//            if (LineEvent.Type.STOP == ev.getType()) {}
        });
        volume(line, volume);
        line.start();
        byte[] buf = new byte[1024];
        int l;
//Debug.println("before: " + is.available());
        while (is.available() > 0) {
            l = is.read(buf, 0, 1024);
            line.write(buf, 0, l);
//Debug.println(l + ", " + is.available());
//os.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
//os.close();
        is.close();
    }
}
