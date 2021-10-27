/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;


/**
 * Test4.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/19 umjammer initial version <br>
 */
public class Test4 {

    @Test
    void test() throws Exception {
        final String PROPERTIES_FILENAME = "sound.properties";
        Path path = Paths.get(System.getProperty("java.home"), "lib", PROPERTIES_FILENAME);
        Files.readAllLines(path).forEach(System.err::println);
    }
}

/* */
