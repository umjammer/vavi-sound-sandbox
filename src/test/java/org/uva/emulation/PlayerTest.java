/*
 * https://github.com/ulrich/opl3-player
 */

package org.uva.emulation;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import vavi.util.Debug;
import vavi.util.StringUtil;

import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class PlayerTest {

    @Test
    public void should_read_laa_sample_file() throws Exception {
        Path path = Paths.get(PlayerTest.class.getClassLoader().getResource("dott_chron-o-john_station2.laa").toURI());
        Player player = new Player(path.toString());

        player.loadFile(path.toFile());
        player.playFirst();

        assertNotNull(player.musicBuffer);
        assertEqualsBuffer(player.musicBuffer);
    }

    private static void assertEqualsBuffer(byte[][] buffer) throws Exception {
        StringBuilder acutalBuffer = new StringBuilder();
        for (int i = 0; i < buffer.length; i++) {
            for (int j = 0; j < buffer[i].length; j++) {
                acutalBuffer.append(buffer[i][j]);
            }
        }
        byte[] actualBuffer = acutalBuffer.toString().getBytes();
        byte[] expectedBuffer = readFileToByteArray(Paths
                .get(PlayerTest.class.getClassLoader().getResource("dott_chron-o-john_station2.raw").toURI())
                .toFile());
Debug.println(StringUtil.getDump(actualBuffer, 819200, 128));
Debug.println(StringUtil.getDump(expectedBuffer, 819200, 128));
        assertArrayEquals(expectedBuffer, actualBuffer);
    }
}
