/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;

import org.rococoa.ID;
import vavi.util.Debug;
import vavix.rococoa.avfoundation.AVAudioFormat;
import vavix.rococoa.avfoundation.AVAudioPlayer;


/**
 * RococoaClip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/21 umjammer initial version <br>
 */
public class RococoaClip implements Clip {

    public static javax.sound.sampled.DataLine.Info info =
            new javax.sound.sampled.DataLine.Info(RococoaClip.class,
                    new AudioFormat(RcococaEncoding.ROCOCOA,
                            AudioSystem.NOT_SPECIFIED,
                            AudioSystem.NOT_SPECIFIED,
                            AudioSystem.NOT_SPECIFIED,
                            AudioSystem.NOT_SPECIFIED,
                            AudioSystem.NOT_SPECIFIED,
                            ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN));

    private final List<LineListener> listeners = new ArrayList<>();

    protected void fireUpdate(LineEvent event) {
        listeners.forEach(l -> l.update(event));
    }

    private final Control[] controls;

    private AVAudioPlayer player;

    private int bufferSize;

    private AudioInputStream stream;

    public RococoaClip() {
        controls = new Control[] {gainControl, panControl};
    }

//#region Control

    protected final class Gain extends FloatControl {

        static float linearToDB(float linear) {

            return (float) (Math.log(((linear == 0.0) ? 0.0001 : linear)) / Math.log(10.0) * 20.0);
        }

        static float dBToLinear(float dB) {

            return (float) Math.pow(10.0, dB / 20.0);
        }

        private float linearGain = 1.0f;

        private Gain() {

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
            // adjust value within range ?? spec says IllegalArgumentException
            //newValue = Math.min(newValue, getMaximum());
            //newValue = Math.max(newValue, getMinimum());

            float newLinearGain = dBToLinear(newValue);
            super.setValue(linearToDB(newLinearGain));
            // if no exception, commit to our new gain
            linearGain = newLinearGain;
            calcVolume();
Debug.println("volume: " + leftGain);
            player.setVolume(leftGain);
        }

        float getLinearGain() {
            return linearGain;
        }
    }

    private final class Mute extends BooleanControl {

        private Mute() {
            super(BooleanControl.Type.MUTE, false, "True", "False");
        }

        @Override
        public void setValue(boolean newValue) {
            super.setValue(newValue);
            calcVolume();
            player.setVolume(leftGain);
        }
    }

    private final class Balance extends FloatControl {

        private Balance() {
            super(FloatControl.Type.BALANCE, -1.0f, 1.0f, (1.0f / 128.0f), -1, 0.0f,
                    "", "Left", "Center", "Right");
        }

        @Override
        public void setValue(float newValue) {
            setValueImpl(newValue);
            panControl.setValueImpl(newValue);
            calcVolume();
            player.setVolume(leftGain);
        }

        void setValueImpl(float newValue) {
            super.setValue(newValue);
        }

    }

    private final class Pan extends FloatControl {

        private Pan() {
            super(FloatControl.Type.PAN, -1.0f, 1.0f, (1.0f / 128.0f), -1, 0.0f,
                    "", "Left", "Center", "Right");
        }

        @Override
        public void setValue(float newValue) {
            setValueImpl(newValue);
            balanceControl.setValueImpl(newValue);
            calcVolume();
            player.setPan(newValue);
        }
        void setValueImpl(float newValue) {
            super.setValue(newValue);
        }
    }

    private void calcVolume() {
        if (getFormat() == null) {
            return;
        }
        if (muteControl.getValue()) {
            leftGain = 0.0f;
            rightGain = 0.0f;
            return;
        }
        float gain = gainControl.getLinearGain();
        if (getFormat().getChannels() == 1) {
            // trivial case: only use gain
            leftGain = gain;
            rightGain = gain;
        } else {
            // need to combine gain and balance
            float bal = balanceControl.getValue();
            if (bal < 0.0f) {
                // left
                leftGain = gain;
                rightGain = gain * (bal + 1.0f);
            } else {
                leftGain = gain * (1.0f - bal);
                rightGain = gain;
            }
        }
    }

    private final Gain gainControl = new Gain();
    private final Mute muteControl = new Mute();
    private final Balance balanceControl = new Balance();
    private final Pan panControl = new Pan();
    private float leftGain, rightGain;

//#endregion Control

    @Override
    public void drain() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void start() {
        boolean r = player.play();
        fireUpdate(new LineEvent(this, LineEvent.Type.START, 0));
Debug.println("play: " + r);
    }

    @Override
    public void stop() {
        player.stop();
    }

    @Override
    public boolean isRunning() {
        return player.isPlaying();
    }

    @Override
    public boolean isActive() {
        return player != null;
    }

    @Override
    public AudioFormat getFormat() {
        AVAudioFormat format = player.format();
Debug.println(format + ", " + format.commonFormat());
        return switch (format.commonFormat()) {
            default -> stream.getFormat();
            case AVAudioFormat.PCMFormatFloat32 -> new AudioFormat(AudioFormat.Encoding.PCM_FLOAT,
                    (int) format.sampleRate(),
                    32,
                    format.channelCount(),
                    AudioSystem.NOT_SPECIFIED,
                    AudioSystem.NOT_SPECIFIED,
                    ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
            case AVAudioFormat.PCMFormatFloat64 -> new AudioFormat(AudioFormat.Encoding.PCM_FLOAT,
                    (int) format.sampleRate(),
                    64,
                    format.channelCount(),
                    AudioSystem.NOT_SPECIFIED,
                    AudioSystem.NOT_SPECIFIED,
                    ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
            case AVAudioFormat.PCMFormatInt16 -> new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    (int) format.sampleRate(),
                    16,
                    format.channelCount(),
                    AudioSystem.NOT_SPECIFIED,
                    AudioSystem.NOT_SPECIFIED,
                    ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
            case AVAudioFormat.PCMFormatInt32 -> new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    (int) format.sampleRate(),
                    32,
                    format.channelCount(),
                    AudioSystem.NOT_SPECIFIED,
                    AudioSystem.NOT_SPECIFIED,
                    ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
        };
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public int available() {
        try {
            return stream.available();
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public javax.sound.sampled.Line.Info getLineInfo() {
        return info;
    }

    @Override
    public void open() throws LineUnavailableException {
Debug.println(Level.WARNING, "use #open(AudioInputStream)");
    }

    @Override
    public void close() {
        if (isRunning()) {
            stop();
        }
        player = null;
        fireUpdate(new LineEvent(this, LineEvent.Type.CLOSE, 0));
    }

    @Override
    public boolean isOpen() {
        return player != null;
    }

    @Override
    public Control[] getControls() {
        return new Control[0];
    }

    @Override
    public boolean isControlSupported(Type controlType) {
        // protect against a NullPointerException
        if (controlType == null) {
            return false;
        }

        for (Control control : controls) {
            if (controlType == control.getType()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Control getControl(Type controlType) {
        // protect against a NullPointerException
        if (controlType != null) {

            for (Control control : controls) {
                if (controlType == control.getType()) {
                    return control;
                }
            }
        }

        throw new IllegalArgumentException("Unsupported control type: " + controlType);
    }

    @Override
    public void addLineListener(LineListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeLineListener(LineListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void open(AudioFormat format, byte[] data, int offset, int bufferSize) throws LineUnavailableException {
        try {
            this.bufferSize = bufferSize;
            open(new AudioInputStream(new ByteArrayInputStream(data, offset, data.length - offset), format, data.length - offset));
        } catch (LineUnavailableException | IOException e) {
            throw (LineUnavailableException) new LineUnavailableException().initCause(e);
        }
    }

    @Override
    public void open(AudioInputStream stream) throws LineUnavailableException, IOException {
        this.stream = stream;

        Path file = Files.createTempFile("vavi.sound.sampled.rococoa", ".clip");
        // TODO StandardOpenOption.DELETE_ON_CLOSE
        Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);

        player = AVAudioPlayer.init(file.toUri());
        // TODO doesn't work
        player.setDelegate(new AVAudioPlayer.AVAudioPlayerDelegate() {
            @Override public void audioPlayerDidFinishPlaying_successfully(ID player, boolean flag) {
                fireUpdate(new LineEvent(RococoaClip.this, LineEvent.Type.STOP, 0));
            }

            @Override public void audioPlayerDecodeErrorDidOccur_error(ID player, ID error) {

            }

            @Override public void audioPlayerBeginInterruption(ID player) {

            }

            @Override public void audioPlayerEndInterruption(ID player) {

            }

            @Override public void audioPlayerEndInterruption_withOptions(ID player, long flags) {

            }

            @Override public void audioPlayerEndInterruption_withFlags(ID player, long flags) {

            }
        });
        fireUpdate(new LineEvent(this, LineEvent.Type.OPEN, 0));
Debug.println("player: " + player);
    }

    @Override
    public int getFrameLength() {
        return (int) stream.getFrameLength();
    }

    @Override
    public long getMicrosecondLength() {
        return (long) Math.ceil((float) player.duration() * 100);
    }

    @Override
    public int getFramePosition() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLongFramePosition() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setFramePosition(int frames) {
        // TODO Auto-generated method stub
    }

    @Override
    public long getMicrosecondPosition() {
        return (long) Math.ceil((float) player.currentTime() * 100);
    }

    @Override
    public void setMicrosecondPosition(long microseconds) {
        player.setCurrentTime(microseconds / 100d);
    }

    @Override
    public void setLoopPoints(int start, int end) {
        // TODO Auto-generated method stub

    }

    @Override
    public void loop(int count) {
        player.setNumberOfLoops(count);
    }

    @Override
    public float getLevel() {
        return player.volume();
    }
}
