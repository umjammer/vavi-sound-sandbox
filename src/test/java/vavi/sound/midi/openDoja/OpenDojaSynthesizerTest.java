/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.openDoja;

import java.nio.file.Files;
import java.nio.file.Paths;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * OpenDojaSynthesizerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/07/09 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class OpenDojaSynthesizerTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static long time = onIde ? 1000 * 1000 : 3 * 1000;

    @Property(name = "vavi.test.volume.midi")
    float volume = 0.2f;

    @Property(name = "openDoja.test")
    String openDojaTest = "src/test/resources/test.mid";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
        Debug.println("volume: " + volume);
    }

    @Test
    @DisplayName("Fuetrek scale")
    void testFuetrekScale() throws Exception {
        Synthesizer synthesizer = new FuetrekSynthesizer();
        synthesizer.open();
        Debug.println("synthesizer: " + synthesizer);

        Receiver receiver = synthesizer.getReceiver();
        volume(receiver, volume);

        int[] notes = {60, 62, 64, 65, 67, 69, 71, 72};
        for (int note : notes) {
            ShortMessage on = new ShortMessage(ShortMessage.NOTE_ON, 0, note, 100);
            receiver.send(on, -1);
            Thread.sleep(300);
            ShortMessage off = new ShortMessage(ShortMessage.NOTE_OFF, 0, note, 0);
            receiver.send(off, -1);
        }
        Thread.sleep(1000);

        receiver.close();
        synthesizer.close();
    }

    @Test
    @DisplayName("Ma3 scale")
    void testMa3Scale() throws Exception {
        Synthesizer synthesizer = new Ma3Synthesizer();
        synthesizer.open();
        Debug.println("synthesizer: " + synthesizer);

        Receiver receiver = synthesizer.getReceiver();
        volume(receiver, volume);

        int[] notes = {60, 62, 64, 65, 67, 69, 71, 72};
        for (int note : notes) {
            ShortMessage on = new ShortMessage(ShortMessage.NOTE_ON, 0, note, 100);
            receiver.send(on, -1);
            Thread.sleep(300);
            ShortMessage off = new ShortMessage(ShortMessage.NOTE_OFF, 0, note, 0);
            receiver.send(off, -1);
        }
        Thread.sleep(1000);

        receiver.close();
        synthesizer.close();
    }
}
