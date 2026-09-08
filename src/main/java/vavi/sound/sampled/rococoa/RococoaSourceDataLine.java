/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.rococoa.Foundation;
import org.rococoa.ObjCBlocks.BlockLiteral;
import vavi.util.ByteUtil;
import vavix.rococoa.avfoundation.AVAudioEngine;
import vavix.rococoa.avfoundation.AVAudioFormat;
import vavix.rococoa.avfoundation.AVAudioNode;
import vavix.rococoa.avfoundation.AVAudioPCMBuffer;
import vavix.rococoa.avfoundation.AVAudioPlayerNode;
import vavix.rococoa.avfoundation.AVAudioUnitComponent;
import vavix.rococoa.avfoundation.AVAudioUnitComponentManager;
import vavix.rococoa.avfoundation.AVAudioUnitEffect;
import vavix.rococoa.avfoundation.AudioComponentDescription;

import static java.lang.System.getLogger;
import static org.rococoa.ObjCBlocks.block;


/**
 * A {@link SourceDataLine} whose output is rendered by an {@link AVAudioEngine}, optionally
 * through a chain of AudioUnit effects.
 * <p>
 * The point is to give anything that writes PCM to a source data line &mdash; gervill above all
 * &mdash; an AudioUnit insert chain, without a second clock. {@link #write(byte[], int, int)}
 * blocks until the player node has consumed a buffer, so core audio's HAL clock, and nothing
 * else, paces the producer.
 * <p>
 * graph: {@link AVAudioPlayerNode} &rarr; {@code aufx}&hellip; &rarr; {@code mainMixerNode} &rarr; {@code outputNode}
 * <p>
 * The engine always runs at float32, non interleaved, stereo, because that is what
 * AudioUnits are happiest with; mono input is duplicated.
 * <p>
 * system property
 * <li> {@code vavi.sound.sampled.rococoa.RococoaSourceDataLine.effects} ... effect chain, comma
 *      separated {@code "manufacturer:subtype"} (component type defaults to {@code aufx}) or
 *      {@code "type:manufacturer:subtype"}, each optionally followed by {@code "?name=value;name=value"},
 *      default none. ex. {@code "appl:mrev?Dry/Wet Mix=20,appl:dely?Dry/Wet Mix=15;Delay Time=0.25"} </li>
 * <p>
 * usage with gervill:
 * <pre>
 *  System.setProperty("javax.sound.sampled.SourceDataLine", "#Rococoa Mixer");
 *  Synthesizer synthesizer = MidiSystem.getSynthesizer(); // "#Gervill"
 *  synthesizer.open();
 * </pre>
 * or, when you want to keep a handle on the chain:
 * <pre>
 *  RococoaSourceDataLine line = new RococoaSourceDataLine();
 *  line.setEffects("appl:mrev");
 *  line.open(new AudioFormat(44100, 16, 2, true, false));
 *  // AUMatrixReverb is 100% wet out of the box, which is far too much
 *  line.getEffects().getFirst().setParameter("Dry/Wet Mix", 20);
 *  new SoftSynthesizer().open(line, null);
 * </pre>
 * {@link vavix.rococoa.avfoundation.AVAudioUnit#getParameters()} lists what an audio unit takes,
 * with the id, the range and the default of each parameter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-08 nsano initial version <br>
 */
public class RococoaSourceDataLine implements SourceDataLine {

    private static final Logger logger = getLogger(RococoaSourceDataLine.class.getName());

    /** the engine graph is always stereo, mono input is duplicated */
    private static final int GRAPH_CHANNELS = 2;

    /** how many pcm buffers are cycled between us and the player node */
    private static final int RING = 4;

    private static final int MIN_FRAMES_PER_BUFFER = 256;

    private static final int MAX_FRAMES_PER_BUFFER = 8192;

    /** */
    public static final DataLine.Info info = new DataLine.Info(SourceDataLine.class,
            new AudioFormat[] {
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 16, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 16, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true),
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 24, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 24, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true),
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 32, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 32, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true),
                    new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, AudioSystem.NOT_SPECIFIED, 8, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
                    new AudioFormat(AudioFormat.Encoding.PCM_FLOAT, AudioSystem.NOT_SPECIFIED, 32, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
                    new AudioFormat(AudioFormat.Encoding.PCM_FLOAT, AudioSystem.NOT_SPECIFIED, 32, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true),
            },
            AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED);

    /** whether we can play a line described by {@code info} */
    public static boolean supports(Line.Info requested) {
        if (!requested.getLineClass().isAssignableFrom(RococoaSourceDataLine.class)) {
            return false;
        }
        if (requested instanceof DataLine.Info dataLineInfo) {
            for (AudioFormat format : dataLineInfo.getFormats()) {
                if (info.isFormatSupported(format)) {
                    return true;
                }
            }
            return dataLineInfo.getFormats().length == 0;
        }
        return true;
    }

//#region core audio

    private AVAudioEngine engine;

    private AVAudioPlayerNode player;

    private final List<AVAudioUnitEffect> effects = new ArrayList<>();

    private AVAudioFormat graphFormat;

    private AVAudioPCMBuffer[] buffers;

    /** kept alive as long as the line is open, it is a global block so reusing it is fine */
    private BlockLiteral completionHandler;

    /** effect chain spec, {@code null} means "ask the system property" */
    private String effectsSpec;

//#endregion

    private AudioFormat format;

    private int framesPerBuffer;

    /** permits for buffer slots not owned by the player node */
    private Semaphore free;

    /** frame length of each scheduled buffer, in schedule order */
    private ArrayBlockingQueue<Integer> inFlight;

    /** the slot being filled, {@code null} when none is */
    private AVAudioPCMBuffer current;

    private int writeIndex;

    private int fillPosition;

    /** de-interleaved float staging area for the slot being filled */
    private float[][] scratch;

    private final AtomicLong framesPlayed = new AtomicLong();

    private volatile boolean open;

    private volatile boolean running;

    private final List<LineListener> listeners = new ArrayList<>();

//#region controls

    private final Gain gainControl = new Gain();

    private final Mute muteControl = new Mute();

    private final Control[] controls = new Control[] {gainControl, muteControl};

    /** master gain, applied to the tail of the chain so effects still see the raw signal */
    private final class Gain extends FloatControl {

        static float linearToDB(float linear) {
            return (float) (Math.log(linear == 0.0f ? 0.0001 : linear) / Math.log(10.0) * 20.0);
        }

        static float dBToLinear(float dB) {
            return (float) Math.pow(10.0, dB / 20.0);
        }

        private float linearGain = 1.0f;

        Gain() {
            super(FloatControl.Type.MASTER_GAIN,
                    linearToDB(0.0f),
                    linearToDB(2.0f),
                    Math.abs(linearToDB(1.0f) - linearToDB(0.0f)) / 128.0f,
                    -1,
                    0.0f,
                    "dB", "Minimum", "", "Maximum");
        }

        @Override
        public void setValue(float newValue) {
            float newLinearGain = dBToLinear(newValue);
            super.setValue(linearToDB(newLinearGain));
            linearGain = newLinearGain;
            applyVolume();
        }

        float getLinearGain() {
            return linearGain;
        }
    }

    private final class Mute extends BooleanControl {

        Mute() {
            super(BooleanControl.Type.MUTE, false, "True", "False");
        }

        @Override
        public void setValue(boolean value) {
            super.setValue(value);
            applyVolume();
        }
    }

    private void applyVolume() {
        if (engine != null) {
            engine.mainMixerNode().setOutputVolume(muteControl.getValue() ? 0 : gainControl.getLinearGain());
        }
    }

//#endregion

//#region effect chain

    /**
     * Sets the effect chain, overriding the system property. Takes effect on the next
     * {@link #open(AudioFormat, int)}.
     *
     * @param spec comma separated {@code "manufacturer:subtype"} or {@code "type:manufacturer:subtype"},
     *             each optionally followed by {@code "?name=value;name=value"},
     *             ex. {@code "appl:mrev?Dry/Wet Mix=20,appl:dely"}, {@code null} or empty for a dry chain
     */
    public void setEffects(String spec) {
        if (open) {
            throw new IllegalStateException("close the line before changing the effect chain");
        }
        this.effectsSpec = spec == null ? "" : spec;
    }

    /** the instantiated AudioUnits, in chain order, to tweak parameters or show their view */
    public List<AVAudioUnitEffect> getEffects() {
        return List.copyOf(effects);
    }

    /** {@code "manufacturer:subtype"} or {@code "type:manufacturer:subtype"}, without the parameters */
    static AudioComponentDescription toDescription(String spec) {
        String[] parts = spec.split(":");
        String type, manufacturer, subType;
        switch (parts.length) {
            case 2 -> { type = "aufx"; manufacturer = parts[0]; subType = parts[1]; }
            case 3 -> { type = parts[0]; manufacturer = parts[1]; subType = parts[2]; }
            default -> throw new IllegalArgumentException("not a component spec: " + spec);
        }
        AudioComponentDescription description = new AudioComponentDescription();
        description.componentType = ByteUtil.readBeInt(fourCC(type));
        description.componentManufacturer = ByteUtil.readBeInt(fourCC(manufacturer));
        description.componentSubType = ByteUtil.readBeInt(fourCC(subType));
        description.componentFlags = 0;
        description.componentFlagsMask = 0;
        return description;
    }

    private static byte[] fourCC(String value) {
        byte[] bytes = value.getBytes();
        if (bytes.length != 4) {
            throw new IllegalArgumentException("not a 4 character code: " + value);
        }
        return bytes;
    }

    /**
     * Applies the {@code "?name=value;name=value"} tail of an effect spec. A key is a parameter
     * id when it parses as an integer, otherwise a display name, matched ignoring case.
     *
     * @see vavix.rococoa.avfoundation.AVAudioUnit#getParameters()
     */
    private static void applyParameters(AVAudioUnitEffect effect, String params) {
        for (String assignment : params.split(";")) {
            assignment = assignment.trim();
            if (assignment.isEmpty()) {
                continue;
            }
            int equals = assignment.indexOf('=');
            if (equals < 0) {
                throw new IllegalArgumentException("not a parameter assignment: " + assignment);
            }
            String key = assignment.substring(0, equals).trim();
            float value;
            try {
                value = Float.parseFloat(assignment.substring(equals + 1).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("not a parameter value: " + assignment, e);
            }
            try {
                effect.setParameter(Integer.parseInt(key), value);
            } catch (NumberFormatException e) {
                effect.setParameter(key, value);
            }
logger.log(Level.DEBUG, "parameter: " + effect.name() + "[" + key + "] = " + value);
        }
    }

    private void createEffects() {
        String spec = effectsSpec != null ? effectsSpec :
                System.getProperty(RococoaSourceDataLine.class.getName() + ".effects", "");
        for (String each : spec.split(",")) {
            each = each.trim();
            if (each.isEmpty()) {
                continue;
            }
            int question = each.indexOf('?');
            String component = question < 0 ? each : each.substring(0, question).trim();
            AudioComponentDescription description = toDescription(component);
            // AVAudioUnitEffect#init aborts the jvm on an unknown description, so look it up first
            List<AVAudioUnitComponent> components = AVAudioUnitComponentManager.shared().components(description);
            if (components.isEmpty()) {
                throw new IllegalArgumentException("no such AudioUnit: " + component);
            }
            AVAudioUnitEffect effect = AVAudioUnitEffect.init(components.getFirst().audioComponentDescription());
            if (effect == null) {
                throw new IllegalArgumentException("cannot instantiate AudioUnit: " + component);
            }
logger.log(Level.DEBUG, "effect: " + component + ", " + effect.name() + ", " + effect.manufacturerName());
            if (question >= 0) {
                applyParameters(effect, each.substring(question + 1));
            }
            effects.add(effect);
        }
    }

//#endregion

    @Override
    public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
        if (open) {
            throw new IllegalStateException("already open");
        }
        if (!info.isFormatSupported(format)) {
            throw new IllegalArgumentException("unsupported format: " + format);
        }
        if (format.getChannels() < 1 || format.getChannels() > GRAPH_CHANNELS) {
            throw new IllegalArgumentException("unsupported channels: " + format.getChannels());
        }

        this.format = format;
        double sampleRate = format.getSampleRate() != AudioSystem.NOT_SPECIFIED ? format.getSampleRate() : 44100;

        int totalFrames = bufferSize > 0 ? bufferSize / format.getFrameSize() : (int) (sampleRate / 5);
        this.framesPerBuffer = Math.clamp(totalFrames / RING, MIN_FRAMES_PER_BUFFER, MAX_FRAMES_PER_BUFFER);

        try {
            createEffects();

            engine = AVAudioEngine.newInstance();
            player = AVAudioPlayerNode.newInstance();
            graphFormat = AVAudioFormat.init(AVAudioFormat.PCMFormatFloat32, sampleRate, GRAPH_CHANNELS, false);

            engine.attachNode(player);
            effects.forEach(engine::attachNode);

            AVAudioNodeChain chain = new AVAudioNodeChain();
            chain.connect(player);
            effects.forEach(chain::connect);
            chain.connect(engine.mainMixerNode());

            buffers = new AVAudioPCMBuffer[RING];
            for (int i = 0; i < RING; i++) {
                buffers[i] = AVAudioPCMBuffer.init(graphFormat, framesPerBuffer);
            }
            scratch = new float[GRAPH_CHANNELS][framesPerBuffer];
            free = new Semaphore(RING);
            inFlight = new ArrayBlockingQueue<>(RING);
            completionHandler = block((AVAudioPlayerNode.CompletionHandler) literal -> {
                Integer frames = inFlight.poll();
                if (frames != null) {
                    framesPlayed.addAndGet(frames);
                }
                free.release();
            });

            player.prepareWithFrameCount(framesPerBuffer);
            engine.prepare();
            if (!engine.start()) {
                throw new LineUnavailableException("cannot start AVAudioEngine");
            }
        } catch (RuntimeException | LineUnavailableException e) {
            disposeEngine();
            throw e;
        }

        writeIndex = 0;
        fillPosition = 0;
        current = null;
        framesPlayed.set(0);
        open = true;
        applyVolume();
        fireUpdate(new LineEvent(this, LineEvent.Type.OPEN, AudioSystem.NOT_SPECIFIED));
    }

    /** connects nodes pairwise as they are handed over */
    private final class AVAudioNodeChain {
        private AVAudioNode previous;
        void connect(AVAudioNode node) {
            if (previous != null) {
                engine.connect_to_format(previous, node, graphFormat);
            }
            previous = node;
        }
    }

    @Override
    public void open(AudioFormat format) throws LineUnavailableException {
        open(format, 0);
    }

    @Override
    public void open() throws LineUnavailableException {
        if (format == null) {
            throw new IllegalArgumentException("format is not set, use open(AudioFormat)");
        }
        open(format, 0);
    }

    @Override
    public void start() {
        if (!open || running) {
            return;
        }
        running = true;
        player.play();
        fireUpdate(new LineEvent(this, LineEvent.Type.START, getLongFramePosition()));
    }

    @Override
    public void stop() {
        if (!open || !running) {
            return;
        }
        running = false;
        player.pause();
        fireUpdate(new LineEvent(this, LineEvent.Type.STOP, getLongFramePosition()));
    }

    @Override
    public int write(byte[] b, int off, int len) {
        if (!open) {
            return 0;
        }
        int frameSize = format.getFrameSize();
        len -= len % frameSize;
        int written = 0;
        while (written < len) {
            if (current == null && !acquire()) {
                break;
            }
            int frames = Math.min(framesPerBuffer - fillPosition, (len - written) / frameSize);
            deinterleave(b, off + written, frames);
            fillPosition += frames;
            written += frames * frameSize;
            if (fillPosition == framesPerBuffer) {
                schedule();
            }
        }
        return written;
    }

    /** takes the next buffer slot back from the player node, blocking. */
    private boolean acquire() {
        try {
            while (open) {
                if (free.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                    current = buffers[writeIndex];
                    writeIndex = (writeIndex + 1) % RING;
                    fillPosition = 0;
                    return true;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /** hands the slot being filled over to the player node. */
    private void schedule() {
        if (current == null || fillPosition == 0) {
            return;
        }
        Pointer channels = current.floatChannelData();
        for (int c = 0; c < GRAPH_CHANNELS; c++) {
            channels.getPointer((long) c * Native.POINTER_SIZE).write(0, scratch[c], 0, fillPosition);
        }
        current.setFrameLength(fillPosition);
        inFlight.add(fillPosition);
        player.scheduleBuffer(current, completionHandler);
        current = null;
        fillPosition = 0;
    }

    /**
     * converts one chunk of the caller's interleaved pcm into the de-interleaved float
     * staging area, duplicating mono into both graph channels.
     */
    private void deinterleave(byte[] b, int off, int frames) {
        int channels = format.getChannels();
        int bytes = format.getSampleSizeInBits() / 8;
        boolean bigEndian = format.isBigEndian();
        boolean isFloat = format.getEncoding().equals(AudioFormat.Encoding.PCM_FLOAT);
        boolean isUnsigned = format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED);
        int shift = 32 - bytes * 8;
        for (int f = 0; f < frames; f++) {
            for (int c = 0; c < channels; c++) {
                int p = off + (f * channels + c) * bytes;
                int x = 0;
                if (bigEndian) {
                    for (int i = 0; i < bytes; i++) {
                        x = (x << 8) | (b[p + i] & 0xff);
                    }
                } else {
                    for (int i = bytes - 1; i >= 0; i--) {
                        x = (x << 8) | (b[p + i] & 0xff);
                    }
                }
                float v;
                if (isFloat) {
                    v = Float.intBitsToFloat(x);
                } else {
                    x <<= shift;
                    if (isUnsigned) {
                        x ^= 0x8000_0000;
                    }
                    v = x * (1f / 0x8000_0000L);
                }
                scratch[c][fillPosition + f] = v;
            }
            if (channels == 1) {
                scratch[1][fillPosition + f] = scratch[0][fillPosition + f];
            }
        }
    }

    @Override
    public void drain() {
        if (!open) {
            return;
        }
        schedule();
        if (!running) {
            return;
        }
        if (!awaitIdle()) {
            return;
        }
        try {
            // "consumed" is one render ahead of "heard", let the tail out of the device
            Thread.sleep((long) (framesPerBuffer * 1000.0 / graphFormat.sampleRate()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void flush() {
        if (!open) {
            return;
        }
        current = null;
        fillPosition = 0;
        // stop() unschedules the pending buffers, their completion handlers still run
        player.stop();
        awaitIdle();
        inFlight.clear();
        if (running) {
            player.play();
        }
    }

    /** waits until the player node holds none of our buffers. */
    private boolean awaitIdle() {
        try {
            while (open) {
                if (free.tryAcquire(RING, 100, TimeUnit.MILLISECONDS)) {
                    free.release(RING);
                    return true;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public void close() {
        if (!open) {
            return;
        }
        open = false;
        running = false;
        disposeEngine();
        fireUpdate(new LineEvent(this, LineEvent.Type.CLOSE, getLongFramePosition()));
    }

    private void disposeEngine() {
        if (player != null) {
            player.stop();
        }
        if (engine != null) {
            engine.stop();
        }
        if (completionHandler != null) {
            Foundation.getRococoaLibrary().releaseObjCBlock(completionHandler.getPointer());
            completionHandler = null;
        }
        effects.clear();
        buffers = null;
        scratch = null;
        current = null;
        player = null;
        engine = null;
        graphFormat = null;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isActive() {
        return running && player != null && player.isPlaying();
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public int getBufferSize() {
        return format == null ? 0 : RING * framesPerBuffer * format.getFrameSize();
    }

    @Override
    public int available() {
        if (!open) {
            return 0;
        }
        int frames = free.availablePermits() * framesPerBuffer + (current != null ? framesPerBuffer - fillPosition : 0);
        return frames * format.getFrameSize();
    }

    @Override
    public int getFramePosition() {
        return (int) getLongFramePosition();
    }

    @Override
    public long getLongFramePosition() {
        return framesPlayed.get();
    }

    @Override
    public long getMicrosecondPosition() {
        double sampleRate = graphFormat != null ? graphFormat.sampleRate() : 44100;
        return (long) (getLongFramePosition() * 1000000.0 / sampleRate);
    }

    @Override
    public float getLevel() {
        return AudioSystem.NOT_SPECIFIED;
    }

    @Override
    public Line.Info getLineInfo() {
        return info;
    }

    @Override
    public Control[] getControls() {
        return controls;
    }

    @Override
    public boolean isControlSupported(Type control) {
        for (Control c : controls) {
            if (c.getType().equals(control)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Control getControl(Type control) {
        for (Control c : controls) {
            if (c.getType().equals(control)) {
                return c;
            }
        }
        throw new IllegalArgumentException("control not supported: " + control);
    }

    @Override
    public void addLineListener(LineListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeLineListener(LineListener listener) {
        listeners.remove(listener);
    }

    private void fireUpdate(LineEvent event) {
        listeners.forEach(l -> l.update(event));
    }
}
