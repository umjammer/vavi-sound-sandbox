/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.sampled.LineEvent;

import jp.or.rim.kt.kemusiro.sound.FMGeneralInstrument;
import jp.or.rim.kt.kemusiro.sound.MMLPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * MmlTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-12-18 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
@DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
public class MmlTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static final float volume = Float.parseFloat(System.getProperty("vavi.test.volume",  "0.2"));

    @Property(name = "mml")
    String mml = "src/test/resources/mml/BADINERIE.mml";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }
    @Test
    void test1() throws Exception {
        main(new String[] {mml});
    }

    private static void usage() {
        System.out.println("java MMLPlayer MML1 [MML2 [MML3]]");
    }

    /**
     * 引数で与えられたMML文字列を演奏する。
     *
     * @param args [-f instr.txt] mml1.mml [mm2.mml [mm3.mml]]
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        if (args[0].equals("-f")) {
            FMGeneralInstrument.readParameter(new FileReader(args[1]));
            String[] new_args = new String[args.length - 2];
            for (int i = 2; i < args.length; i++) {
                new_args[i - 2] = args[i];
            }
            args = new_args;
        } else {
            FMGeneralInstrument.readParameterByResource();
        }
        CountDownLatch cdl = new CountDownLatch(1);
        MMLPlayer p = new MMLPlayer(e -> {
            if (e.getType() == LineEvent.Type.STOP) cdl.countDown();
        });
        String[] mmls = new String[args.length];
        int i = 0;
        for (String arg : args) {
            mmls[i++] = String.join("", Files.readAllLines(Paths.get(arg)));
        }
        p.setVolume(volume);
        p.setMML(mmls);
        p.start();
        cdl.await();
Debug.println("here");
        p.stop();
        Thread.getAllStackTraces().keySet().forEach(System.err::println);
    }
}