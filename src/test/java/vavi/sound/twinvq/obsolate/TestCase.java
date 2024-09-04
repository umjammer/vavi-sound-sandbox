/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq.obsolate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import vavi.sound.twinvq.obsolate.TwinVQ.ConfInfo;
import vavi.sound.twinvq.obsolate.TwinVQ.ConfInfoSubBlock;
import vavi.sound.twinvq.obsolate.TwinVQ.HeaderInfo;
import vavi.sound.twinvq.obsolate.TwinVQ.Index;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-07-04 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "twinvq")
    String twinvq = "src/test/resources/test.vqf";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    void test1() throws Exception {
        Player player = new Player(twinvq);
    }

    @Test
    @Disabled("jna method maker")
    void fields() throws Exception {
        Class<?>[] classes = new Class[] {
                Index.class,
                ConfInfoSubBlock.class,
                ConfInfo.class,
                HeaderInfo.class,

        };
        for (Class<?> c : classes) {
            System.out.println(c.getName());
            Arrays.stream(c.getDeclaredFields()).forEach(f -> {
                System.out.print("\"" + f.getName() + "\", ");
            });
            System.out.println();
        }
    }
}
