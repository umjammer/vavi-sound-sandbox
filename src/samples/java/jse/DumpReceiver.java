/*
 * DumpReceiver.java
 *
 * This file is part of the Java Sound Examples.
 */

package jse;

/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 675 Mass
 * Ave, Cambridge, MA 02139, USA.
 *
 */
import java.io.PrintStream;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;


/**
 * Receiver that outputs MIDI events as text.
 */
public class DumpReceiver implements Receiver {
    private static final boolean DEBUG = false;

    private static boolean sm_bPrintRawData = true;

    private static final String[] sm_astrKeyNames = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private static final String[] sm_astrKeySignatures = {
        "Cb", "Gb", "Db", "Ab", "Eb", "Bb", "F", "C", "G", "D", "A", "E", "B", "F#", "C#"
    };

    private static final String[] SYSTEM_MESSAGE_TEXT = {
        "System Exclusive (should not be in ShortMessage!)", "MTC Quarter Frame: ", "Song Position: ", "Song Select: ", "Undefined", "Undefined", "Tune Request", "End of SysEx (should not be in ShortMessage!)", "Timing clock", "Undefined", "Start", "Continue", "Stop", "Undefined", "Active Sensing", "System Reset"
    };

    private static final String[] QUARTER_FRAME_MESSAGE_TEXT = {
        "frame count LS: ", "frame count MS: ", "seconds count LS: ", "seconds count MS: ", "minutes count LS: ", "minutes count MS: ", "hours count LS: ", "hours count MS: "
    };

    private static final String[] FRAME_TYPE_TEXT = {
        "24 frames/second", "25 frames/second", "30 frames/second (drop)", "30 frames/second (non-drop)",
    };

    private static final String[] MANUFACTURER_ID_ONE_BYTE = {
        null, // 0x00, escape to 3-byte manufacturer code
        "Sequential Circuits", // 0x01
        "Big Briar", // 0x02
        "Octave / Plateau", // 0x03
        "Moog", // 0x04
        "Passport Designs", // 0x05
        "Lexicon", // 0x06
        "Kurzweil", // 0x07
        "Fender", // 0x08
        "Gulbransen", // 0x09
        "Delta Labs", // 0x0A
        "Sound Comp.", // 0x0B
        "General Electro", // 0x0C
        "Techmar", // 0x0D
        "Matthews Research", // 0x0E
        null, // 0x0F
        "Oberheim", // 0x10
        "PAIA", // 0x11
        "Simmons", // 0x12
        "DigiDesign", // 0x13
        "Fairlight", // 0x14
        "JL Cooper", // 0x15
        "Lowery", // 0x16
        "Lin", // 0x17
        "Emu", // 0x18
        null, // 0x19
        null, // 0x1A
        "Peavey", // 0x1B
        null, // 0x1C
        null, // 0x1D
        null, // 0x1E
        null, // 0x1F
        "Bon Tempi", // 0x20
        "S.I.E.L.", // 0x21
        null, // 0x22
        "SyntheAxe", // 0x23
        "Hohner", // 0x24
        "Crumar", // 0x25
        "Solton", // 0x26
        "Jellinghaus Ms", // 0x27
        "CTS", // 0x28
        "PPG", // 0x29
        null, // 0x2A
        null, // 0x2B
        null, // 0x2C
        null, // 0x2D
        null, // 0x2E
        "Elka", // 0x2F
        null, // 0x30
        null, // 0x31
        null, // 0x32
        null, // 0x33
        null, // 0x34
        null, // 0x35
        "Cheetah", // 0x36
        null, // 0x37
        null, // 0x38
        null, // 0x39
        null, // 0x3A
        null, // 0x3B
        null, // 0x3C
        null, // 0x3D
        "Waldorf", // 0x3E
        null, // 0x3F
        "Kawai", // 0x40
        "Roland", // 0x41
        "Korg", // 0x42
        "Yamaha", // 0x43
        "Casio", // 0x44
        "Akai", // 0x45
        "Kamiya", // 0x46
        "Akai", // 0x47
        "Victor", // 0x48
        null, // 0x49
        null, // 0x4A
        "Fujitsu", // 0x4B
        "Sony", // 0x4C
        null, // 0x4D
        "Teac", // 0x4E
        null, // 0x4F
        "Matsushita", // 0x50
        "Fostex", // 0x51
        "Zoom", // 0x52
        null, // 0x53
        "Matsushita", // 0x54
        "Suzuki", // 0x55
        "Fuji", // 0x56
        "Acoustic", // 0x57
        null, // 0x58
        null, // 0x59
        null, // 0x5A
        null, // 0x5B
        null, // 0x5C
        null, // 0x5D
        null, // 0x5E
        null, // 0x5F
        null, // 0x60
        null, // 0x61
        null, // 0x62
        null, // 0x63
        null, // 0x64
        null, // 0x65
        null, // 0x66
        null, // 0x67
        null, // 0x68
        null, // 0x69
        null, // 0x6A
        null, // 0x6B
        null, // 0x6C
        null, // 0x6D
        null, // 0x6E
        null, // 0x6F
        null, // 0x70
        null, // 0x71
        null, // 0x72
        null, // 0x73
        null, // 0x74
        null, // 0x75
        null, // 0x76
        null, // 0x77
        null, // 0x78
        null, // 0x79
        null, // 0x7A
        null, // 0x7B
        null, // 0x7C
        "Non-commercial", // 0x7D
        null, // 0x7E, non real-time universal
        null, // 0x7F, real-time universal
    };

    private PrintStream m_printStream;

    private boolean m_bDebug;

    private boolean m_bPrintTimeStampAsTicks;

    public DumpReceiver(PrintStream printStream) {
        this(printStream, false);
    }

    public DumpReceiver(PrintStream printStream, boolean bPrintTimeStampAsTicks) {
        m_printStream = printStream;
        m_bDebug = false;
        m_bPrintTimeStampAsTicks = bPrintTimeStampAsTicks;
    }

    public void close() {
        // DO NOTHING
    }

    public void send(MidiMessage message, long lTimeStamp) {
        if (DEBUG) {
            m_printStream.println("DumpReceiver.send(): called");
            m_printStream.println("Class of Message: " + message.getClass().getName());
        }
        if (sm_bPrintRawData) {
            m_printStream.print("[");

            byte[] abData = message.getMessage();
            int nLength = message.getLength();
            m_printStream.print(getHexString(abData, 0, nLength));
            m_printStream.println("] ");
        }

        String strMessage = null;
        if (message instanceof ShortMessage) {
            strMessage = decodeMessage((ShortMessage) message);
        } else if (message instanceof SysexMessage) {
            strMessage = decodeMessage((SysexMessage) message);
        } else if (message instanceof MetaMessage) {
            strMessage = decodeMessage((MetaMessage) message);
        } else {
            strMessage = "unknown message type";
        }

        String strTimeStamp = null;
        if (m_bPrintTimeStampAsTicks) {
            strTimeStamp = "tick " + lTimeStamp + ": ";
        } else {
            if (lTimeStamp == -1L) {
                strTimeStamp = "timestamp [unknown]: ";
            } else {
                strTimeStamp = "timestamp " + lTimeStamp + " : ";
            }
        }
        m_printStream.println(strTimeStamp + strMessage);
    }

    private String decodeMessage(ShortMessage message) {
        String strMessage = null;
        switch (message.getCommand()) {
        case 0x80:
            strMessage = "note Off " + getKeyName(message.getData1()) + " velocity: " + message.getData2();
            break;
        case 0x90:
            strMessage = "note On " + getKeyName(message.getData1()) + " velocity: " + message.getData2();
            break;
        case 0xa0:
            strMessage = "polyphonic key pressure " + getKeyName(message.getData1()) + " pressure: " + message.getData2();
            break;
        case 0xb0:
            strMessage = "control change " + message.getData1() + " value: " + message.getData2();
            break;
        case 0xc0:
            strMessage = "program change " + message.getData1();
            break;
        case 0xd0:
            strMessage = "key pressure " + getKeyName(message.getData1()) + " pressure: " + message.getData2();
            break;
        case 0xe0:
            strMessage = "pitch wheel change " + get14bitValue(message.getData1(), message.getData2());
            break;
        case 0xF0:
            strMessage = SYSTEM_MESSAGE_TEXT[message.getChannel()];
            switch (message.getChannel()) {
            case 0x1:

                int nQType = (message.getData1() & 0x70) >> 4;
                int nQData = message.getData1() & 0x0F;
                if (nQType == 7) {
                    nQData = nQData & 0x1;
                }
                strMessage += (QUARTER_FRAME_MESSAGE_TEXT[nQType] + nQData);
                if (nQType == 7) {
                    int nFrameType = (message.getData1() & 0x06) >> 1;
                    strMessage += (", frame type: " + FRAME_TYPE_TEXT[nFrameType]);
                }
                break;
            case 0x2:
                strMessage += get14bitValue(message.getData1(), message.getData2());
                break;
            case 0x3:
                strMessage += message.getData1();
                break;
            }
            break;
        default:
            strMessage = "unknown message: status = " + message.getStatus() + ", byte1 = " + message.getData1() + ", byte2 = " + message.getData2();
            break;
        }
        if (message.getCommand() != 0xF0) {
            int nChannel = message.getChannel() + 1;
            String strChannel = "channel " + nChannel + ": ";
            strMessage = strChannel + strMessage;
        }
        return strMessage;
    }

    private String decodeMessage(SysexMessage message) {
        // without status byte, but everyting else
        byte[] abData = message.getData();
        String strMessage = null;

        // System.out.println("sysex status: " + message.getStatus());
        if (message.getStatus() == SysexMessage.SYSTEM_EXCLUSIVE) {
            strMessage = "Sysex message (F0): ";

            int nManufacturer = abData[0];
            int nStart = 1;
            if (nManufacturer == 0x7E) // non real-time universal
            {
                strMessage += "[non real-time universal] (7E) ";
                strMessage += getHexString(abData, nStart, abData.length - nStart);
            } else if (nManufacturer == 0x7F) // real-time universal
            {
                strMessage += "[real-time universal] (7F) ";
                strMessage += getHexString(abData, nStart, abData.length - nStart);
            } else if (nManufacturer == 0) // three byte manufacturer id
            {
                // strMessage += MANUFACTURER_ID_THREE_BYTE[/*nManufacturer*/] +
                // " (" + getHexString(abData, nStart - 1, 1) + ") ";
                strMessage += ("[three byte manufacturer code]" + " (" + getHexString(abData, nStart - 1, 3) + ") ");
                nStart += 2;
                strMessage += getHexString(abData, nStart, abData.length - nStart);
            } else // one byte manufacturer id
            {
                strMessage += MANUFACTURER_ID_ONE_BYTE[nManufacturer];
                strMessage += (" (" + getHexString(abData, nStart - 1, 1) + ") ");
                if (nManufacturer == 0x43) {
                    strMessage += decodeYamahaSysexData(abData, nStart);
                } else {
                    strMessage += decodeSysexData(abData, nStart);

                    // strMessage += getHexString(abData, nStart, abData.length
                    // - nStart);
                }
            }
        } else if (message.getStatus() == SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE) {
            strMessage = "Continued Sysex message (F7) " + getHexString(abData);
        }
        return strMessage;
    }

    private String decodeSysexData(byte[] abData, int nStart) {
        String strData = null;
        if (abData[abData.length - 1] == SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE) {
            strData = getHexString(abData, nStart, abData.length - nStart - 1) + "EOX (F7)";
        } else {
            strData = getHexString(abData, nStart, abData.length - nStart);
        }
        return strData;
    }

    private String decodeYamahaSysexData(byte[] abData, int nStart) {
        return decodeSysexData(abData, nStart);

        // String strData = null;
        // strData = "device " + (abData[nStart + 0] + 1);
        // switch (abData[nStart + 1])
        // {
        // case 0x4B:
        // strData += " CS1x";
        // break;
        // case 0x4C:
        // strData += " MU-80"; // or does it mean XG?
        // break;
        // default:
        // strData += " unknown model";
        // }
        // // byte 2,3,4: address
        // // byte 5: data
        // strData += " " + abData[nStart + 5];
        // if (abData[nStart + 6] == SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE)
        // {
        // strData += " EOX";
        // }
        // else
        // {
        // strData += " [unknown data format, expected EOX]";
        // }
        // return strData;
    }

    private String decodeMessage(MetaMessage message) {
        byte[] abMessage = message.getMessage();
        byte[] abData = message.getData();
        int nDataLength = message.getLength();
        String strMessage = null;

        // System.out.println("data array length: " + abData.length);
        switch (message.getType()) {
        case 0:

            int nSequenceNumber = ((abData[0] & 0xFF) << 8) | (abData[1] & 0xFF);
            strMessage = "Sequence Number: " + nSequenceNumber;
            break;
        case 1:

            String strText = new String(abData);
            strMessage = "Text Event: " + strText;
            break;
        case 2:

            String strCopyrightText = new String(abData);
            strMessage = "Copyright Notice: " + strCopyrightText;
            break;
        case 3:

            String strTrackName = new String(abData);
            strMessage = "Sequence/Track Name: " + strTrackName;
            break;
        case 4:

            String strInstrumentName = new String(abData);
            strMessage = "Instrument Name: " + strInstrumentName;
            break;
        case 5:

            String strLyrics = new String(abData);
            strMessage = "Lyric: " + strLyrics;
            break;
        case 6:

            String strMarkerText = new String(abData);
            strMessage = "Marker: " + strMarkerText;
            break;
        case 7:

            String strCuePointText = new String(abData);
            strMessage = "Cue Point: " + strCuePointText;
            break;
        case 0x20:

            int nChannelPrefix = abData[0] & 0xFF;
            strMessage = "MIDI Channel Prefix: " + nChannelPrefix;
            break;
        case 0x2F:
            strMessage = "End of Track";
            break;
        case 0x51:

            // int nTempo = signedByteToUnsigned(abData[0]) * 65536 +
            // signedByteToUnsigned(abData[1]) * 256 +
            // signedByteToUnsigned(abData[2]);
            // strMessage = "Set Tempo (1s/quarter note): " + nTempo;
            int nTempo = ((abData[0] & 0xFF) << 16) | ((abData[1] & 0xFF) << 8) | (abData[2] & 0xFF); // tempo in microseconds per beat
            float bpm = convertTempo(nTempo);

            // truncate it to 2 digits after point
            bpm = (Math.round(nTempo * 100) / 100.0f);
            strMessage = "Set Tempo: " + bpm + " bpm";
            break;
        case 0x54:

            // System.out.println("data array length: " + abData.length);
            strMessage = "SMTPE Offset: " + (abData[0] & 0xFF) + ":" + (abData[1] & 0xFF) + ":" + (abData[2] & 0xFF) + "." + (abData[3] & 0xFF) + "." + (abData[4] & 0xFF);
            break;
        case 0x58:
            strMessage = "Time Signature: " + (abData[0] & 0xFF) + "/" + (1 << (abData[1] & 0xFF)) + ", MIDI clocks per metronome tick: " + (abData[2] & 0xFF) + ", 1/32 per 24 MIDI clocks: " + (abData[3] & 0xFF);
            break;
        case 0x59:

            String strGender = (abData[1] == 1) ? "minor" : "major";
            strMessage = "Key Signature: " + sm_astrKeySignatures[abData[0] + 7] + " " + strGender;
            break;
        case 0x7F:

            // TODO: decode vendor code, dump data in rows
            String strDataDump = getHexString(abData);
            strMessage = "Sequencer-Specific Meta event: " + strDataDump;
            break;
        default:

            String strUnknownDump = getHexString(abData);
            strMessage = "unknown Meta event: " + strUnknownDump;
            break;
        }
        return strMessage;
    }

    private static String getKeyName(int nKeyNumber) {
        if (nKeyNumber > 127) {
            return "illegal value";
        } else {
            int nNote = nKeyNumber % 12;
            int nOctave = nKeyNumber / 12;
            return sm_astrKeyNames[nNote] + (nOctave - 1);
        }
    }

    private static int get14bitValue(int nLowerPart, int nHigherPart) {
        return (nLowerPart & 0x7F) | ((nHigherPart & 0x7F) << 7);
    }

    private static int signedByteToUnsigned(byte b) {
        return b & 0xFF;
    }

    // convert from microseconds per quarter note to beats per minute and vice
    // versa
    private static float convertTempo(float value) {
        if (value <= 0) {
            value = 0.1f;
        }
        return 60000000000.0f / value;
    }

    /**
     * TODO: This method is a convenience wrapper for
     * {@link #getHexString(byte[], int, int) getHexString(byte[], int, int)}.
     */
    private static String getHexString(byte[] aByte) {
        return getHexString(aByte, 0, aByte.length);
    }

    /**
     * TODO:
     */
    private static String getHexString(byte[] aByte, int nStart, int nLength) {
        StringBuffer sbuf = new StringBuffer((aByte.length * 3) + 2);
        for (int i = nStart; i < nLength; i++) {
            sbuf.append(' ');

            byte bhigh = (byte) ((aByte[i] & 0xf0) >> 4);
            sbuf.append((char) ((bhigh > 9) ? ((bhigh + 'A') - 10) : (bhigh + '0')));

            byte blow = (byte) (aByte[i] & 0x0f);
            sbuf.append((char) ((blow > 9) ? ((blow + 'A') - 10) : (blow + '0')));
        }
        return new String(sbuf).trim();
    }
}
