/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import jp.or.rim.kt.kemusiro.sound.MMLPlayer;
import org.junit.jupiter.api.Test;


/**
 * MmlTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-12-18 nsano initial version <br>
 */
public class MmlTest {
    @Test
    void test1() throws Exception {
        MMLPlayer.main(new String[] {"src/test/resources/mml/BADINERIE.mml"});
    }
}