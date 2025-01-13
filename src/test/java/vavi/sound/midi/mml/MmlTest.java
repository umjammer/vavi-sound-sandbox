/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mml;

import java.io.BufferedInputStream;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.LineEvent;

import jp.or.rim.kt.kemusiro.sound.FMGeneralInstrument;
import jp.or.rim.kt.kemusiro.sound.MMLPlayer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static vavi.sound.midi.MidiUtil.volume;


/**
 * vavi.sound.midi.mml.MmlTest.
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

    static {
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer"); // why this not comes first?
//        System.setProperty("javax.sound.midi.Synthesizer", "#Gervill"); // why this not comes first?
    }

    static boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static long time = onIde ? 1000 * 1000 : 10 * 1000;

    @Property(name = "vavi.test.volume")
    float volume = 0.2f;

    @Property(name = "vavi.test.volume.midi")
    float midiVolume = 0.2f;

    @Property(name = "mml")
    String mml = "src/test/resources/mml/BADINERIE.mml";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
Debug.println("volume: " + volume + ", midi vplume: " + midiVolume);
    }

    @BeforeAll
    static void setupAll() throws Exception {
        System.setProperty("vavi.sound.opl3.MidiFile", "true");
    }

    @AfterAll
    static void teardown() throws Exception {
        System.setProperty("vavi.sound.opl3.MidiFile", "false");
    }

    @Test
    @DisplayName("play")
    void test1() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);
        MMLPlayer p = new MMLPlayer(e -> {
            if (e.getType() == LineEvent.Type.STOP) cdl.countDown();
        });
        p.setVolume(volume);
        p.setMML(new String[] {String.join("", Files.readAllLines(Paths.get(mml)))});
        p.start();
if (!onIde) {
 Thread.sleep(time);
 p.stop();
 Debug.println("STOP");
} else {
        cdl.await();
        p.stop();
}
Debug.println("here");
//Thread.getAllStackTraces().keySet().forEach(System.err::println);
    }

    /**
     * Plays the MML string given as an argument.
     *
     * @param args [-f instr.txt] mml1.mml [mm2.mml [mm3.mml]]
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("java MMLPlayer MML1 [MML2 [MML3]]");
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

        MmlTest app = new MmlTest();
        app.setup();
        for (String arg : args) {
            app.mml = arg;
            app.test1();
        }
    }

    @Test
    @DisplayName("spi, convert to midi sequence -> midi sequencer")
    void test2() throws Exception {
        Sequence sequence = new MmlMidiFileReader().getSequence(new BufferedInputStream(Files.newInputStream(Paths.get(mml))));

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        };
        Sequencer sequencer = MidiSystem.getSequencer(true);
Debug.println("sequencer: " + sequencer);
        sequencer.open();
        volume(sequencer.getReceiver(), midiVolume);
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);

        sequencer.start();
if (!onIde) {
 Thread.sleep(time);
 sequencer.stop();
 Debug.println("STOP");
} else {
        cdl.await();
}
        sequencer.removeMetaEventListener(mel);
        sequencer.close();
    }

    @Test
    @DisplayName("spi")
    void test3() throws Exception {
        Sequence sequence = new MmlMidiFileReader().getSequence(new BufferedInputStream(Files.newInputStream(Paths.get(mml))));

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        };
        Sequencer sequencer = MidiSystem.getSequencer(false);
Debug.println("sequencer: " + sequencer);
        sequencer.addMetaEventListener(mel);
        sequencer.open();
        Synthesizer synthesizer = new MmlSynthesizer();
        synthesizer.open();
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        volume(synthesizer.getReceiver(), midiVolume);
        sequencer.setSequence(sequence);

        sequencer.start();
if (!onIde) {
 Thread.sleep(time);
 sequencer.stop();
 Debug.println("STOP");
} else {
        cdl.await();
}
        sequencer.removeMetaEventListener(mel);
        sequencer.close();
    }

    static Stream<Arguments> programs() {
        return Stream.of(
                arguments(0, 0),
                arguments(1, 0),
                arguments(2, 0),
                arguments(2, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("programs")
    void test4(int data1, int data2) throws Exception {
        Synthesizer synthesizer = new MmlSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);
        Arrays.stream(synthesizer.getLoadedInstruments())
                .forEach(i ->
                        System.err.printf("patch: %d.%d%n".formatted(i.getPatch().getBank(), i.getPatch().getProgram())));

        volume(synthesizer.getReceiver(), midiVolume);

        MidiChannel channel = synthesizer.getChannels()[0];
        channel.programChange(data1, data2);
        for (int i = 0; i < 32; i++) {
            channel.noteOn(63 + i, 127);
            Thread.sleep(200);
            channel.noteOff(63 + i);
        }

        Thread.sleep(1000);

        synthesizer.close();
    }
}
