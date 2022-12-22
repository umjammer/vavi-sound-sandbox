/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import vavi.util.Debug;


/**
 * MMLを楽譜に変換するクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.4 $
 */
public class MMLCompiler {

    private MusicScore score;
    private int tickPerBeat = 240;
    private int maxAmplitude = 127;
    private int currentTempo = 60;
    private int initialOctave = 4;
    private int[] currentOctave;
    private int currentVolume = 8;
    private int currentTick = tickPerBeat;
    private int initialQuantity = 8;
    private int[] currentQuantity;
    private int tick = 0;
    private String mml;
    private int position;

    public MMLCompiler(int newTickPerBeat, int channelCount) {
        tickPerBeat = newTickPerBeat;
        currentOctave = new int[channelCount];
        currentQuantity = new int[channelCount];
        for (int ch = 0; ch < channelCount; ch++) {
            currentOctave[ch] = initialOctave;
            currentQuantity[ch] = initialQuantity;
        }
    }

    private int getNumber(StringBuilder buf) {
        int number = 0;
        char c;

        while (buf.length() > 0 && Character.isDigit(buf.charAt(0))) {
            number *= 10;
            number += Character.digit(buf.charAt(0), 10);
            buf.deleteCharAt(0);
        }
        return number;
    }

    private int getNoteNumber(int octave, char noteName) throws MMLException {
        int number = octave * 12 + 12;

        // noteName must be a lower-case character
        switch (noteName) {
        case 'c':
            number += 0;
            break;
        case 'd':
            number += 2;
            break;
        case 'e':
            number += 4;
            break;
        case 'f':
            number += 5;
            break;
        case 'g':
            number += 7;
            break;
        case 'a':
            number += 9;
            break;
        case 'b':
            number += 11;
            break;
        default:
            throw new MMLException();
        }
        return number;
    }

    private int getTick(StringBuilder buf) {
        int tick;
        char c;

        tick = currentTick;
        getLength:
        {
            if (buf.length() == 0) {
                break getLength;
            }
            c = buf.charAt(0);
            if (Character.isDigit(c)) {
                tick = tickPerBeat * 4 / getNumber(buf);
                if (buf.length() == 0) {
                    break getLength;
                }
                c = buf.charAt(0);
            }
            if (c == '.') {
                tick = tick * 3 / 2;
                buf.deleteCharAt(0);
            }
        }
        return tick;
    }

    private void processNote(StringBuilder buf, int channel, char noteName) throws MMLException {
        int number = getNoteNumber(currentOctave[channel], noteName);

        if (buf.length() > 0) {
            char c = buf.charAt(0);
            if (c == '+' || c == '-') {
                if (c == '+') {
                    number++;
                } else {
                    number--;
                }
                buf.deleteCharAt(0);
            }
        }
        if (number < 0 || number > 127) {
            throw new MMLException("invalid note number: " + number);
        }
        score.add(new NoteOn(tick, channel, number, currentVolume * 8));
        int noteTick = getTick(buf);
        score.add(new NoteOff(tick + noteTick * currentQuantity[channel] / 8 - 1, channel, number, currentVolume * 8));
        tick += noteTick;
    }

    private void processChangeInstrument(StringBuilder buf, int channel) throws MMLException {
        Instrument inst;
        char c = buf.charAt(0);

        buf.deleteCharAt(0);
        switch (c) {
        case 'a':
            inst = new SquareWaveInstrument();
            break;
        case 'b':
            inst = new SineWaveInstrument();
            break;
        case 'c':
            inst = new FMGeneralInstrument(getNumber(buf));
            break;
        default:
            throw new MMLException("invalid program name");
        }
        score.add(new ChangeInstrument(tick, channel, inst));
    }

    public void compile(MusicScore newScore, String[] mml) throws MMLException {
        for (int ch = 0; ch < mml.length; ch++) {
            if (mml[ch] != null && mml[ch].length() > 0) {
                compile(newScore, ch, mml[ch]);
            }
        }
    }

    public void compile(MusicScore newScore, int channel, String mml) throws MMLException {
        StringBuilder buf = new StringBuilder(mml);
Debug.println("mml: " + mml);
        char c;

        score = newScore;
        tick = 0;
        while (buf.length() > 0) {
            c = buf.charAt(0);
            buf.deleteCharAt(0);

            c = Character.toLowerCase(c);
            switch (c) {
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'a':
            case 'b':
                // Note
                processNote(buf, channel, c);
                break;
            case 'r':
                // Rest
                tick += getTick(buf);
                break;
            case 't':
                // Tempo
                score.add(new ChangeTempo(tick, channel, getNumber(buf)));
                break;
            case 'v':
                // Volume
                currentVolume = getNumber(buf);
                if (currentVolume > 7) {
                    throw new MMLException("voume must be in range of 0 to 7");
                }
                break;
            case 'l':
                // Default Length
                currentTick = getTick(buf);
                break;
            case 'o':
                // Octave
                currentOctave[channel] = getNumber(buf);
                if (currentOctave[channel] > 7) {
                    throw new MMLException("octave must be in range of 0 to 7");
                }
                break;
            case 'q':
                // Default Quantity
                currentQuantity[channel] = getNumber(buf);
                if (currentQuantity[channel] == 0 || currentQuantity[channel] > 8) {
                    throw new MMLException("octave must be in range of 1 to 8");
                }
                break;
            case '>':
                // Octave Up
                System.out.println(">");
                if (currentOctave[channel] == 0) {
                    throw new MMLException("octave must be >= 0");
                }
                currentOctave[channel]--;
                break;
            case '<':
                // Octave Down
                if (currentOctave[channel] == 7) {
                    throw new MMLException("octave must be <= 7");
                }
                currentOctave[channel]++;
                break;
            case '@':
                // Change Instrument
                processChangeInstrument(buf, channel);
                break;
            default:
                System.err.println("invalid MML command: " + c);
                break;
            }
        }
    }
}
