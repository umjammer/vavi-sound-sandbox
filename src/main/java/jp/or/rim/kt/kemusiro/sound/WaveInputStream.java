/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;


/**
 * Generates binary data from musical scores.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class WaveInputStream extends InputStream {

    private final MusicScore score;
    private final int samplingRate;
    private final int samplingDepth;
    private final Instrument[] insts;
    private final NoteOn[] notes;
    private int currentTick = 0;
    private static final double currentTime = 0.0;
    private final double[] time;
    private int currentTempo = 60;
    private int pos = 0;
    private final LinkedList<MusicEvent> events;
    private final ByteArrayOutputStream output;
    private byte[] buffer;

    public WaveInputStream(MusicScore score, int rate, int bits) {
        this.score = score;
        samplingRate = rate;
        if (bits % 8 != 0 || bits == 0 || bits > 32) {
            throw new RuntimeException("unsupported bit depth: " + bits);
        }
        samplingDepth = bits;

        events = score.getEventList();
        insts = new Instrument[score.getChannelCount()];
        notes = new NoteOn[score.getChannelCount()];
        time = new double[score.getChannelCount()];

        output = new ByteArrayOutputStream(4 * samplingRate / score.getTickPerBeat());
    }

    private double getTimePerBeat() {
        return 60.0 / currentTempo;
    }

    private double getTimePerTick() {
        return getTimePerBeat() / score.getTickPerBeat();
    }

    private int getSampleCountPerTick() {
        return (int) (getTimePerTick() * (double) samplingRate);
    }

    private void processEvent() {
        MusicEvent e = events.getFirst();

        while (e != null && e.getTick() == currentTick) {
            int ch = e.getChannel();

            switch (e) {
                case ChangeInstrument changeInstrument -> {
                    insts[ch] = changeInstrument.getInstrument();
                    insts[ch].setTimeStep(1.0 / (double) samplingRate);
                }
                case ChangeTempo changeTempo -> currentTempo = changeTempo.getTempo();
                case NoteOn noteOn -> {
                    notes[ch] = noteOn;
                    insts[ch].press();
                }
                case NoteOff noteOff ->
                    // notes[ch] = null;
                        insts[ch].release();
                default -> {
                }
            }
            events.removeFirst();
            if (events.isEmpty()) {
                break;
            }
            e = events.getFirst();
        }
    }

    private void writeDouble(double value) {
        int intValue = (int) value;

        if (samplingDepth == 32) {
            output.write((intValue & 0xff000000) >> 24);
        }
        if (samplingDepth >= 24) {
            output.write((intValue & 0x00ff0000) >> 16);
        }
        if (samplingDepth >= 16) {
            output.write((intValue & 0x0000ff00) >> 8);
        }
        output.write(intValue & 0x000000ff);
    }

    private void processNote() {
        for (int p = 0; p < getSampleCountPerTick(); p++) {
            double value = 0.0;
            for (int ch = 0; ch < score.getChannelCount(); ch++) {
                if (notes[ch] != null) {
                    value += notes[ch].getVelocity() * insts[ch].getValue(notes[ch].getNumber());
                }
            }
            writeDouble(value / score.getChannelCount());
        }
    }

    private int fillBuffer() {
        output.reset();
        for (int t = 0; t < score.getTickPerBeat(); t++) {
            if (events.isEmpty()) {
                break;
            }
            if (events.getFirst().getTick() == currentTick) {
                processEvent();
            }

            processNote();
            currentTick++;
        }
        buffer = output.toByteArray();
        return buffer.length;
    }

    @Override
    public int read() throws IOException {
        if (buffer == null || pos == buffer.length) {
            if (fillBuffer() <= 0) {
                buffer = null;
                return -1;
            }
            pos = 0;
        }
        return buffer[pos++] & 0xff;
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
