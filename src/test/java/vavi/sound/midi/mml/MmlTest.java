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
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.sampled.LineEvent;

import jp.or.rim.kt.kemusiro.sound.FMGeneralInstrument;
import jp.or.rim.kt.kemusiro.sound.MMLCompiler;
import jp.or.rim.kt.kemusiro.sound.MMLPlayer;
import jp.or.rim.kt.kemusiro.sound.MusicEvent;
import jp.or.rim.kt.kemusiro.sound.MusicScore;
import jp.or.rim.kt.kemusiro.sound.WaveInputStream;
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
    static long time = onIde ? 1000 * 1000 : 5 * 1000;

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
Debug.println("volume: " + volume + ", midi volume: " + midiVolume);
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

    @Test
    @DisabledIfEnvironmentVariable(named = "vavi.test", matches = "ai")
    void test5_investigate() throws Exception {
        // 1. Analyze WaveInputStream (used by test1)
        int tickPerBeat = 240;
        String mmlContent = String.join("", Files.readAllLines(Paths.get(mml)));
        MusicScore score = new MusicScore(tickPerBeat, 1);
        MMLCompiler compiler = new MMLCompiler(tickPerBeat, 1);
        compiler.compile(score, new String[] {mmlContent});

        System.err.println("--- Test1 (WaveInputStream / API) ---");
        System.err.println("MusicScore events:");
        for (MusicEvent e : score.getEventList()) {
            if (e.getTick() < 500) {
                System.err.printf("  tick: %d, event: %s%n", e.getTick(), e);
            }
        }

        WaveInputStream in = new WaveInputStream(score, 22100, 8);
        byte[] buf = new byte[22100];
        int read = in.read(buf);
        System.err.printf("WaveInputStream read %d bytes%n", read);

        // Find length of first non-zero section in buf
        int firstNonZero = -1;
        int firstZeroAfterNonZero = -1;
        for (int i = 0; i < read; i++) {
            if (buf[i] != 0 && firstNonZero == -1) {
                firstNonZero = i;
            } else if (buf[i] == 0 && firstNonZero != -1 && firstZeroAfterNonZero == -1) {
                firstZeroAfterNonZero = i;
            }
        }
        System.err.printf("Test1 first note start sample: %d, end sample: %d, count: %d samples (%.2f ms)%n",
                firstNonZero, firstZeroAfterNonZero, (firstZeroAfterNonZero - firstNonZero),
                (firstZeroAfterNonZero - firstNonZero) * 1000.0 / 22100);
        System.err.print("Test1 first 10 samples: ");
        for (int i = 0; i < 10 && i < read; i++) {
            System.err.printf("%d ", buf[i]);
        }
        System.err.println();

        // 2. Analyze MmlSequence (used by test3)
        System.err.println("--- Test3 (MmlSequence / SPI) ---");
        MmlSequence mmlSeq = new MmlSequence();
        mmlSeq.setScore(new BufferedInputStream(Files.newInputStream(Paths.get(mml))));
        Sequence sequence = mmlSeq.toMidiSequence();
        System.err.printf("Sequence resolution (PPQ): %d, divisionType: %f%n", sequence.getResolution(), sequence.getDivisionType());

        Track track = sequence.getTracks()[0];
        System.err.println("Sequence Track 0 events:");
        long noteOnTick = -1;
        long noteOffTick = -1;
        for (int i = 0; i < track.size(); i++) {
            MidiEvent ev = track.get(i);
            if (ev.getTick() < 500) {
                MidiMessage msg = ev.getMessage();
                if (msg instanceof ShortMessage sm) {
                    System.err.printf("  tick: %d, short: cmd=%02x ch=%d data1=%02x data2=%02x%n",
                            ev.getTick(), sm.getCommand(), sm.getChannel(), sm.getData1(), sm.getData2());
                    if (sm.getCommand() == ShortMessage.NOTE_ON && noteOnTick == -1) {
                        noteOnTick = ev.getTick();
                    } else if (sm.getCommand() == ShortMessage.NOTE_OFF && noteOffTick == -1) {
                        noteOffTick = ev.getTick();
                    }
                } else if (msg instanceof MetaMessage mm) {
                    byte[] data = mm.getData();
                    int tempo = 0;
                    if (mm.getType() == 0x51 && data.length == 3) {
                        tempo = ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
                    }
                    System.err.printf("  tick: %d, meta: type=%02x len=%d tempoMPQ=%d (BPM=%.1f)%n",
                            ev.getTick(), mm.getType(), mm.getLength(), tempo, tempo > 0 ? 60000000.0 / tempo : 0);
                }
            }
        }
        System.err.printf("Test3 NoteOn tick: %d, NoteOff tick: %d, tick diff: %d%n",
                noteOnTick, noteOffTick, (noteOffTick - noteOnTick));
    }
}
