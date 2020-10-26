/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.uva.emulation;

import com.cozendey.opl3.OPL3;


/**
 * Adlib.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/23 umjammer initial version <br>
 */
class Adlib {
    static final int LUCAS_STYLE = 1;
    static final int CMF_STYLE = 2;
    static final int MIDI_STYLE = 4;
    static final int SIERRA_STYLE = 8;

    static final int MELODIC = 0;
    static final int RYTHM = 1;

    /**
     * This table holds the register offset for operator 1 for each of the nine
     * channels. To get the register offset for operator 2, simply add 3.
     */
    private static final int[] opadd = { 0, 1, 2, 8, 9, 10, 16, 17, 18 };
    /** Standard AdLib frequency table */
    private static final int[] fnums = { 363, 385, 408, 432, 458, 485, 514, 544, 577, 611, 647, 686 };
    /**
     * map CMF drum channels 12 - 15 to corresponding AdLib drum operators
     * bass drum (channel 11) not mapped, cause it's handled like a normal instrument
     */
    private static final int[] map_chan = { 20, 18, 21, 17 };
    /** Map CMF drum channels 11 - 15 to corresponding AdLib drum channels */
    public static final int[] percussion_map = { 6, 7, 8, 8, 7 };
    /** logarithmic relationship between midi and FM volumes */
    public static int[] my_midi_fm_vol_table = {
        0, 11, 16, 19, 22, 25, 27, 29, 32, 33, 35, 37, 39, 40, 42, 43,
        45, 46, 48, 49, 50, 51, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62,
        64, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 75, 76, 77,
        78, 79, 80, 80, 81, 82, 83, 83, 84, 85, 86, 86, 87, 88, 89, 89,
        90, 91, 91, 92, 93, 93, 94, 95, 96, 96, 97, 97, 98, 99, 99, 100,
        101, 101, 102, 103, 103, 104, 104, 105, 106, 106, 107, 107, 108,
        109, 109, 110, 110, 111, 112, 112, 113, 113, 114, 114, 115, 115,
        116, 117, 117, 118, 118, 119, 119, 120, 120, 121, 121, 122, 122,
        123, 123, 124, 124, 125, 125, 126, 126, 127
    };
    static final int[] ops = { 32, 32, 64, 64, 96, 96, 128, 128, 224, 224, 192 };

    int style;
    int mode;
    private int[] data = new int[256];

    // for outer opl3
    @FunctionalInterface
    interface Writer {
        void write(int a, int b, int c);
    }

    // internal opl3
    private OPL3 opl3;

    // for internal opl3
    Adlib() {
        opl3 = new OPL3();
        this.writer = this::write;
    }

    // for internal opl3
    protected void write(int array, int address, int data) {
        opl3.write(array, address, data);
    }

    // for outer opl3
    Writer writer;

    // for outer opl3
    Adlib(Writer writer) {
        this.writer = writer;
    }

    int read(int address) {
        return data[address];
    }

    void write(int address, int data) {
        writer.write(0, address, data);
        this.data[address] = data;
    }

    void instrument(int voice, int[] inst) {
        if ((style & SIERRA_STYLE) != 0) {
            // just gotta make sure this happens..
            // 'cause who knows when it'll be
            // reset otherwise.
            write(0xbd, 0);
        }

        write(0x20 + opadd[voice], inst[0]);
        write(0x23 + opadd[voice], inst[1]);
        if ((style & LUCAS_STYLE) != 0) {
            write(0x43 + opadd[voice], 0x3f);
            if ((inst[10] & 1) == 0) {
                write(0x40 + opadd[voice], inst[2]);
            } else {
                write(0x40 + opadd[voice], 0x3f);
            }
        } else if ((style & SIERRA_STYLE) != 0) {
            write(0x40 + opadd[voice], inst[2]);
            write(0x43 + opadd[voice], inst[3]);
        } else {
            write(0x40 + opadd[voice], inst[2]);
            if ((inst[10] & 1) == 0) {
                write(0x43 + opadd[voice], inst[3]);
            } else {
                write(0x43 + opadd[voice], 0);
            }
        }

        write(0x60 + opadd[voice], inst[4]);
        write(0x63 + opadd[voice], inst[5]);
        write(0x80 + opadd[voice], inst[6]);
        write(0x83 + opadd[voice], inst[7]);
        write(0xe0 + opadd[voice], inst[8]);
        write(0xe3 + opadd[voice], inst[9]);
        write(0xc0 + voice, 0xf0 | inst[10]);
    }

    void percussion(int ch, int[] inst) {
        int opadd = map_chan[ch - 12];
        write(0x20 + opadd, inst[0]);
        write(0x40 + opadd, inst[2]);
        write(0x60 + opadd, inst[4]);
        write(0x80 + opadd, inst[6]);
        write(0xe0 + opadd, inst[8]);
        write(0xc0 + opadd, 0xf0 | inst[10]);
    }

    void volume(int voice, int volume) {
        if ((style & SIERRA_STYLE) == 0) { // sierra likes it loud!
            int vol = volume >> 2;
            if ((style & LUCAS_STYLE) != 0) {
                if ((data[0xc0 + voice] & 1) == 1) {
                    write(0x40 + opadd[voice], 63 - vol | data[0x40 + opadd[voice]] & 0xc0);
                }

                write(0x43 + opadd[voice], 63 - vol | data[0x43 + opadd[voice]] & 0xc0);
            } else {
                if ((data[0xc0 + voice] & 1) == 1) {
                    write(0x40 + opadd[voice], 63 - vol | data[0x40 + opadd[voice]] & 0xc0);
                }

                write(0x43 + opadd[voice], 63 - vol | data[0x43 + opadd[voice]] & 0xc0);
            }
        }
    }

    void playNote(int voice, int note, int volume) {
        if (note < 0) {
            note = 12 - note % 12;
        }

        int freq = fnums[note % 12];
        int oct = note / 12;
        volume(voice, volume);
        write(0xa0 + voice, freq & 0xff);
        int c = ((freq & 0x300) >> 8) + (oct << 2) + (mode == MELODIC || voice < 6 ? (1 << 5) : 0);
        write(0xb0 + voice, c);
    }

    void endNote(int voice) {
        write(0xb0 + voice, data[0xb0 + voice] & (255 - 32));
    }

    void reset() {
        for (int i = 0; i < 256; ++i) {
            write(i, 0);
        }

        for (int i = 0xc0; i <= 0xc8; ++i) {
            write(i, 0xf0);
        }

        write(0x01, 0x20);
        write(0xbd, 0xc0);
    }

    // TODO
    public byte[] readBytes(int len) {
        byte[] buf = new byte[len];
        for (int i = 0; i < len; i += 4) {
            short[] data = opl3.read();
            short chA = data[0];
            short chB = data[1];
            buf[i] = (byte) (chA & 0xff);
            buf[i + 1] = (byte) (chA >> 8 & 0xff);
            buf[i + 2] = (byte) (chB & 0xff);
            buf[i + 3] = (byte) (chB >> 8 & 0xff);
        }
        return buf;
    }
}

/* */
