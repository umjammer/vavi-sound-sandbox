/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.opl3;

import java.util.logging.Level;

import vavi.util.Debug;


/**
 * Adlib.
 *
 * TODO can be oscillator
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/23 umjammer initial version <br>
 */
public class Adlib {
    public static final int LUCAS_STYLE = 1;
    public static final int CMF_STYLE = 2;
    public static final int MIDI_STYLE = 4;
    public static final int SIERRA_STYLE = 8;

    public static final int MELODIC = 0;
    public static final int RYTHM = 1;

    /** This set of GM instrument patches was provided by Jorrit Rouwe... */
    public static final int[][] midi_fm_instruments = {
        { 33, 33, 143, 12, 242, 242, 69, 118, 0, 0, 8, 0, 0, 0 },
        { 49, 33, 75, 9, 242, 242, 84, 86, 0, 0, 8, 0, 0, 0 },
        { 49, 33, 73, 9, 242, 242, 85, 118, 0, 0, 8, 0, 0, 0 },
        { 177, 97, 14, 9, 242, 243, 59, 11, 0, 0, 6, 0, 0, 0 },
        { 1, 33, 87, 9, 241, 241, 56, 40, 0, 0, 0, 0, 0, 0 },
        { 1, 33, 147, 9, 241, 241, 56, 40, 0, 0, 0, 0, 0, 0 },
        { 33, 54, 128, 23, 162, 241, 1, 213, 0, 0, 8, 0, 0, 0 },
        { 1, 1, 146, 9, 194, 194, 168, 88, 0, 0, 10, 0, 0, 0 },
        { 12, 129, 92, 9, 246, 243, 84, 181, 0, 0, 0, 0, 0, 0 },
        { 7, 17, 151, 137, 246, 245, 50, 17, 0, 0, 2, 0, 0, 0 },
        { 23, 1, 33, 9, 86, 246, 4, 4, 0, 0, 2, 0, 0, 0 },
        { 24, 129, 98, 9, 243, 242, 230, 246, 0, 0, 0, 0, 0, 0 },
        { 24, 33, 35, 9, 247, 229, 85, 216, 0, 0, 0, 0, 0, 0 },
        { 21, 1, 145, 9, 246, 246, 166, 230, 0, 0, 4, 0, 0, 0 },
        { 69, 129, 89, 137, 211, 163, 130, 227, 0, 0, 12, 0, 0, 0 },
        { 3, 129, 73, 137, 116, 179, 85, 5, 1, 0, 4, 0, 0, 0 },
        { 113, 49, 146, 9, 246, 241, 20, 7, 0, 0, 2, 0, 0, 0 },
        { 114, 48, 20, 9, 199, 199, 88, 8, 0, 0, 2, 0, 0, 0 },
        { 112, 177, 68, 9, 170, 138, 24, 8, 0, 0, 4, 0, 0, 0 },
        { 35, 177, 147, 9, 151, 85, 35, 20, 1, 0, 4, 0, 0, 0 },
        { 97, 177, 19, 137, 151, 85, 4, 4, 1, 0, 0, 0, 0, 0 },
        { 36, 177, 72, 9, 152, 70, 42, 26, 1, 0, 12, 0, 0, 0 },
        { 97, 33, 19, 9, 145, 97, 6, 7, 1, 0, 10, 0, 0, 0 },
        { 33, 161, 19, 146, 113, 97, 6, 7, 0, 0, 6, 0, 0, 0 },
        { 2, 65, 156, 137, 243, 243, 148, 200, 1, 0, 12, 0, 0, 0 },
        { 3, 17, 84, 9, 243, 241, 154, 231, 1, 0, 12, 0, 0, 0 },
        { 35, 33, 95, 9, 241, 242, 58, 248, 0, 0, 0, 0, 0, 0 },
        { 3, 33, 135, 137, 246, 243, 34, 248, 1, 0, 6, 0, 0, 0 },
        { 3, 33, 71, 9, 249, 246, 84, 58, 0, 0, 0, 0, 0, 0 },
        { 35, 33, 74, 14, 145, 132, 65, 25, 1, 0, 8, 0, 0, 0 },
        { 35, 33, 74, 9, 149, 148, 25, 25, 1, 0, 8, 0, 0, 0 },
        { 9, 132, 161, 137, 32, 209, 79, 248, 0, 0, 8, 0, 0, 0 },
        { 33, 162, 30, 9, 148, 195, 6, 166, 0, 0, 2, 0, 0, 0 },
        { 49, 49, 18, 9, 241, 241, 40, 24, 0, 0, 10, 0, 0, 0 },
        { 49, 49, 141, 9, 241, 241, 232, 120, 0, 0, 10, 0, 0, 0 },
        { 49, 50, 91, 9, 81, 113, 40, 72, 0, 0, 12, 0, 0, 0 },
        { 1, 33, 139, 73, 161, 242, 154, 223, 0, 0, 8, 0, 0, 0 },
        { 33, 33, 139, 17, 162, 161, 22, 223, 0, 0, 8, 0, 0, 0 },
        { 49, 49, 139, 9, 244, 241, 232, 120, 0, 0, 10, 0, 0, 0 },
        { 49, 49, 18, 9, 241, 241, 40, 24, 0, 0, 10, 0, 0, 0 },
        { 49, 33, 21, 9, 221, 86, 19, 38, 1, 0, 8, 0, 0, 0 },
        { 49, 33, 22, 9, 221, 102, 19, 6, 1, 0, 8, 0, 0, 0 },
        { 113, 49, 73, 9, 209, 97, 28, 12, 1, 0, 8, 0, 0, 0 },
        { 33, 35, 77, 137, 113, 114, 18, 6, 1, 0, 2, 0, 0, 0 },
        { 241, 225, 64, 9, 241, 111, 33, 22, 1, 0, 2, 0, 0, 0 },
        { 2, 1, 26, 137, 245, 133, 117, 53, 1, 0, 0, 0, 0, 0 },
        { 2, 1, 29, 137, 245, 243, 117, 244, 1, 0, 0, 0, 0, 0 },
        { 16, 17, 65, 9, 245, 242, 5, 195, 1, 0, 2, 0, 0, 0 },
        { 33, 162, 155, 10, 177, 114, 37, 8, 1, 0, 14, 0, 0, 0 },
        { 161, 33, 152, 9, 127, 63, 3, 7, 1, 1, 0, 0, 0, 0 },
        { 161, 97, 147, 9, 193, 79, 18, 5, 0, 0, 10, 0, 0, 0 },
        { 33, 97, 24, 9, 193, 79, 34, 5, 0, 0, 12, 0, 0, 0 },
        { 49, 114, 91, 140, 244, 138, 21, 5, 0, 0, 0, 0, 0, 0 },
        { 161, 97, 144, 9, 116, 113, 57, 103, 0, 0, 0, 0, 0, 0 },
        { 113, 114, 87, 9, 84, 122, 5, 5, 0, 0, 12, 0, 0, 0 },
        { 144, 65, 0, 9, 84, 165, 99, 69, 0, 0, 8, 0, 0, 0 },
        { 33, 33, 146, 10, 133, 143, 23, 9, 0, 0, 12, 0, 0, 0 },
        { 33, 33, 148, 14, 117, 143, 23, 9, 0, 0, 12, 0, 0, 0 },
        { 33, 97, 148, 9, 118, 130, 21, 55, 0, 0, 12, 0, 0, 0 },
        { 49, 33, 67, 9, 158, 98, 23, 44, 1, 1, 2, 0, 0, 0 },
        { 33, 33, 155, 9, 97, 127, 106, 10, 0, 0, 2, 0, 0, 0 },
        { 97, 34, 138, 15, 117, 116, 31, 15, 0, 0, 8, 0, 0, 0 },
        { 161, 33, 134, 140, 114, 113, 85, 24, 1, 0, 0, 0, 0, 0 },
        { 33, 33, 77, 9, 84, 166, 60, 28, 0, 0, 8, 0, 0, 0 },
        { 49, 97, 143, 9, 147, 114, 2, 11, 1, 0, 8, 0, 0, 0 },
        { 49, 97, 142, 9, 147, 114, 3, 9, 1, 0, 8, 0, 0, 0 },
        { 49, 97, 145, 9, 147, 130, 3, 9, 1, 0, 10, 0, 0, 0 },
        { 49, 97, 142, 9, 147, 114, 15, 15, 1, 0, 10, 0, 0, 0 },
        { 33, 33, 75, 9, 170, 143, 22, 10, 1, 0, 8, 0, 0, 0 },
        { 49, 33, 144, 9, 126, 139, 23, 12, 1, 1, 6, 0, 0, 0 },
        { 49, 50, 129, 9, 117, 97, 25, 25, 1, 0, 0, 0, 0, 0 },
        { 50, 33, 144, 9, 155, 114, 33, 23, 0, 0, 4, 0, 0, 0 },
        { 225, 225, 31, 9, 133, 101, 95, 26, 0, 0, 0, 0, 0, 0 },
        { 225, 225, 70, 9, 136, 101, 95, 26, 0, 0, 0, 0, 0, 0 },
        { 161, 33, 156, 9, 117, 117, 31, 10, 0, 0, 2, 0, 0, 0 },
        { 49, 33, 139, 9, 132, 101, 88, 26, 0, 0, 0, 0, 0, 0 },
        { 225, 161, 76, 9, 102, 101, 86, 38, 0, 0, 0, 0, 0, 0 },
        { 98, 161, 203, 9, 118, 85, 70, 54, 0, 0, 0, 0, 0, 0 },
        { 98, 161, 162, 9, 87, 86, 7, 7, 0, 0, 11, 0, 0, 0 },
        { 98, 161, 156, 9, 119, 118, 7, 7, 0, 0, 11, 0, 0, 0 },
        { 34, 33, 89, 9, 255, 255, 3, 15, 2, 0, 0, 0, 0, 0 },
        { 33, 33, 14, 9, 255, 255, 15, 15, 1, 1, 0, 0, 0, 0 },
        { 34, 33, 70, 137, 134, 100, 85, 24, 0, 0, 0, 0, 0, 0 },
        { 33, 161, 69, 9, 102, 150, 18, 10, 0, 0, 0, 0, 0, 0 },
        { 33, 34, 139, 9, 146, 145, 42, 42, 1, 0, 0, 0, 0, 0 },
        { 162, 97, 158, 73, 223, 111, 5, 7, 0, 0, 2, 0, 0, 0 },
        { 32, 96, 26, 9, 239, 143, 1, 6, 0, 2, 0, 0, 0, 0 },
        { 33, 33, 143, 134, 241, 244, 41, 9, 0, 0, 10, 0, 0, 0 },
        { 119, 161, 165, 9, 83, 160, 148, 5, 0, 0, 2, 0, 0, 0 },
        { 97, 177, 31, 137, 168, 37, 17, 3, 0, 0, 10, 0, 0, 0 },
        { 97, 97, 23, 9, 145, 85, 52, 22, 0, 0, 12, 0, 0, 0 },
        { 113, 114, 93, 9, 84, 106, 1, 3, 0, 0, 0, 0, 0, 0 },
        { 33, 162, 151, 9, 33, 66, 67, 53, 0, 0, 8, 0, 0, 0 },
        { 161, 33, 28, 9, 161, 49, 119, 71, 1, 1, 0, 0, 0, 0 },
        { 33, 97, 137, 12, 17, 66, 51, 37, 0, 0, 10, 0, 0, 0 },
        { 161, 33, 21, 9, 17, 207, 71, 7, 1, 0, 0, 0, 0, 0 },
        { 58, 81, 206, 9, 248, 134, 246, 2, 0, 0, 2, 0, 0, 0 },
        { 33, 33, 21, 9, 33, 65, 35, 19, 1, 0, 0, 0, 0, 0 },
        { 6, 1, 91, 9, 116, 165, 149, 114, 0, 0, 0, 0, 0, 0 },
        { 34, 97, 146, 140, 177, 242, 129, 38, 0, 0, 12, 0, 0, 0 },
        { 65, 66, 77, 9, 241, 242, 81, 245, 1, 0, 0, 0, 0, 0 },
        { 97, 163, 148, 137, 17, 17, 81, 19, 1, 0, 6, 0, 0, 0 },
        { 97, 161, 140, 137, 17, 29, 49, 3, 0, 0, 6, 0, 0, 0 },
        { 164, 97, 76, 9, 243, 129, 115, 35, 1, 0, 4, 0, 0, 0 },
        { 2, 7, 133, 12, 210, 242, 83, 246, 0, 1, 0, 0, 0, 0 },
        { 17, 19, 12, 137, 163, 162, 17, 229, 1, 0, 0, 0, 0, 0 },
        { 17, 17, 6, 9, 246, 242, 65, 230, 1, 2, 4, 0, 0, 0 },
        { 147, 145, 145, 9, 212, 235, 50, 17, 0, 1, 8, 0, 0, 0 },
        { 4, 1, 79, 9, 250, 194, 86, 5, 0, 0, 12, 0, 0, 0 },
        { 33, 34, 73, 9, 124, 111, 32, 12, 0, 1, 6, 0, 0, 0 },
        { 49, 33, 133, 9, 221, 86, 51, 22, 1, 0, 10, 0, 0, 0 },
        { 32, 33, 4, 138, 218, 143, 5, 11, 2, 0, 6, 0, 0, 0 },
        { 5, 3, 106, 137, 241, 195, 229, 229, 0, 0, 6, 0, 0, 0 },
        { 7, 2, 21, 9, 236, 248, 38, 22, 0, 0, 10, 0, 0, 0 },
        { 5, 1, 157, 9, 103, 223, 53, 5, 0, 0, 8, 0, 0, 0 },
        { 24, 18, 150, 9, 250, 248, 40, 229, 0, 0, 10, 0, 0, 0 },
        { 16, 0, 134, 12, 168, 250, 7, 3, 0, 0, 6, 0, 0, 0 },
        { 17, 16, 65, 12, 248, 243, 71, 3, 2, 0, 4, 0, 0, 0 },
        { 1, 16, 142, 9, 241, 243, 6, 2, 2, 0, 14, 0, 0, 0 },
        { 14, 192, 0, 9, 31, 31, 0, 255, 0, 3, 14, 0, 0, 0 },
        { 6, 3, 128, 145, 248, 86, 36, 132, 0, 2, 14, 0, 0, 0 },
        { 14, 208, 0, 14, 248, 52, 0, 4, 0, 3, 14, 0, 0, 0 },
        { 14, 192, 0, 9, 246, 31, 0, 2, 0, 3, 14, 0, 0, 0 },
        { 213, 218, 149, 73, 55, 86, 163, 55, 0, 0, 0, 0, 0, 0 },
        { 53, 20, 92, 17, 178, 244, 97, 21, 2, 0, 10, 0, 0, 0 },
        { 14, 208, 0, 9, 246, 79, 0, 245, 0, 3, 14, 0, 0, 0 },
        { 38, 228, 0, 9, 255, 18, 1, 22, 0, 1, 14, 0, 0, 0 },
        { 0, 0, 0, 9, 243, 246, 240, 201, 0, 2, 14, 0, 0, 0}
    };

    /**
     * This table holds the register offset for operator 1 for each of the nine
     * channels. To get the register offset for operator 2, simply add 3.
     */
    private static final int[] opadd = { 0, 1, 2, 8, 9, 10, 16, 17, 18 };
    /** Standard AdLib frequency table */
    private static final int[] fnums = { 363, 385, 408, 432, 458, 485, 514, 544, 577, 611, 647, 686 };
    /**
     * map CMF drum channels 12 - 15 to corresponding AdLib drum operators
     * bass drum (channel 11) not mapped, because it's handled like a normal instrument
     */
    private static final int[] map_chan = { 20, 18, 21, 17 };
    /** Map CMF drum channels 11 - 15 to corresponding AdLib drum channels */
    public static final int[] percussion_map = { 6, 7, 8, 8, 7 };
    /** logarithmic relationship between midi and FM volumes */
    public static final int[] my_midi_fm_vol_table = {
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
    private static final int[] ops = { 32, 32, 64, 64, 96, 96, 128, 128, 224, 224, 192 };

    public int style;
    public int mode;
    private final int[] data = new int[256];

    /** for outer opl3 */
    @FunctionalInterface
    public interface Writer {
        void write(int array, int address, int data);
    }

    /** internal opl3 */
    private OPL3 opl3;

    /** for internal opl3 */
    public Adlib() {
        opl3 = new OPL3();
        this.writer = this::write;
    }

    /** for internal opl3 */
    protected void write(int array, int address, int data) {
        opl3.write(array, address, data);
    }

    /** for outer opl3 */
    private final Writer writer;

    /** for outer opl3 */
    public Adlib(Writer writer) {
        this.writer = writer;
    }

    public boolean isOplInternal() {
        return opl3 != null;
    }

    public int read(int address) {
        return data[address];
    }

    public void write(int address, int data) {
Debug.printf(Level.FINEST, "write: %04x, %02x", address, data);
        writer.write(0, address, data);
        this.data[address] = data;
    }

    public void instrument(int voice, int[] inst) {
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
        } else if ((style & SIERRA_STYLE) != 0 || (style & CMF_STYLE) != 0) {
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

    public void percussion(int ch, int[] inst) {
        int opadd = map_chan[ch - 12];
        write(0x20 + opadd, inst[0]);
        write(0x40 + opadd, inst[2]);
        write(0x60 + opadd, inst[4]);
        write(0x80 + opadd, inst[6]);
        write(0xe0 + opadd, inst[8]);
        write(0xc0 + opadd, 0xf0 | inst[10]);
    }

    public void volume(int voice, int volume) {
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

    public void playNote(int voice, int note, int volume) {
        if (note < 0) {
            note = 12 - note % 12;
        }

        int freq = fnums[note % 12];
        int oct = note / 12;
        volume(voice, volume);
        write(0xa0 + voice, freq & 0xff);
        int c = ((freq & 0x300) >> 8) + ((oct & 7) << 2) + (mode == MELODIC || voice < 6 ? (1 << 5) : 0);
        write(0xb0 + voice, c);
    }

    public void endNote(int voice) {
        write(0xb0 + voice, data[0xb0 + voice] & (255 - 32));
    }

    public void reset() {
        for (int i = 0; i < 256; ++i) {
            write(i, 0);
        }

        for (int i = 0xc0; i <= 0xc8; ++i) {
            write(i, 0xf0);
        }

        write(0x01, 0x20);
        write(0xbd, 0xc0);
    }

    public int read(byte[] buf, int ofs, int len) {
        for (int i = ofs; i < len; i += 4) {
            short[] data = opl3.read();
            short chA = data[0];
            short chB = data[1];
            buf[i] = (byte) (chA & 0xff);
            buf[i + 1] = (byte) (chA >> 8 & 0xff);
            buf[i + 2] = (byte) (chB & 0xff);
            buf[i + 3] = (byte) (chB >> 8 & 0xff);
        }
        return len;
    }
}
