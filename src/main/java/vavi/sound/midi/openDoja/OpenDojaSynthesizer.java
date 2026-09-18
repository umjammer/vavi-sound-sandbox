/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.openDoja;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.midi.Instrument;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import opendoja.audio.mld.MLD;
import opendoja.audio.mld.MLDRawExtBHandler;
import opendoja.audio.mld.Sampler;
import opendoja.audio.mld.SamplerProvider;
import vavi.sound.mfi.vavi.sequencer.MfiValueExclusive;
import vavi.sound.mobile.MobileExclusive;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer.MFi_SYSEX_FUNCTION_ID_MACHINE_DEPENDENT;
import static vavi.sound.midi.VaviMidiDeviceProvider.MANUFACTURER_ID;


/**
 * A {@link Synthesizer} in front of an openDoja {@link Sampler}.
 * <p>
 * An openDoja sampler is the sound source of an mfi (".mld") file and is played by
 * {@code opendoja.audio.mld.MLDPlayer}, which hands it the file's own values, not midi ones.
 * The midi a converted mfi file is ({@code ../vavi-sound:VaviMfiMidiConverter}) says most of
 * that: an mfi volume, panpot, pitch bend or velocity is its midi counterpart doubled, and an
 * mfi percussion note is a note on {@value #CHANNEL_DRUM} 10 semitones down. What midi has no
 * room for comes along as the exclusives of {@link MfiValueExclusive} (bank, master volume,
 * fine pitch bend, pitch bend range, channel configuration, expression) and, for the voices and waves of the file itself, as the
 * machine dependent exclusive a {@link MobileExclusive#pack packed} vavi sysex carries.
 * </p>
 * <p>
 * So the receiver puts the mfi values back together and drives the sampler the way
 * {@code MLDPlayer} does:
 * </p>
 * <ul>
 *  <li>a sampler which takes raw ext-B opcodes ({@link MLDRawExtBHandler}, the fuetrek one) gets
 *      the 6 bit mfi bytes as they are, as {@code FueTrekMLDPlaybackEngine} gives them;</li>
 *  <li>any other sampler (the ma-3 one) gets the normalized values
 *      {@code MLDNormalizedEventDispatcher} computes, the volume curve of
 *      {@link #volumeToAmplitude} included;</li>
 *  <li>the machine dependent exclusive goes to {@link Sampler#sysEx} from its vendor byte on,
 *      which is where an ext-info event of the file would have started. Without it a fuetrek
 *      file has no ucs voices and an ma-3 one no wave drums, which is why it sounded wrong.</li>
 * </ul>
 * <p>
 * The universal master volume is the listener's and scales the rendering, unless it is the one
 * {@code MasterVolumeMessage} sends right behind its own exclusive, which is the song's.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/07/09 nsano initial version <br>
 *          0.01 2026-09-18 nsano drive the sampler with mfi values <br>
 */
public abstract class OpenDojaSynthesizer implements Synthesizer {

    private static final Logger logger = getLogger(OpenDojaSynthesizer.class.getName());

    private static final int MAX_CHANNEL = 16;

    /**
     * The channel {@code vavi.sound.mfi.vavi.MidiContext} gathers every mfi percussion channel
     * on, so the only channel a converted mfi file can drum on.
     */
    private static final int CHANNEL_DRUM = 9;

    /**
     * The semitones {@code MidiContext#retrievePitch} takes off a note on {@link #CHANNEL_DRUM},
     * which {@link OpenDojaMidiChannel#key} puts back so that the sampler gets the key the mld
     * parser would have read out of the file.
     */
    private static final int DRUM_PITCH_SHIFT = 10;

    /** ext-B: a level relative to the channel volume */
    private static final int EVENT_LEVEL_DELTA = 0xe6;

    /** ext-B: modulation depth */
    private static final int EVENT_MODULATION = 0xea;

    private static final float SAMPLE_RATE = 48000.0f;
    private static final int BLOCK_SIZE = 1024;

    private final AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);

    /**
     * How often each key of each sampler channel is on. An mfi note longer than its gate time
     * overlaps the next one on the same key, which the player answers by keeping the one note
     * and putting off its key off, see {@code MLDPlayer#evtNote}. The midi the converter makes
     * of such a pair is a note on, a note on, a note off and a note off, so they are counted.
     * Per sampler channel and key, not per midi channel and note, as the converter gathers
     * channels on one. Indexed by the key + 69.
     */
    private final int[][] keysOn = new int[MAX_CHANNEL][128 + DRUM_PITCH_SHIFT];

    private final OpenDojaMidiChannel[] channels = new OpenDojaMidiChannel[MAX_CHANNEL];
    private final List<VoiceStatus> voiceStatuses = new ArrayList<>();
    private final List<Receiver> receivers = new ArrayList<>();

    private final Object lock = new Object();

    private long timestamp;
    private volatile boolean isOpen;
    private SourceDataLine line;
    private Sampler sampler;

    /** the listener's volume, the amplitude the sampler renders with */
    private volatile float masterGain = 1.0f;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "OpenDoja Renderer");
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.setDaemon(true);
        return thread;
    });

    protected OpenDojaSynthesizer() {
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new OpenDojaMidiChannel(i);
        }
    }

    protected abstract SamplerProvider createSamplerProvider();

    /**
     * The amplitude an mfi volume of 0 ~ 1 sounds at.
     *
     * @see "opendoja.audio.mld.MLD#volumeToAmplitude"
     */
    static float volumeToAmplitude(float param) {
        param = Math.clamp(param, 0f, 1f);
        return param == 0.0f ? 0.0f : (float) Math.pow(2, (1 - param) * -96 / 20);
    }

    @Override
    public void open() throws MidiUnavailableException {
        if (isOpen()) {
            logger.log(Level.WARNING, "already open: " + hashCode());
            return;
        }

        synchronized (lock) {
            SamplerProvider provider = createSamplerProvider();
            sampler = provider.instance(SAMPLE_RATE);
            sampler.reset(); // as MLDPlayer does before a sequence
            for (OpenDojaMidiChannel channel : channels) {
                channel.reset();
            }
        }

        try {
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
            line = (SourceDataLine) AudioSystem.getLine(lineInfo);
            line.addLineListener(event -> logger.log(Level.DEBUG, "Line: " + event.getType()));
            line.open(audioFormat);
            line.start();
        } catch (LineUnavailableException e) {
            throw (MidiUnavailableException) new MidiUnavailableException().initCause(e);
        }

        timestamp = 0;
        isOpen = true;

        executor.submit(this::play);
    }

    private void play() {
        float[] samples = new float[BLOCK_SIZE * 2];
        byte[] buf = new byte[BLOCK_SIZE * 4];

        while (isOpen) {
            try {
                float gain = masterGain;
                synchronized (lock) {
                    if (sampler != null) {
                        sampler.render(samples, 0, BLOCK_SIZE, gain, gain, true, true);
                    } else {
                        Arrays.fill(samples, 0.0f);
                    }
                }

                int output = 0;
                for (int i = 0; i < BLOCK_SIZE * 2; i++) {
                    float sample = Math.clamp(samples[i], -1.0f, 1.0f);
                    int value = Math.round(sample * Short.MAX_VALUE);
                    buf[output++] = (byte) (value & 0xFF);
                    buf[output++] = (byte) ((value >>> 8) & 0xFF);
                }

                line.write(buf, 0, buf.length);
            } catch (Exception e) {
                logger.log(Level.INFO, e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() {
        isOpen = false;
        for (Receiver receiver : new ArrayList<>(receivers)) {
            receiver.close();
        }
        executor.shutdown();
        if (line != null) {
            line.drain();
            line.close();
        }
        synchronized (lock) {
            if (sampler != null) {
                sampler.stopAll();
                sampler = null;
            }
        }
        voiceStatuses.clear();
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public long getMicrosecondPosition() {
        return timestamp;
    }

    @Override
    public int getMaxReceivers() {
        return -1;
    }

    @Override
    public int getMaxTransmitters() {
        return 0;
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return new OpenDojaReceiver();
    }

    @Override
    public List<Receiver> getReceivers() {
        return receivers;
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        throw new MidiUnavailableException("No transmitter available");
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return Collections.emptyList();
    }

    @Override
    public int getMaxPolyphony() {
        return 64;
    }

    @Override
    public long getLatency() {
        return (long) (BLOCK_SIZE / SAMPLE_RATE * 1_000_000);
    }

    @Override
    public MidiChannel[] getChannels() {
        return channels;
    }

    @Override
    public VoiceStatus[] getVoiceStatus() {
        synchronized (lock) {
            return voiceStatuses.toArray(VoiceStatus[]::new);
        }
    }

    @Override
    public boolean isSoundbankSupported(Soundbank soundbank) {
        return false;
    }

    @Override
    public boolean loadInstrument(Instrument instrument) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void unloadInstrument(Instrument instrument) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public boolean remapInstrument(Instrument from, Instrument to) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Soundbank getDefaultSoundbank() {
        return null;
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return new Instrument[0];
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        return new Instrument[0];
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    // ----

    /**
     * Whether the sampler takes the mfi ext-B bytes as they are.
     * <p>
     * {@code FueTrekMLDPlaybackEngine} keeps the volume, panpot, pitch bend, program, bank,
     * drum enable and the lanes around them away from the normalized {@link Sampler} methods
     * and hands the sampler the raw parameter byte instead, because its sound source does not
     * mean by them what the shared mld flow does (0xe7 is a modulation seed, not a bend range).
     * </p>
     * <p>
     * Must be called with {@link #lock} held.
     * </p>
     */
    private boolean isRawExtB() {
        return sampler instanceof MLDRawExtBHandler;
    }

    /**
     * Must be called with {@link #lock} held.
     *
     * @param eventId an mfi ext-B opcode
     * @param param the mfi parameter byte, as the file has it
     */
    private void rawExtB(int eventId, int channel, int param) {
        ((MLDRawExtBHandler) sampler).handleRawExtBEvent(eventId, channel, param);
    }

    /**
     * The parameter byte of an mfi ext-B event: its channel in the top 2 bits, its 6 bit value
     * below them. A midi channel is the mfi track * 4 + the voice, so the voice is its low 2 bits.
     */
    private static int param(int channel, int value) {
        return ((channel & 3) << 6) | (value & 0x3f);
    }

    /**
     * A midi channel of the sampler.
     * <p>
     * The mfi value of a controller is the midi one halved, see
     * {@code vavi.sound.mfi.vavi.track}.
     * </p>
     */
    public class OpenDojaMidiChannel implements MidiChannel {

        private final int channel;
        private boolean mute = false;
        private boolean solo = false;
        private int program = 0;
        private int bank = 0;
        private int pitchBend = 8192;
        private final int[] control = new int[128];
        private final int[] polyPressure = new int[128];
        private int pressure = 0;

        /** how often each key of the sampler channel of this is on, see {@link #keysOn} */
        private final int[] keysOn;

        /**
         * The mfi channel the next channel message is from, -1 for this.
         * <p>
         * The converter moves a channel away from its own, every percussion one to
         * {@link #CHANNEL_DRUM} and a melody one on it to a free one, and sends the
         * {@link MfiValueExclusive#CHANNEL} exclusive right before each message moved.
         * </p>
         */
        private int origin = -1;

        /** the mfi program the next program change is, -1 for its own */
        private int mfiProgram = -1;

        public OpenDojaMidiChannel(int channel) {
            this.channel = channel;
            this.keysOn = OpenDojaSynthesizer.this.keysOn[channel];
            control[7] = 127; // default volume
        }

        /** Must be called with {@link #lock} held. */
        void reset() {
            Arrays.fill(keysOn, 0);
            origin = -1;
            mfiProgram = -1;
        }

        /**
         * The sampler channel of a message the converter may have moved, the origin taken.
         * <p>
         * Must be called with {@link #lock} held.
         * </p>
         */
        private int target() {
            int target = origin >= 0 ? origin : channel;
            origin = -1;
            return target;
        }

        /** Must be called with {@link #lock} held. */
        void origin(int channel) {
            this.origin = channel;
        }

        /** Must be called with {@link #lock} held. */
        void mfiProgram(int program) {
            this.mfiProgram = program;
        }

        /**
         * The key the sampler wants, semitones from A4.
         * <p>
         * A note is the mfi pitch + 45 and the key the mld parser reads the mfi pitch - 24, so
         * the key of a note is it - 69. On {@link #CHANNEL_DRUM} the note has
         * {@value #DRUM_PITCH_SHIFT} off it already, which is put back here: whether the
         * channel drums is the sound source's business, see {@link #channelConfiguration}.
         * </p>
         */
        private int key(int noteNumber) {
            return noteNumber - 69 + (channel == CHANNEL_DRUM ? DRUM_PITCH_SHIFT : 0);
        }

        @Override
        public void noteOn(int noteNumber, int velocity) {
            if (velocity == 0) {
                noteOff(noteNumber);
                return;
            }
            if (mute) {
                return;
            }
            noteNumber &= 0x7f;
            synchronized (lock) {
                int target = target();
                int[] keysOn = OpenDojaSynthesizer.this.keysOn[target];
                int key = key(noteNumber);
                boolean sounding = keysOn[key + 69]++ > 0;
                if (sampler != null && (!sounding || !sampler.suppressActiveKeyRetrigger())) {
                    // an mfi velocity is 6 bit, the midi one it became is it doubled
                    sampler.keyOn(target, key, Math.min(velocity / 126f, 1f));
                }
                if (sounding) {
                    return;
                }

                VoiceStatus voiceStatus = new VoiceStatus();
                voiceStatus.channel = target;
                voiceStatus.program = program;
                voiceStatus.note = noteNumber;
                voiceStatus.volume = velocity;
                voiceStatus.active = true;
                voiceStatuses.add(voiceStatus);
            }
        }

        @Override
        public void noteOff(int noteNumber, int velocity) {
            noteNumber &= 0x7f;
            synchronized (lock) {
                int target = target();
                int[] keysOn = OpenDojaSynthesizer.this.keysOn[target];
                int key = key(noteNumber);
                int count = keysOn[key + 69];
                if (count == 0) {
                    return;
                }
                keysOn[key + 69] = count - 1;
                if (count > 1) {
                    return;
                }
                if (sampler != null) {
                    sampler.keyOff(target, key);
                }

                VoiceStatus voiceStatus = find(target, noteNumber);
                if (voiceStatus != null) {
                    voiceStatus.active = false;
                    voiceStatuses.remove(voiceStatus);
                }
            }
        }

        @Override
        public void noteOff(int noteNumber) {
            noteOff(noteNumber, 0);
        }

        private VoiceStatus find(int channel, int noteNumber) {
            return voiceStatuses.stream()
                .filter(vs -> vs.channel == channel && vs.note == noteNumber)
                .findFirst().orElse(null);
        }

        @Override
        public void setPolyPressure(int noteNumber, int pressure) {
            polyPressure[noteNumber] = pressure;
        }

        @Override
        public int getPolyPressure(int noteNumber) {
            return polyPressure[noteNumber];
        }

        @Override
        public void setChannelPressure(int pressure) {
            this.pressure = pressure;
        }

        @Override
        public int getChannelPressure() {
            return pressure;
        }

        @Override
        public void controlChange(int controller, int value) {
            control[controller] = value;
            synchronized (lock) {
                int target = target();
                if (sampler == null) {
                    return;
                }
                switch (controller) {
                    case 1 -> { // modulation depth
                        if (isRawExtB()) {
                            rawExtB(EVENT_MODULATION, target, param(target, value >> 1));
                        }
                    }
                    case 7 -> volume(target, value >> 1);
                    case 10 -> panpot(target, value >> 1);
                    case 11 -> // mfi 0xe6, which comes as it is as the exclusive before this
                        logger.log(Level.TRACE, "expression: " + target + "ch, " + value);
                    case 120, 123 -> soundOff(target);
                    default -> logger.log(Level.TRACE, "unhandled control: " + controller + ", " + value);
                }
            }
        }

        /**
         * Must be called with {@link #lock} held.
         *
         * @param value mfi volume, 0 ~ 63
         */
        void volume(int target, int value) {
            value &= 0x3f;
            if (isRawExtB()) {
                rawExtB(MLD.EVENT_VOLUME, target, param(target, value));
            } else {
                sampler.volume(target, volumeToAmplitude(value / 63f));
            }
        }

        /**
         * Must be called with {@link #lock} held.
         *
         * @param value mfi panpot, 0 ~ 63, 32 is center
         */
        void panpot(int target, int value) {
            value &= 0x3f;
            if (isRawExtB()) {
                rawExtB(MLD.EVENT_PANPOT, target, param(target, value));
            } else {
                sampler.panpot(target, value < 32 ? value / 32f - 1 : (value - 32) / 31f);
            }
        }

        @Override
        public int getController(int controller) {
            return control[controller];
        }

        @Override
        public void programChange(int program) {
            this.program = program;
            synchronized (lock) {
                if (sampler == null) {
                    return;
                }
                // the midi program keeps the mfi program in its low 6 bits and only bit 0 of
                // the mfi bank above them, the bank itself comes as an exclusive
                int target = target();
                if (mfiProgram >= 0) {
                    program = mfiProgram;
                    mfiProgram = -1;
                }
                if (isRawExtB()) {
                    rawExtB(MLD.EVENT_PROGRAM_CHANGE, target, param(target, program & 0x3f));
                } else {
                    sampler.programChange(target, program & 0x3f);
                }
            }
        }

        @Override
        public void programChange(int bank, int program) {
            synchronized (lock) {
                bank(bank);
                programChange(program);
            }
        }

        /**
         * Must be called with {@link #lock} held.
         *
         * @param bank mfi bank, 0 ~ 63
         */
        void bank(int bank) {
            this.bank = bank;
            int target = target();
            if (sampler == null) {
                return;
            }
            if (isRawExtB()) {
                rawExtB(MLD.EVENT_BANK_CHANGE, target, param(target, bank & 0x3f));
            } else {
                sampler.bankChange(target, bank & 0x3f);
            }
        }

        @Override
        public int getProgram() {
            return program;
        }

        @Override
        public void setPitchBend(int bend) {
            this.pitchBend = bend;
            synchronized (lock) {
                int target = target();
                if (sampler == null) {
                    return;
                }
                // an mfi pitch bend is 6 bit and became the msb of the midi one doubled
                int value = (bend >> 8) & 0x3f;
                if (isRawExtB()) {
                    rawExtB(MLD.EVENT_PITCHBEND, target, param(target, value));
                } else {
                    sampler.pitchBend(target, (value - 32) / 3200f);
                }
            }
        }

        /**
         * mfi 0xe9. The fuetrek sound source caches it as the low 6 bits of the channel's pitch
         * bend, which is what vavi calls it; the shared mld flow the ma-3 one goes through reads
         * the very same event as a panpot, see {@code MLDNormalizedEventDispatcher}.
         * <p>
         * Must be called with {@link #lock} held.
         * </p>
         */
        void pitchBendFine(int value) {
            int target = target();
            if (sampler == null) {
                return;
            }
            if (isRawExtB()) {
                rawExtB(MLD.EVENT_WAVE_CHANNEL_PANPOT, target, param(target, value & 0x3f));
            } else {
                panpot(target, value);
            }
        }

        /**
         * mfi 0xe7. The fuetrek sound source takes it as a modulation seed, not as a bend
         * range, which is why the rpn 0 vavi sends along with it is not used.
         * <p>
         * Must be called with {@link #lock} held.
         * </p>
         */
        void pitchBendRange(int value) {
            int target = target();
            if (sampler == null) {
                return;
            }
            if (isRawExtB()) {
                rawExtB(MLD.EVENT_PITCHBEND_RANGE, target, param(target, value & 0x3f));
            } else {
                sampler.pitchBendRange(target, value & 0x3f);
            }
        }

        /**
         * mfi 0xba, the channel configuration, sent to the midi channel the converter moved
         * the mfi channel to. Its channel bits are the mfi channel's, which it is for; the
         * lower 3 bits are the fuetrek family mode, bit 0 drums.
         * <p>
         * Must be called with {@link #lock} held.
         * </p>
         * @param value the mfi byte
         * @see "MLD#eventDrumEnable"
         */
        void channelConfiguration(int value) {
            if (sampler == null) {
                return;
            }
            int target = (value >> 3) & 0x0f;
            if (isRawExtB()) {
                // the sound source finds the channel in the byte, what the player hands along
                // as the channel is its top 2 bits as ever, for the track of the event 0
                rawExtB(MLD.EVENT_X_DRUM_ENABLE, (value >> 6) & 3, value);
            } else {
                sampler.drumEnable(target, (value & 1) != 0);
            }
        }

        /**
         * mfi 0xe6, a level relative to the channel volume, 0x20 is none. Only the fuetrek
         * sound source takes it, the shared mld flow the ma-3 one goes through lets it go.
         * <p>
         * Must be called with {@link #lock} held.
         * </p>
         */
        void expression(int value) {
            int target = target();
            if (sampler != null && isRawExtB()) {
                rawExtB(EVENT_LEVEL_DELTA, target, param(target, value));
            }
        }

        @Override
        public int getPitchBend() {
            return pitchBend;
        }

        @Override
        public void resetAllControllers() {
            Arrays.fill(control, 0);
            control[7] = 127;
            synchronized (lock) {
                if (sampler != null) {
                    volume(channel, 0x3f);
                    panpot(channel, 0x20);
                    setPitchBend(8192);
                }
            }
        }

        @Override
        public void allNotesOff() {
            soundOff(channel);
        }

        @Override
        public void allSoundOff() {
            soundOff(channel);
        }

        /**
         * The keys of the sampler channel, wherever they came from.
         * <p>
         * A sampler has no way of cutting a channel short, only of releasing its keys.
         * </p>
         */
        void soundOff(int target) {
            synchronized (lock) {
                int[] keysOn = OpenDojaSynthesizer.this.keysOn[target];
                for (int i = 0; i < keysOn.length; i++) {
                    if (keysOn[i] > 0) {
                        keysOn[i] = 0;
                        if (sampler != null) {
                            sampler.keyOff(target, i - 69);
                        }
                    }
                }
                voiceStatuses.removeIf(vs -> vs.channel == target);
            }
        }

        @Override
        public boolean localControl(boolean on) {
            return false;
        }

        @Override
        public void setMono(boolean on) {
        }

        @Override
        public boolean getMono() {
            return false;
        }

        @Override
        public void setOmni(boolean on) {
        }

        @Override
        public boolean getOmni() {
            return false;
        }

        @Override
        public void setMute(boolean mute) {
            this.mute = mute;
            if (mute) {
                allSoundOff();
            }
        }

        @Override
        public boolean getMute() {
            return mute;
        }

        @Override
        public void setSolo(boolean soloState) {
            this.solo = soloState;
        }

        @Override
        public boolean getSolo() {
            return solo;
        }
    }

    // ----

    /**
     * The mfi machine dependent exclusive a vavi packed sysex carries.
     * <pre>
     *  0  1  2 3  4  5  6  7
     * 45 01 Δ ff ff ll ll &lt;vendor|carrier&gt; ...
     * </pre>
     *
     * @param data an unpacked vavi exclusive
     * @return the data from the vendor byte on, which is an mld ext-info event's, or
     *         {@code null} if the exclusive is not one
     * @see vavi.sound.mfi.vavi.track.MachineDependentMessage
     */
    private static byte[] machineDependent(byte[] data) {
        int vendor = 7;
        if (data.length <= vendor ||
            data[0] != (byte) MANUFACTURER_ID ||
            data[1] != MFi_SYSEX_FUNCTION_ID_MACHINE_DEPENDENT ||
            data[3] != (byte) 0xff || data[4] != (byte) 0xff) {
            return null;
        }
        int length = ((data[5] & 0xff) << 8) | (data[6] & 0xff);
        return Arrays.copyOfRange(data, vendor, Math.min(vendor + length, data.length));
    }

    private class OpenDojaReceiver implements MidiDeviceReceiver {
        private boolean receiverOpen = true;

        /** the next universal master volume is the song's, already taken */
        private boolean songVolume;

        public OpenDojaReceiver() {
            receivers.add(this);
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!receiverOpen) throw new IllegalStateException("receiver is not open");
            timestamp = timeStamp;

            switch (message) {
                case ShortMessage shortMessage -> {
                    int channel = shortMessage.getChannel();
                    int command = shortMessage.getCommand();
                    int data1 = shortMessage.getData1();
                    int data2 = shortMessage.getData2();
                    switch (command) {
                        case ShortMessage.NOTE_OFF:
                            channels[channel].noteOff(data1, data2);
                            break;
                        case ShortMessage.NOTE_ON:
                            channels[channel].noteOn(data1, data2);
                            break;
                        case ShortMessage.POLY_PRESSURE:
                            channels[channel].setPolyPressure(data1, data2);
                            break;
                        case ShortMessage.CONTROL_CHANGE:
                            channels[channel].controlChange(data1, data2);
                            break;
                        case ShortMessage.PROGRAM_CHANGE:
                            channels[channel].programChange(data1);
                            break;
                        case ShortMessage.CHANNEL_PRESSURE:
                            channels[channel].setChannelPressure(data1);
                            break;
                        case ShortMessage.PITCH_BEND:
                            channels[channel].setPitchBend(data1 | (data2 << 7));
                            break;
                        default:
                            logger.log(Level.DEBUG, "unhandled short: %02X".formatted(command));
                    }
                }
                case SysexMessage sysexMessage -> sysex(sysexMessage);
                case MetaMessage metaMessage -> {
                    logger.log(Level.DEBUG, "meta: %02x".formatted(metaMessage.getType()));
                }
                default -> {
                    assert false;
                }
            }
        }

        /**
         * @see MfiValueExclusive
         * @see MobileExclusive
         */
        private void sysex(SysexMessage sysexMessage) {
            byte[] whole = sysexMessage.getMessage(); // f0 ... f7
            byte[] data = sysexMessage.getData();     // ... f7
logger.log(Level.TRACE, "sysex: %02X\n%s".formatted(sysexMessage.getStatus(), StringUtil.getDump(data, 32)));
            if (data.length == 0) {
                return;
            }

            // the mfi values midi has no room for: f0 45 04 sub ... f7
            int sub = MfiValueExclusive.sub(whole);
            if (sub != -1) {
                synchronized (lock) {
                    switch (sub) {
                        case MfiValueExclusive.BANK -> {
                            if (whole.length >= 7) channels[whole[4] & 0x0f].bank(whole[5] & 0x3f);
                        }
                        case MfiValueExclusive.MASTER_VOLUME -> {
                            // the universal master volume following is the same one
                            if (whole.length >= 6) masterVolume(whole[4] & 0x7f);
                            songVolume = true;
                        }
                        case MfiValueExclusive.PITCH_BEND_FINE -> {
                            if (whole.length >= 7) channels[whole[4] & 0x0f].pitchBendFine(whole[5] & 0x3f);
                        }
                        case MfiValueExclusive.PITCH_BEND_RANGE -> {
                            if (whole.length >= 7) channels[whole[4] & 0x0f].pitchBendRange(whole[5] & 0x3f);
                        }
                        case MfiValueExclusive.CHANNEL_CONFIGURATION -> {
                            if (whole.length >= 8) channels[whole[4] & 0x0f].channelConfiguration((whole[5] & 0x7f) | ((whole[6] & 0x01) << 7));
                        }
                        case MfiValueExclusive.CHANNEL -> {
                            if (whole.length >= 7) channels[whole[4] & 0x0f].origin(whole[5] & 0x0f);
                        }
                        case MfiValueExclusive.PROGRAM -> {
                            if (whole.length >= 7) channels[whole[4] & 0x0f].mfiProgram(whole[5] & 0x3f);
                        }
                        case MfiValueExclusive.EXPRESSION -> {
                            if (whole.length >= 7) channels[whole[4] & 0x0f].expression(whole[5] & 0x3f);
                        }
                        default -> logger.log(Level.DEBUG, "unhandled fuetrek sub: %02x".formatted(sub));
                    }
                }
                return;
            }

            // universal device control: f0 7f 7f 04 nn ll mm f7
            if (data.length >= 6 && (data[0] & 0xff) == 0x7f && data[2] == 0x04) {
                int value = (data[4] & 0x7f) | ((data[5] & 0x7f) << 7);
                if (data[3] == 0x01) { // master volume
                    if (songVolume) {
                        // the song's, the exclusive before it has been taken already
                        songVolume = false;
                    } else {
                        float gain = value / 16383f;
logger.log(Level.DEBUG, "sysex volume: gain: %4.2f".formatted(gain));
                        masterGain = gain;
                    }
                } else {
                    logger.log(Level.DEBUG, "unhandled device control: %02x".formatted(data[3]));
                }
                return;
            }

            // gm system on: f0 7e 7f 09 01 f7
            if (data.length >= 4 && (data[0] & 0xff) == 0x7e && data[2] == 0x09 && data[3] == 0x01) {
                synchronized (lock) {
                    if (sampler != null) {
                        sampler.reset();
                        for (OpenDojaMidiChannel channel : channels) {
                            channel.reset();
                        }
                    }
                }
                return;
            }

            // the voices and the waves of the file itself, packed 8 bit into 7
            if (data.length >= 3 && data[0] == (byte) MANUFACTURER_ID &&
                (data[1] & 0xff) == MobileExclusive.MIDI_SYSEX_FUNCTION_ID_PACKED) {
                byte[] exclusive = MobileExclusive.unpack(data);
                byte[] extInfo = machineDependent(exclusive);
                if (extInfo == null) {
                    // a smaf file's yamaha exclusive ends up here, which no openDoja sampler takes
                    logger.log(Level.DEBUG, "not an mfi machine dependent exclusive:\n" + StringUtil.getDump(exclusive, 32));
                    return;
                }
logger.log(Level.DEBUG, "ext-info: %d bytes, vendor: %02x, sub: %02x".formatted(extInfo.length, extInfo[0], extInfo.length > 1 ? extInfo[1] : 0));
                synchronized (lock) {
                    if (sampler != null) {
                        sampler.sysEx(extInfo);
                    }
                }
                return;
            }

            logger.log(Level.DEBUG, "unhandled manufacturer: %02x".formatted(data[0]));
        }

        /**
         * Must be called with {@link #lock} held.
         *
         * @param volume mfi master volume, 0 ~ 127
         */
        private void masterVolume(int volume) {
            if (sampler == null) {
                return;
            }
            if (sampler instanceof MLDRawExtBHandler) {
                // the whole byte is the volume, so what the player reads as the channel of the
                // event is its top bit; it is not used for anything but having to be a channel
                rawExtB(MLD.EVENT_MASTER_VOLUME, (volume >> 6) & 0x0f, volume & 0x7f);
            } else {
                sampler.masterVolume(volumeToAmplitude(volume / 127f));
            }
        }

        @Override
        public void close() {
            receivers.remove(this);
            receiverOpen = false;
        }

        @Override
        public MidiDevice getMidiDevice() {
            return OpenDojaSynthesizer.this;
        }
    }
}
