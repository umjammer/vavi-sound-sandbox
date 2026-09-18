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
import java.util.Arrays;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        // for a run of another file w/o touching local.properties
        mfi = System.getProperty("openDoja.mfi", mfi);
        provider = System.getProperty("openDoja.provider", provider);

        switch (provider) {
            case "fuetrek" -> System.setProperty("javax.sound.midi.Synthesizer", "#Fuetrek MIDI Synthesizer");
            default -> System.setProperty("javax.sound.midi.Synthesizer", "#Ma3 MIDI Synthesizer");
        };

        Debug.println("volume: " + volume + ", provider: " + System.getProperty("javax.sound.midi.Synthesizer", "#Ma3 MIDI Synthesizer"));
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

        // each message on the frame the player is on at its tick, the synthesizer calls the
        // sampler in the very order the player does, as far as the midi goes: a jump of the
        // file the player repeats, the midi plays once. the player lets go of keys which are
        // not on (a note of velocity 0), a no-op of every sampler, see testRenderParity
        expected = expected.stream().filter(s -> !s.startsWith("off")).toList();
        actual = actual.stream().filter(s -> !s.startsWith("off")).toList();
        assertEquals(expected.subList(0, Math.min(expected.size(), actual.size())), actual);
    }

    @Test
    @EnabledIfSystemProperty(named = "openDoja.timed", matches = ".+")
    void dumpTimedCalls() throws Exception {
        RecordingSampler.timed = true;
        try {
            Files.write(Path.of("tmp", "calls_player.txt"), byPlayer());
            List<String> calls = bySynthesizer();
            Files.write(Path.of("tmp", "calls_synthesizer.txt"), calls);
        } finally {
            RecordingSampler.timed = false;
        }
    }

    /**
     * The real time sequencer sends the messages whenever its thread wakes up, the renderer
     * has to put them on frames spaced as their ticks are, for every track to keep the tempo.
     */
    @Test
    @DisplayName("real time: the notes keep their spacing")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void testRealtimeTiming() throws Exception {
        assumeTrue(Files.exists(Path.of(mfi)), mfi + " not found");
        assumeTrue(mfi.toLowerCase().endsWith(".mld"), mfi + " is not an mfi file");

        RecordingSampler.timed = true;
        try {
            List<String> expected = bySynthesizer().stream().filter(c -> c.contains(" on ")).toList();

            List<String> calls = java.util.Collections.synchronizedList(new ArrayList<>());
            OpenDojaSynthesizer synthesizer = new OpenDojaSynthesizer() {
                @Override public Info getDeviceInfo() { return null; }
                @Override protected SamplerProvider createSamplerProvider() {
                    return rate -> RecordingSampler.of(samplerProvider().instance(rate), calls);
                }
            };
            Sequence sequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(Path.of(mfi))));
            Sequencer sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            synthesizer.open();
            Receiver receiver = synthesizer.getReceiver();
            volume(receiver, volume);
            sequencer.getTransmitter().setReceiver(receiver);
            sequencer.setSequence(sequence);
            sequencer.start();
            Thread.sleep(10_000);
            sequencer.stop();
            sequencer.close();
            synthesizer.close();

            List<String> actual;
            synchronized (calls) {
                actual = calls.stream().filter(c -> c.contains(" on ")).toList();
            }
            // the same notes, each one late by as much as the first one
            int n = Math.min(expected.size(), actual.size()) - 20; // the last ones may be cut
            long[] lateness = new long[n];
            for (int i = 0; i < n; i++) {
                String[] e = expected.get(i).split(" ", 2), a = actual.get(i).split(" ", 2);
                assertEquals(e[1], a[1], "note " + i);
                lateness[i] = Long.parseLong(a[0]) - Long.parseLong(e[0]);
            }
            // the player floors the frames of every run of ticks, so it runs a little fast,
            // which is not the jitter
            double mx = 0, my = 0, sxx = 0, sxy = 0;
            long[] x = new long[n];
            for (int i = 0; i < n; i++) { x[i] = Long.parseLong(expected.get(i).split(" ", 2)[0]); mx += x[i]; my += lateness[i]; }
            mx /= n; my /= n;
            for (int i = 0; i < n; i++) { sxx += (x[i] - mx) * (x[i] - mx); sxy += (x[i] - mx) * (lateness[i] - my); }
            double slope = sxx == 0 ? 0 : sxy / sxx;
            long[] sorted = new long[n];
            for (int i = 0; i < n; i++) sorted[i] = Math.abs(Math.round(lateness[i] - my - slope * (x[i] - mx)));
            Arrays.sort(sorted);
            long worst = sorted[n - 1];
            long p99 = sorted[n - 1 - n / 100];
Debug.printf("real time: %d notes, latency: %.0f frames, drift: %.1f frames/s, jitter: 99%%: %.2f ms, worst: %.2f ms", n, my, slope * 48000, p99 / 48d, worst / 48d);
            assertTrue(p99 < 48 * 3, "jitter: " + p99);
        } finally {
            RecordingSampler.timed = false;
        }
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
        OpenDojaSynthesizer synthesizer = new OpenDojaSynthesizer() {
            @Override public Info getDeviceInfo() { return null; }
            @Override protected SamplerProvider createSamplerProvider() {
                return rate -> RecordingSampler.of(samplerProvider().instance(rate), calls);
            }
        };
        renderBySynthesizer(synthesizer);
        calls.removeLast(); // the stopAll of close
        return new ArrayList<>(calls);
    }

    /**
     * The events of all the tracks in order, each with its time in microseconds, as a
     * sequencer would send them, only on the very frames {@link MLDPlayer} is on at their ticks.
     * <p>
     * The player floors the frames of every run of ticks to its next event, events the midi
     * does not have included, so they are taken from the player itself: the frame it is on
     * whenever it calls the sampler, at the tick it is on.
     * </p>
     *
     * @param chunks the frames the player starts rendering at, as a fuetrek sampler sounds
     *        differently for other runs of frames rendered at once
     */
    List<long[]> timeline(Sequence sequence, List<MidiEvent> events, List<Long> chunks) throws Exception {
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) events.add(track.get(i));
        }
        events.sort(Comparator.comparingLong(MidiEvent::getTick)); // stable, track order in a tick

        java.util.NavigableMap<Long, Long> tickFrames = new TreeMap<>();
        java.lang.reflect.Field tickNow = MLDPlayer.class.getDeclaredField("tickNow");
        tickNow.setAccessible(true);
        java.lang.reflect.Field timebase = MLDPlayer.class.getDeclaredField("currentTimebase");
        timebase.setAccessible(true);
        MLDPlayer[] player = new MLDPlayer[1];
        RecordingSampler[] recorder = new RecordingSampler[1];
        SamplerProvider provider = rate -> {
            recorder[0] = (RecordingSampler) RecordingSampler.of(samplerProvider().instance(rate), new ArrayList<>());
            recorder[0].listener = new RecordingSampler.Listener() {
                @Override public void call() {
                    try {
                        if (player[0] != null) tickFrames.putIfAbsent((Long) tickNow.get(player[0]), recorder[0].frame);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
                @Override public void render(long frame, int frames) {
                    if (frames > 0) chunks.add(frame);
                }
            };
            return recorder[0];
        };
        int mfiTimebase;
        try (InputStream is = new BufferedInputStream(Files.newInputStream(Path.of(mfi)))) {
            player[0] = new MLDPlayer(new MLD(is), provider, 48000f);
            player[0].setLoopEnabled(false);
            float[] samples = new float[4096 * 2];
            long frames = 0, limit = 48000L * 60 * 10;
            while (!player[0].isFinished() && frames < limit) {
                int rendered = player[0].render(samples, 0, 4096, 0f);
                if (rendered == -1) break;
                frames += Math.max(rendered, 0);
            }
            mfiTimebase = (int) timebase.get(player[0]);
        }

        List<long[]> times = new ArrayList<>();
        int resolution = sequence.getResolution();
        for (MidiEvent event : events) {
            long mfiTick = event.getTick() * mfiTimebase / resolution;
            // one the player calls nothing at does not sound, the frame before will do
            Map.Entry<Long, Long> entry = tickFrames.floorEntry(mfiTick);
            long frame = entry == null ? 0 : entry.getValue();
            times.add(new long[] {Math.round(frame * 1_000_000d / 48000)});
        }
        return times;
    }

    /** the converted midi of {@link #mfi} through the synthesizer, each message on its frame */
    float[] renderBySynthesizer(OpenDojaSynthesizer synthesizer) throws Exception {
        Sequence sequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(Path.of(mfi))));
        List<MidiEvent> events = new ArrayList<>();
        List<Long> chunks = new ArrayList<>();
        List<long[]> times = timeline(sequence, events, chunks);
        synthesizer.openStream();
        try {
            Receiver receiver = synthesizer.getReceiver();
            for (int i = 0; i < events.size(); i++) receiver.send(events.get(i).getMessage(), times.get(i)[0]);
            long end = times.isEmpty() ? 0 : times.getLast()[0];
            int frames = (int) (end * 48000L / 1_000_000) + 48000 * 2; // and the release tails
            float[] samples = new float[frames * 2];
            // in the runs of frames the player rendered
            int o = 0;
            for (long chunk : chunks) {
                if (chunk > o && chunk <= frames) {
                    synthesizer.render(samples, o * 2, (int) chunk - o);
                    o = (int) chunk;
                }
            }
            for (; o < frames; o += 4096) {
                synthesizer.render(samples, o * 2, Math.min(4096, frames - o));
            }
            return samples;
        } finally {
            synthesizer.close();
        }
    }

    /** {@link #mfi} as the openDoja player plays it */
    float[] renderByPlayer() throws Exception {
        try (InputStream is = new BufferedInputStream(Files.newInputStream(Path.of(mfi)))) {
            MLDPlayer player = new MLDPlayer(new MLD(is), samplerProvider(), 48000f);
            player.setLoopEnabled(false);
            List<Float> out = new ArrayList<>();
            float[] samples = new float[4096 * 2];
            long frames = 0, limit = 48000L * 60 * 10;
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            while (!player.isFinished() && frames < limit) {
                int rendered = player.render(samples, 0, 4096, 1f);
                if (rendered == -1) break;
                for (int i = 0; i < rendered * 2; i++) dos.writeFloat(samples[i]);
                frames += Math.max(rendered, 0);
            }
            java.nio.FloatBuffer fb = java.nio.ByteBuffer.wrap(baos.toByteArray()).asFloatBuffer();
            float[] result = new float[fb.remaining()];
            fb.get(result);
            return result;
        }
    }

    /**
     * The sound itself: the synthesizer takes every message on its frame, so what it renders
     * out of the converted midi is what the player renders out of the file.
     */
    @Test
    @DisplayName("the synthesizer sounds like MLDPlayer")
    void testRenderParity() throws Exception {
        assumeTrue(Files.exists(Path.of(mfi)), mfi + " not found");
        assumeTrue(mfi.toLowerCase().endsWith(".mld"), mfi + " is not an mfi file");

        float[] expected = renderByPlayer();
        OpenDojaSynthesizer synthesizer = switch (provider) {
            case "fuetrek" -> new FuetrekSynthesizer();
            default -> new Ma3Synthesizer();
        };
        float[] actual = renderBySynthesizer(synthesizer);

        // a jump of the file the player repeats, the midi plays it through once
        int n = Math.min(expected.length, actual.length - 48000 * 2 * 2);
        double signal = 0, noise = 0;
        int firstDiff = -1;
        for (int i = 0; i < n; i++) {
            double d = expected[i] - actual[i];
            signal += expected[i] * (double) expected[i];
            noise += d * d;
            if (firstDiff < 0 && Math.abs(d) > 1e-4) firstDiff = i / 2;
        }
        double snr = 10 * Math.log10(signal / Math.max(noise, 1e-30));
Debug.printf("player: %d frames, synthesizer: %d frames, snr: %.2f dB, first difference at frame %d (%.3f s)",
        expected.length / 2, actual.length / 2, snr, firstDiff, firstDiff / 48000d);
        if (snr <= 40) { // to listen to
            dump(expected, Path.of("tmp", "opendoja_player.wav"));
            dump(actual, Path.of("tmp", "opendoja_synthesizer.wav"));
        }
        assertTrue(snr > 40, "snr: " + snr);
    }

    static void dump(float[] samples, Path path) throws Exception {
        byte[] pcm = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int v = Math.clamp(Math.round(samples[i] * Short.MAX_VALUE), Short.MIN_VALUE, Short.MAX_VALUE);
            pcm[i * 2] = (byte) v;
            pcm[i * 2 + 1] = (byte) (v >> 8);
        }
        javax.sound.sampled.AudioFormat format = new javax.sound.sampled.AudioFormat(48000, 16, 2, true, false);
        Files.createDirectories(path.getParent());
        Files.deleteIfExists(path); // the writer does not truncate
        javax.sound.sampled.AudioSystem.write(new javax.sound.sampled.AudioInputStream(new java.io.ByteArrayInputStream(pcm), format, samples.length / 2),
                javax.sound.sampled.AudioFileFormat.Type.WAVE, path.toFile());
    }

    /**
     * A {@link Sampler} which writes down every call but a render.
     *
     * @see RecordingSampler#of the raw ext-B path of a sampler is only taken for one which
     *      takes raw ext-B events, so the recorder must not be one when the sampler is not
     */
    static class RecordingSampler implements Sampler {

        /** whether a call is written down with its frame */
        static boolean timed;

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

        /** frames rendered so far */
        long frame;

        /** told of every call */
        interface Listener {
            void call();
            default void render(long frame, int frames) {}
        }

        Listener listener = () -> {};

        void add(String call) {
            listener.call();
            calls.add(timed ? frame + " " + call : call);
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
        @Override public void render(float[] s, int o, int f) { frame += f; sampler.render(s, o, f); }
        @Override public void render(float[] s, int o, int f, float a) { frame += f; sampler.render(s, o, f, a); }
        @Override public void render(float[] s, int o, int f, float l, float r) { frame += f; sampler.render(s, o, f, l, r); }
        @Override public void render(float[] s, int o, int f, float l, float r, boolean e, boolean c) { listener.render(frame, f); frame += f; sampler.render(s, o, f, l, r, e, c); }
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
