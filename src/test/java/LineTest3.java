/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.TargetDataLine;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;


/**
 * LineTest3.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/19 umjammer initial version <br>
 */
public class LineTest3 {

    @Test
    @Disabled("+8 doesn't work?")
    void test() throws Exception {
        final String PROPERTIES_FILENAME = "sound.properties";
        Path path = Paths.get(System.getProperty("java.home"), "lib", PROPERTIES_FILENAME);
        Files.readAllLines(path).forEach(System.err::println);
    }

    @Test
    void test2() throws Exception {
        AudioFormat targetFormat = new AudioFormat(NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, true, true);
        Line.Info targetInfo = new DataLine.Info(TargetDataLine.class, targetFormat);
        Arrays.stream(AudioSystem.getTargetLineInfo(targetInfo)).forEach(System.err::println);
    }
}
