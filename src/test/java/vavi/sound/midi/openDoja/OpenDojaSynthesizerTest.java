/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.openDoja;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

import opendoja.audio.mld.MLD;
import opendoja.audio.mld.MLDPlayer;
import opendoja.audio.mld.Sampler;
import opendoja.audio.mld.SamplerProvider;
import opendoja.audio.mld.fuetrek.FueTrekSamplerProvider;
import opendoja.audio.mld.ma3.MA3SamplerProvider;
import vavi.sound.midi.MidiConstants;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
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

    static {
        // ensure synthesizer using default
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
        // should be set for playing openDoja synthesizer
//        System.setProperty("javax.sound.midi.Synthesizer", "#Fuetrek MIDI Synthesizer");
        System.setProperty("javax.sound.midi.Synthesizer", "#Ma3 MIDI Synthesizer");
    }

    static boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static long time = onIde ? 1000 * 1000 : 3 * 1000;

    @Property(name = "vavi.test.volume.midi")
    float volume = 0.2f;

    @Property(name = "openDoja.test")
    String openDojaTest = "src/test/resources/test.mid";

    @Property
    String mfi = "src/test/resources/test.mid";

    @Property(name = "openDoja.provider")
    String provider = "ma3";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

Debug.println("volume: " + volume + ", provider: " + provider);
    }

    SamplerProvider samplerProvider() {
        return switch (provider) {
            case "fuetrek" -> new FueTrekSamplerProvider();
            default -> new MA3SamplerProvider();
        };
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

    @Test
    @DisplayName("play mld")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
Debug.println(mfi + ", " + Files.exists(Path.of(mfi)));
        Sequence sequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(Path.of(mfi))));

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + MidiConstants.MetaEvent.valueOf(meta.getType()));
            if (meta.getType() == 47) cdl.countDown();
        };
        Sequencer sequencer = MidiSystem.getSequencer(false);
Debug.println("sequencer: " + sequencer);
        sequencer.addMetaEventListener(mel);
        sequencer.open();
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
Debug.println("synthesizer: " + synthesizer);
        assertInstanceOf(OpenDojaSynthesizer.class, synthesizer);
        synthesizer.open();
        // straight to the synthesizer: it takes the machine dependent exclusives of the file
        // itself, the voices and the waves, and gives them to the sampler
        Receiver receiver = synthesizer.getReceiver();
        sequencer.getTransmitter().setReceiver(receiver);
        sequencer.setSequence(sequence);
        volume(receiver, volume);

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
        synthesizer.close();
    }

    /**
     * The point of {@link OpenDojaSynthesizer}: the midi a converted mfi file is has to reach
     * the sampler as the very calls {@link MLDPlayer} makes out of the file itself.
     */
    @Test
    @DisplayName("the synthesizer drives the sampler like MLDPlayer does")
    void testSamplerParity() throws Exception {
        assumeTrue(Files.exists(Path.of(mfi)), mfi + " not found");
        assumeTrue(mfi.toLowerCase().endsWith(".mld"), mfi + " is not an mfi file");

        List<String> expected = byPlayer();
        List<String> actual = bySynthesizer();
Debug.println("MLDPlayer: " + expected.size() + " calls, synthesizer: " + actual.size() + " calls");

        // the converter sends a program change for a bank change too, which writes back the
        // program the channel has already, so drop the one a second program change follows
        List<String> pruned = new ArrayList<>();
        for (int i = 0; i < actual.size(); i++) {
            String call = actual.get(i);
            String head = call.substring(0, call.lastIndexOf(' ') + 1);
            boolean program = call.startsWith("raw e0") || call.startsWith("prog");
            if (program && i + 1 < actual.size() && actual.get(i + 1).startsWith(head)) continue;
            pruned.add(call);
        }

        // the order within a tick is the converter's, so only what is called and with what is
        // compared here, and a key off of a key which is not on is a no-op of every sampler
        assertEquals(histogram(expected.stream().filter(s -> !s.startsWith("off")).toList()),
                     histogram(pruned.stream().filter(s -> !s.startsWith("off")).toList()));
    }

    static Map<String, Integer> histogram(List<String> calls) {
        Map<String, Integer> histogram = new TreeMap<>();
        calls.forEach(call -> histogram.merge(call, 1, Integer::sum));
        return histogram;
    }

    /** what the openDoja player itself calls for the file */
    List<String> byPlayer() throws Exception {
        List<String> calls = new ArrayList<>();
        SamplerProvider provider = rate -> RecordingSampler.of(samplerProvider().instance(rate), calls);
        try (InputStream is = new BufferedInputStream(Files.newInputStream(Path.of(mfi)))) {
            MLDPlayer player = new MLDPlayer(new MLD(is), provider, 48000f);
            player.setLoopEnabled(false);
            float[] samples = new float[4096 * 2];
            // a jump of the file may well loop for ever, the midi of it plays through once
            long frames = 0, limit = 48000L * 60 * 10;
            while (!player.isFinished() && frames < limit) {
                int rendered = player.render(samples, 0, 4096, 0f);
                if (rendered == -1) break;
                frames += Math.max(rendered, 0);
            }
        }
        return calls;
    }

    /** what the synthesizer calls for the midi the file is converted into */
    List<String> bySynthesizer() throws Exception {
        List<String> calls = new ArrayList<>();
        Sequence sequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(Path.of(mfi))));
        OpenDojaSynthesizer synthesizer = new OpenDojaSynthesizer() {
            @Override public Info getDeviceInfo() { return null; }
            @Override protected SamplerProvider createSamplerProvider() {
                return rate -> RecordingSampler.of(samplerProvider().instance(rate), calls);
            }
        };
        synthesizer.open();
        try {
            Receiver receiver = synthesizer.getReceiver();
            volume(receiver, 0); // the parity is in the calls, no need to hear it
            List<MidiEvent> events = new ArrayList<>();
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) events.add(track.get(i));
            }
            events.sort(Comparator.comparingLong(MidiEvent::getTick));
            for (MidiEvent event : events) receiver.send(event.getMessage(), -1);
            return new ArrayList<>(calls);
        } finally {
            synthesizer.close();
        }
    }

    /**
     * A {@link Sampler} which writes down every call but a render.
     *
     * @see RecordingSampler#of the raw ext-B path of a sampler is only taken for one which
     *      takes raw ext-B events, so the recorder must not be one when the sampler is not
     */
    static class RecordingSampler implements Sampler {

        final Sampler sampler;
        final List<String> calls;

        RecordingSampler(Sampler sampler, List<String> calls) {
            this.sampler = sampler;
            this.calls = calls;
        }

        static Sampler of(Sampler sampler, List<String> calls) {
            return sampler instanceof opendoja.audio.mld.MLDRawExtBHandler
                    ? new RawRecordingSampler(sampler, calls) : new RecordingSampler(sampler, calls);
        }

        void add(String call) {
            calls.add(call);
        }

        static String f(float value) {
            return "%.5f".formatted(value);
        }

        @Override public void bankChange(int c, int b) { add("bank " + c + " " + b); sampler.bankChange(c, b); }
        @Override public void drumEnable(int c, boolean e) { add("drum " + c + " " + e); sampler.drumEnable(c, e); }
        @Override public boolean isFinished() { return sampler.isFinished(); }
        @Override public opendoja.audio.mld.MLDPlaybackEngine createPlaybackEngine(MLD mld, float rate) {
            // the sampler would bind the engine to itself, and the raw ext-B events
            // FueTrekMLDPlaybackEngine takes out of the shared flow would go past this recorder
            return sampler instanceof opendoja.audio.mld.MLDRawExtBHandler
                    ? new opendoja.audio.mld.fuetrek.FueTrekMLDPlaybackEngine(this, mld, rate)
                    : new opendoja.audio.mld.ma3.MA3MLDPlaybackEngine(this);
        }
        @Override public void keyOff(int c, int k) { add("off " + c + " " + k); sampler.keyOff(c, k); }
        @Override public void keyOn(int c, int k, float v) { add("on " + c + " " + k + " " + f(v)); sampler.keyOn(c, k, v); }
        @Override public void masterTune(float s) { add("mtune " + f(s)); sampler.masterTune(s); }
        @Override public void masterVolume(float v) { add("mvol " + f(v)); sampler.masterVolume(v); }
        @Override public void panpot(int c, float p) { add("pan " + c + " " + f(p)); sampler.panpot(c, p); }
        @Override public void pitchBend(int c, float s) { add("bend " + c + " " + f(s)); sampler.pitchBend(c, s); }
        @Override public void pitchBendRange(int c, float r) { add("brange " + c + " " + f(r)); sampler.pitchBendRange(c, r); }
        @Override public void programChange(int c, int p) { add("prog " + c + " " + p); sampler.programChange(c, p); }
        @Override public void render(float[] s, int o, int f) { sampler.render(s, o, f); }
        @Override public void render(float[] s, int o, int f, float a) { sampler.render(s, o, f, a); }
        @Override public void render(float[] s, int o, int f, float l, float r) { sampler.render(s, o, f, l, r); }
        @Override public void render(float[] s, int o, int f, float l, float r, boolean e, boolean c) { sampler.render(s, o, f, l, r, e, c); }
        @Override public void reset() { add("reset"); sampler.reset(); }
        @Override public float sampleRate() { return sampler.sampleRate(); }
        @Override public void stopAll() { add("stopAll"); sampler.stopAll(); }
        @Override public void sysEx(byte[] m) { add("sysex " + m.length + " " + head(m)); sampler.sysEx(m); }
        @Override public boolean suppressActiveKeyRetrigger() { return sampler.suppressActiveKeyRetrigger(); }
        @Override public void volume(int c, float v) { add("vol " + c + " " + f(v)); sampler.volume(c, v); }

        static String head(byte[] message) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(message.length, 6); i++) sb.append("%02x".formatted(message[i]));
            return sb.toString();
        }
    }

    /** the recorder of a sampler which takes the raw ext-B events, the fuetrek one */
    static class RawRecordingSampler extends RecordingSampler implements opendoja.audio.mld.MLDRawExtBHandler {

        RawRecordingSampler(Sampler sampler, List<String> calls) {
            super(sampler, calls);
        }

        @Override
        public void handleRawExtBEvent(int id, int channel, int param) {
            add("raw %02x".formatted(id) + " " + channel + " " + param);
            ((opendoja.audio.mld.MLDRawExtBHandler) sampler).handleRawExtBEvent(id, channel, param);
        }
    }
}
