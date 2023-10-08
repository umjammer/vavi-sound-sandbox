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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;

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

    private AVAudioPlayer player;

    private int bufferSize;

    private AudioInputStream stream;

    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void drain() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void start() {
        boolean r = player.play();
        if (r) {
            service.scheduleAtFixedRate(this::check, 100, 100, TimeUnit.MILLISECONDS);
            fireUpdate(new LineEvent(this, LineEvent.Type.START, 0));
        }
Debug.println("play: " + r);
    }

    // TODO use AVFoudation's delegate
    private void check() {
        if (!player.isPlaying()) {
Debug.println("stop detected");
            stopInternal();
        }
    }

    private void stopInternal() {
        fireUpdate(new LineEvent(this, LineEvent.Type.STOP, 0));
        service.shutdown();
    }

    @Override
    public void stop() {
        player.stop();
        stopInternal();
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
    public boolean isControlSupported(Type control) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Control getControl(Type control) {
        // TODO Auto-generated method stub
        return null;
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
//        player.setDelegate(new AVAudioPlayer.Delegate() {
//            @Override
//            public void audioPlayerDidFinishPlaying_successfully(ID player, boolean flag) {
//                stop();
//            }
//        });
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

/* */
