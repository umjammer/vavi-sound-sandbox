/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2008 Simon Peter, <dn.tlp@gmx.net>, et al.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *
 * MIDI & MIDI-like file player - Last Update: 10/15/2005
 *                  by Phil Hassey - www.imitationpickles.org
 *                                   philhassey@hotmail.com
 *
 * Can play the following
 *      .LAA - a raw save of a Lucas Arts Adlib music
 *             or
 *             a raw save of a LucasFilm Adlib music
 *      .MID - a "midi" save of a Lucas Arts Adlib music
 *           - or general MIDI files
 *      .CMF - Creative Music Format
 *      .SCI - the sierra "midi" format.
 *             Files must be in the form
 *             xxxNAME.sci
 *             So that the loader can load the right patch file:
 *             xxxPATCH.003  (patch.003 must be saved from the
 *                            sierra resource from each game.)
 *
 * 6/2/2000:  v1.0 relased by phil hassey
 *      Status:  LAA is almost perfect
 *                      - some volumes are a bit off (intrument too quiet)
 *               MID is fine (who wants to listen to MIDI vid adlib anyway)
 *               CMF is okay (still needs the adlib rythm mode implemented
 *                            for real)
 * 6/6/2000:
 *      Status:  SCI:  there are two SCI formats, orginal and advanced.
 *                    original:  (Found in SCI/EGA Sierra Adventures)
 *                               played almost perfectly, I believe
 *                               there is one mistake in the instrument
 *                               loader that causes some sounds to
 *                               not be quite right.  Most sounds are fine.
 *                    advanced:  (Found in SCI/VGA Sierra Adventures)
 *                               These are multi-track files.  (Thus the
 *                               player had to be modified to work with
 *                               them.)  This works fine.
 *                               There are also multiple tunes in each file.
 *                               I think some of them are supposed to be
 *                               played at the same time, but I'm not sure
 *                               when.
 * 8/16/2000:
 *      Status:  LAA: now EGA and VGA lucas games work pretty well
 *
 * 10/15/2005: Changes by Simon Peter
 *  Added rhythm mode support for CMF format.
 *
 * 09/13/2008: Changes by Adam Nielsen (malvineous@shikadi.net)
 *      Fixed a couple of CMF rhythm mode bugs
 *      Disabled note velocity for CMF files
 *      Added support for nonstandard CMF AM+VIB controller (for VGFM CMFs)
 *
 * Other acknowledgements:
 *  Allegro - for the midi instruments and the midi volume table
 *  SCUMM Revisited - for getting the .LAA / .MIDs out of those
 *                    LucasArts files.
 *  FreeSCI - for some information on the sci music files
 *  SD - the SCI Decoder (to get all .sci out of the Sierra files)
 */

package org.uva.emulation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;


class MidPlayer extends Opl3Player {

    static Logger logger = Logger.getLogger(MidPlayer.class.getName());

    static final int LUCAS_STYLE = 1;
    static final int CMF_STYLE = 2;
    static final int MIDI_STYLE = 4;
    static final int SIERRA_STYLE = 8;

    static final int ADLIB_MELODIC = 0;
    static final int ADLIB_RYTHM = 1;

    static final int FILE_LUCAS = 1;
    static final int FILE_MIDI = 2;
    static final int FILE_CMF = 3;
    static final int FILE_SIERRA = 4;
    static final int FILE_ADVSIERRA = 5;
    static final int FILE_OLDLUCAS = 6;

    static final int[] adlib_opadd = { 0, 1, 2, 8, 9, 10, 16, 17, 18 };
    static final int[] ops = { 32, 32, 64, 64, 96, 96, 128, 128, 224, 224, 192 };
    /**
     * map CMF drum channels 12 - 15 to corresponding AdLib drum operators
     * bass drum (channel 11) not mapped, cause it's handled like a normal instrument
     */
    static final int[] map_chan = { 20, 18, 21, 17 };
    /** Standard AdLib frequency table */
    static final int[] fnums = { 363, 385, 408, 432, 458, 485, 514, 544, 577, 611, 647, 686 };
    /** Map CMF drum channels 11 - 15 to corresponding AdLib drum channels */
    static final int[] percussion_map = { 6, 7, 8, 8, 7 };
    /** This set of GM instrument patches was provided by Jorrit Rouwe... */
    int[][] midi_fm_instruments = {
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
    /** logarithmic relationship between midi and FM volumes */
    static int[] my_midi_fm_vol_table = {
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

    static class MidiTrack {
        int tend;
        int spos;
        int pos;
        int iwait;
        int on;
        int pv;
    }

    static class MidiChannel {
        int inum;
        int[] ins = new int[11];
        int vol;
        int nshift;
        int on;
    }

    String author;
    String title;
    String remarks;
    String emptystr;
    int flen;
    int pos;
    int sierra_pos;
    int subsongs;
    byte[] data;
    int[] adlib_data = new int[256];
    int adlib_style;
    int adlib_mode;
    int[][] myinsbank = new int[128][16];
    int[][] smyinsbank = new int[128][16];
    MidiChannel[] ch = new MidiChannel[16];
    int[][] chp = new int[18][3];
    int deltas;
    int msqtr;
    MidiTrack[] track = new MidiTrack[16];
    int curtrack;
    float fwait;
    int iwait;
    int doing;
    int type;
    int tins;
    int stins;

    String getTitle() {
        return title;
    }

    String getAuthor() {
        return author;
    }

    String getDesc() {
        return remarks;
    }

    int getInstruments() {
        return tins;
    }

    int getSubSongs() {
        return subsongs;
    }

    public int getTotalMiliseconds() {
        return 0;
    }

    public MidPlayer() {
        author = title = remarks = emptystr = null;
        flen = 0;
        data = null;

        for (int i = 0; i < ch.length; ++i) {
            ch[i] = new MidiChannel();
        }

        for (int i = 0; i < track.length; ++i) {
            track[i] = new MidiTrack();
        }
    }

    int lookData(int pos) {
        return pos >= 0 && pos < flen ? (data[pos] & 0xff) : 0;
    }

    int getNextI(int num) {
        int v = 0;

        for (int i = 0; i < num; ++i) {
            v += lookData(pos) << 8 * i;
            ++pos;
        }

        return v;
    }

    int getNext(int num) {
        int b = 0;

        for (int i = 0; i < num; ++i) {
            b <<= 8;
            b += lookData(pos);
            ++pos;
        }

        return b;
    }

    int getVal() {
        int b = getNext(1);
        int v = b & 0x7f;
        while ((b & 0x80) != 0) {
            b = getNext(1);
            v = (v << 7) + (b & 0x7F);
        }

        return v;
    }

    boolean loadSierraIns(String name) throws IOException {
        int[] buf = new int[28];
        StringBuffer sb = new StringBuffer(name.length() + 9);
        sb.append(name);
        long p = 0L;

        for (int i = sb.length() - 1; i >= 0; --i) {
            if (sb.charAt(i) == '/' || sb.charAt(i) == '\\') {
                p = i + 1;
                break;
            }
        }

        sb.replace((int) (p + 3), sb.length() - 1, "patch.003");
        File file = new File(sb.toString());
        FileInputStream fis = new FileInputStream(file);
        fis.skip(2);
        stins = 0;

        for (int i = 0; i < 2; ++i) {
            for (int k = 0; k < 48; ++k) {
                int l = i * 48 + k;
                logger.fine(String.format("\n%2d: ", l));

                for (int j = 0; j < 28; ++j) {
                    buf[j] = fis.read();
                }

                myinsbank[l][0] = buf[9] * 0x80 + buf[10] * 0x40 + buf[5] * 0x20 + buf[11] * 0x10 + buf[1];
                myinsbank[l][1] = buf[22] * 0x80 + buf[23] * 0x40 + buf[18] * 0x20 + buf[24] * 0x10 + buf[14];
                myinsbank[l][2] = (buf[0] << 6) + buf[8];
                myinsbank[l][3] = (buf[13] << 6) + buf[21];
                myinsbank[l][4] = (buf[3] << 4) + buf[6];
                myinsbank[l][5] = (buf[16] << 4) + buf[19];
                myinsbank[l][6] = (buf[4] << 4) + buf[7];
                myinsbank[l][7] = (buf[17] << 4) + buf[20];
                myinsbank[l][8] = buf[26];
                myinsbank[l][9] = buf[27];
                myinsbank[l][10] = (buf[2] << 1) + (1 - (buf[12] & 1));

                for (int j = 0; j < 11; ++j) {
                    logger.fine(String.format("%02X ", myinsbank[l][j]));
                }

                ++stins;
            }

            fis.skip(2);
        }

        fis.close();
        smyinsbank = myinsbank.clone();
        return true;
    }

    void sierra_next_section() {
        for (int i = 0; i < 16; ++i) {
            track[i].on = 0;
        }

        logger.fine("\n\nnext adv sierra section:\n");
        pos = sierra_pos;
        int j = 0;

        for (int i = 0; i != 255; i = getNext(1)) {
            getNext(1);
            curtrack = j++;
            track[curtrack].on = 1;
            track[curtrack].spos = getNext(1);
            track[curtrack].spos += (getNext(1) << 8) + 4; // 4 best usually +3? not 0,1,2 or 5
            track[curtrack].tend = flen;
            track[curtrack].iwait = 0;
            track[curtrack].pv = 0;
            logger.fine(String.format("track %d starts at %x\n", curtrack, track[curtrack].spos));
            getNext(2);
        }

        getNext(2);
        deltas = 32;
        sierra_pos = pos;
        fwait = 0.0F;
        doing = 1;
    }

    public void load(byte[] file) throws IOException {

        byte good = 0;
        subsongs = 0;
        switch (file[0] & 0xff) {
        case 'A':
            if (file[1] == 'D' && file[2] == 'L') {
                good = FILE_LUCAS;
            }
            break;
        case 'M':
            if (file[1] == 'T' && file[2] == 'h' && file[3] == 'd') {
                good = FILE_MIDI;
            }
            break;
        case 'C':
            if (file[1] == 'T' && file[2] == 'M' && file[3] == 'F') {
                good = FILE_CMF;
            }
            break;
        case 0x84:
            if (file[1] == 0 && loadSierraIns(file.toString())) {
                if ((file[2] & 0xff) == 0xf0) {
                    good = FILE_ADVSIERRA;
                } else {
                    good = FILE_SIERRA;
                }
            }
            break;
        default:
            if (file[4] == 'A' && file[5] == 'D') {
                good = FILE_OLDLUCAS;
            }
        }

        if (good == 0) {
            throw new IllegalArgumentException("unsupported type");
        } else {
            subsongs = 1;
            type = good;
logger.fine("type: " + type);
            flen = file.length;
            data = file;

            rewind(0);
        }
    }

    void midi_write_adlib(int r, int v) {
        write(0, r, v);
        adlib_data[r] = v;
    }

    void midi_fm_instrument(int voice, int[] inst) {
        if ((adlib_style & 8) != 0) {
            // just gotta make sure this happens..
            // 'cause who knows when it'll be
            // reset otherwise.
            midi_write_adlib(0xbd, 0);
        }

        midi_write_adlib(0x20 + adlib_opadd[voice], inst[0]);
        midi_write_adlib(0x23 + adlib_opadd[voice], inst[1]);
        if ((adlib_style & LUCAS_STYLE) != 0) {
            midi_write_adlib(0x43 + adlib_opadd[voice], 0x3f);
            if ((inst[10] & 1) == 0) {
                midi_write_adlib(0x40 + adlib_opadd[voice], inst[2]);
            } else {
                midi_write_adlib(0x40 + adlib_opadd[voice], 0x3f);
            }
        } else if ((adlib_style & SIERRA_STYLE) != 0) {
            midi_write_adlib(0x40 + adlib_opadd[voice], inst[2]);
            midi_write_adlib(0x43 + adlib_opadd[voice], inst[3]);
        } else {
            midi_write_adlib(0x40 + adlib_opadd[voice], inst[2]);
            if ((inst[10] & 1) == 0) {
                midi_write_adlib(0x43 + adlib_opadd[voice], inst[3]);
            } else {
                midi_write_adlib(0x43 + adlib_opadd[voice], 0);
            }
        }

        midi_write_adlib(0x60 + adlib_opadd[voice], inst[4]);
        midi_write_adlib(0x63 + adlib_opadd[voice], inst[5]);
        midi_write_adlib(0x80 + adlib_opadd[voice], inst[6]);
        midi_write_adlib(0x83 + adlib_opadd[voice], inst[7]);
        midi_write_adlib(0xe0 + adlib_opadd[voice], inst[8]);
        midi_write_adlib(0xe3 + adlib_opadd[voice], inst[9]);
        midi_write_adlib(0xc0 + voice, 0xf0 | inst[10]);
    }

    void midi_fm_percussion(int ch, int[] inst) {
        int opadd = map_chan[ch - 12];
        midi_write_adlib(0x20 + opadd, inst[0]);
        midi_write_adlib(0x40 + opadd, inst[2]);
        midi_write_adlib(0x60 + opadd, inst[4]);
        midi_write_adlib(0x80 + opadd, inst[6]);
        midi_write_adlib(0xe0 + opadd, inst[8]);
        midi_write_adlib(0xc0 + opadd, 0xf0 | inst[10]);
    }

    void midi_fm_volume(int voice, int volume) {
        if ((adlib_style & SIERRA_STYLE) == 0) { //sierra likes it loud!
            int vol = volume >> 2;
            if ((adlib_style & LUCAS_STYLE) != 0) {
                if ((adlib_data[0xc0 + voice] & 1) == 1) {
                    midi_write_adlib(0x40 + adlib_opadd[voice], 63 - vol | adlib_data[0x40 + adlib_opadd[voice]] & 0xc0);
                }

                midi_write_adlib(0x43 + adlib_opadd[voice], 63 - vol | adlib_data[0x43 + adlib_opadd[voice]] & 0xc0);
            } else {
                if ((adlib_data[0xc0 + voice] & 1) == 1) {
                    midi_write_adlib(0x40 + adlib_opadd[voice], 63 - vol | adlib_data[0x40 + adlib_opadd[voice]] & 0xc0);
                }

                midi_write_adlib(0x43 + adlib_opadd[voice], 63 - vol | adlib_data[0x43 + adlib_opadd[voice]] & 0xc0);
            }
        }
    }

    void midi_fm_playnote(int voice, int note, int volume) {
        if (note < 0) {
            note = 12 - note % 12;
        }

        int freq = fnums[note % 12];
        int oct = note / 12;
        midi_fm_volume(voice, volume);
        midi_write_adlib(0xa0 + voice, freq & 0xff);
        int c = ((freq & 0x300) >> 8) + (oct << 2) + (adlib_mode == ADLIB_MELODIC || voice < 6 ? (1 << 5) : 0);
        midi_write_adlib(0xb0 + voice, c);
    }

    void midi_fm_endnote(int voice) {
        midi_write_adlib(0xb0 + voice, adlib_data[0xb0 + voice] & (255 - 32));
    }

    void midi_fm_reset() {
        for (int i = 0; i < 256; ++i) {
            midi_write_adlib(i, 0);
        }

        for (int i = 0xc0; i <= 0xc8; ++i) {
            midi_write_adlib(i, 0xf0);
        }

        midi_write_adlib(0x01, 0x20);
        midi_write_adlib(0xbd, 0xc0);
    }

    public boolean update() {
        if (doing == 1) {
            for (curtrack = 0; curtrack < 16; ++curtrack) {
                if (track[curtrack].on != 0) {
                    pos = track[curtrack].pos;
                    if (type != FILE_SIERRA && type != FILE_ADVSIERRA) {
                        track[curtrack].iwait += getVal();
                    } else {
                        track[curtrack].iwait += getNext(1);
                    }

                    track[curtrack].pos = pos;
                }
            }

            doing = 0;
        }

        iwait = 0;
        boolean ret = true;

        while (iwait == 0 && ret) {
            for (curtrack = 0; curtrack < 16; ++curtrack) {
                if (track[curtrack].on != 0 && track[curtrack].iwait == 0L && track[curtrack].pos < track[curtrack].tend) {
                    pos = track[curtrack].pos;
                    int v = getNext(1);
                    if (v < 0x80) {
                        v = track[curtrack].pv;
                        --pos;
                    }

                    track[curtrack].pv = v;
                    int c = v & 0x0f;
                    logger.fine(String.format("[%2X]", v));
                    int note;
                    int vel;
                    int x;
                    switch (v & 0xf0) {
                    case 0x80: // note off
                        note = getNext(1);
                        vel = getNext(1);
                        for (int i = 0; i < 9; ++i) {
                            if (chp[i][0] == c && chp[i][1] == note) {
                                midi_fm_endnote(i);
                                chp[i][0] = -1;
                            }
                        }
                        break;
                    case 0x90: // note on
                        note = getNext(1);
                        vel = getNext(1);
                        byte numchan;
                        if (adlib_mode == ADLIB_RYTHM) {
                            numchan = 6;
                        } else {
                            numchan = 9;
                        }

                        if (ch[c].on != 0) {
                            for (int i = 0; i < 18; ++i) {
                                ++chp[i][2];
                            }

                            int on;
                            if (c < 11 || adlib_mode == ADLIB_MELODIC) {
                                boolean f = false;
                                on = -1;
                                int onl = 0;

                                for (int i = 0; i < numchan; ++i) {
                                    if (chp[i][0] == -1 && chp[i][2] > onl) {
                                        onl = chp[i][2];
                                        on = i;
                                        f = true;
                                    }
                                }

                                if (on == -1) {
                                    onl = 0;

                                    for (int i = 0; i < numchan; ++i) {
                                        if (chp[i][2] > onl) {
                                            onl = chp[i][2];
                                            on = i;
                                        }
                                    }
                                }

                                if (!f) {
                                    midi_fm_endnote(on);
                                }
                            } else {
                                on = percussion_map[c - 11];
                            }

                            if (vel != 0L && ch[c].inum >= 0 && ch[c].inum < 128) {
                                if (adlib_mode == ADLIB_MELODIC || c < 12) {
                                    midi_fm_instrument(on, ch[c].ins);
                                } else {
                                    midi_fm_percussion(c, ch[c].ins);
                                }

                                int nv;
                                if ((adlib_style & MIDI_STYLE) != 0) {
                                    nv = ch[c].vol * vel / 128;
                                    if ((adlib_style & LUCAS_STYLE) != 0) {
                                        nv *= 2L;
                                    }

                                    if (nv > 127) {
                                        nv = 127;
                                    }

                                    nv = my_midi_fm_vol_table[nv];
                                    if ((adlib_style & LUCAS_STYLE) != 0) {
                                        nv = (int) ((float) Math.sqrt((nv)) * 11.0F);
                                    }
                                } else {
                                    nv = vel;
                                }

                                midi_fm_playnote(on, note + ch[c].nshift, nv * 2);

                                chp[on][0] = c;
                                chp[on][1] = note;
                                chp[on][2] = 0;

                                if (adlib_mode == 1 && c >= 11) {
                                    midi_write_adlib(0xbd, adlib_data[0xbd] & ~(16 >> (c - 11)));
                                    midi_write_adlib(0xbd, adlib_data[0xbd] | 16 >> (c - 11));
                                }
                            } else if (vel == 0L) { // same code as end note
                                for (int i = 0; i < 9; ++i) {
                                    if (chp[i][0] == c && chp[i][1] == note) {
                                        midi_fm_endnote(i);
                                        chp[i][0] = -1;
                                    }
                                }
                            } else { // i forget what this is for.
                                chp[on][0] = -1;
                                chp[on][2] = 0;
                            }

                            logger.fine(String.format(" [%d:%d:%d:%d]\n", c, ch[c].inum, note, vel));
                        } else {
                            logger.fine("off");
                        }
                        break;
                    case 0xa0: // key after touch
                        note = getNext(1);
                        vel = getNext(1);
                        break;
                    case 0xb0: // control change .. pitch bend?
                        long ctrl = getNext(1);
                        vel = getNext(1);
                        switch ((int) ctrl) {
                        case 0x07:
                            logger.fine(String.format("(pb:%d: %d %d)", c, ctrl, vel));
                            ch[c].vol = vel;
                            logger.fine("vol");
                            break;
                        case 0x67:
                            logger.fine(String.format("\n\nhere:%d\n\n", vel));
                            if ((adlib_style & CMF_STYLE) != 0) {
                                adlib_mode = vel;
                                if (adlib_mode == ADLIB_RYTHM) {
                                    midi_write_adlib(0xbd, adlib_data[0xbd] | (1 << 5));
                                } else {
                                    midi_write_adlib(0xbd, adlib_data[0xbd] & ~(1 << 5));
                                }
                            }
                            break;
                        }
                        break;
                    case 0xc0: // patch change
                        x = getNext(1);
                        ch[c].inum = x;
                        for (int i = 0; i < 11; ++i) {
                            ch[c].ins[i] = myinsbank[ch[c].inum][i];
                        }
                        break;
                    case 0xd0: // channel touch
                        x = getNext(1);
                        break;
                    case 0xe0: // pitch wheel
                        x = getNext(1);
                        x = getNext(1);
                        break;
                    case 0xf0:
                        int l;
                        switch (v) {
                        case 0xf0:
                        case 0xf7: // sysex
                            boolean f = false;
                            l = getVal();
                            if (lookData(pos + l) == 0xf7) {
                                f = true;
                            }

                            logger.fine(String.format("{%d}", l));
                            logger.fine("\n");
                            if (lookData(pos) == 0x7d && lookData(pos + 1) == 0x10 && lookData(pos + 2) < 16) {
                                adlib_style = LUCAS_STYLE | MIDI_STYLE;

                                for (int i = 0; i < l; ++i) {
                                    logger.fine(String.format("%x ", lookData(pos + i)));
                                    if ((i - 3) % 10 == 0) {
                                        logger.fine("\n");
                                    }
                                }

                                logger.fine("\n");
                                getNext(1);
                                getNext(1);
                                c = getNext(1);
                                getNext(1);
                                ch[c].ins[0] = (getNext(1) << 4) + getNext(1);
                                ch[c].ins[2] = 0xff - ((getNext(1) << 4) + getNext(1) & 0x3f);
                                ch[c].ins[4] = 0xff - ((getNext(1) << 4) + getNext(1));
                                ch[c].ins[6] = 0xff - ((getNext(1) << 4) + getNext(1));
                                ch[c].ins[8] = (getNext(1) << 4) + getNext(1);
                                ch[c].ins[1] = (getNext(1) << 4) + getNext(1);
                                ch[c].ins[3] = 0xff - ((getNext(1) << 4) + getNext(1) & 0x3f);
                                ch[c].ins[5] = 0xff - ((getNext(1) << 4) + getNext(1));
                                ch[c].ins[7] = 0xff - ((getNext(1) << 4) + getNext(1));
                                ch[c].ins[9] = (getNext(1) << 4) + getNext(1);
                                ch[c].ins[10] = (getNext(1) << 4) + getNext(1);
                                logger.fine(String.format("\n%d: ", c));

                                for (int i = 0; i < 11; ++i) {
                                    logger.fine(String.format("%2X ", ch[c].ins[i]));
                                }

                                getNext(l - 26);
                            } else {
                                logger.fine(String.format("\n", new Object[0]));

                                for (int i = 0; i < l; ++i) {
                                    logger.fine(String.format("%2X ", getNext(1)));
                                }
                            }

                            logger.fine(String.format("\n", new Object[0]));
                            if (f) {
                                getNext(1);
                            }
                            break;
                        case 0xf1:
                        case 0xf4:
                        case 0xf5:
                        case 0xfd:
                        case 0xfe:
                        default:
                            break;
                        case 0xf2:
                            getNext(2);
                            break;
                        case 0xf3:
                            getNext(1);
                            break;
                        case 0xf6: // something
                        case 0xf8:
                        case 0xfa:
                        case 0xfb:
                        case 0xfc:
                            // this ends the track for sierra.
                            if (type == FILE_SIERRA || type == FILE_ADVSIERRA) {
                                track[curtrack].tend = pos;
                                logger.fine(String.format("endmark: %d -- %x\n", pos, pos));
                            }
                            break;
                        case 0xff:
                            v = getNext(1);
                            l = getVal();
                            logger.fine("\n");
                            logger.fine(String.format("{%X_%X}", v, l));
                            if (v == 0x51) {
                                msqtr = getNext(l); // set tempo
                                logger.fine(String.format("(qtr=%d)", msqtr));
                            } else {
                                for (int i = 0; i < l; ++i) {
                                    logger.fine(String.format("%2X ", getNext(1)));
                                }
                            }
                            break;
                        }
                        break;
                    default:
                        // if we get down here, a error occurred
                        logger.fine(String.format("!", v));
                        break;
                    }

                    if (pos < track[curtrack].tend) {
                        int w;
                        if (type != FILE_SIERRA && type != FILE_ADVSIERRA) {
                            w = getVal();
                        } else {
                            w = getNext(1);
                        }

                        track[curtrack].iwait = w;
                    } else {
                        track[curtrack].iwait = 0;
                    }

                    track[curtrack].pos = pos;
                }
            }

            ret = false; // end of song.
            iwait = 0;

            for (curtrack = 0; curtrack < 16; ++curtrack) {
                if (track[curtrack].on == 1 && track[curtrack].pos < track[curtrack].tend) {
                    ret = true; // not yet...
                }
            }

            if (ret) {
                iwait = 0xffffff; // bigger than any wait can be!

                for (curtrack = 0; curtrack < 16; ++curtrack) {
                    if (track[curtrack].on == 1 && track[curtrack].pos < track[curtrack].tend && track[curtrack].iwait < iwait) {
                        iwait = track[curtrack].iwait;
                    }
                }
            }
        }

        if (iwait != 0 && ret) {
            for (curtrack = 0; curtrack < 16; ++curtrack) {
                if (track[curtrack].on != 0) {
                    track[curtrack].iwait -= iwait;
                }
            }

            fwait = 1.0F / ((float) iwait / (float) deltas * (msqtr / 1000000.0F));
        } else {
            fwait = 50.0F; // 1/50th of a second
        }

        logger.fine("\n");

        for (int i = 0; i < 16; ++i) {
            if (track[i].on != 0) {
                if (track[i].pos < track[i].tend) {
                    logger.fine(String.format("<%d>", track[i].iwait));
                } else {
                    logger.fine("stop");
                }
            }
        }

        return ret;
    }

    public float getRefresh() {
        return fwait > 0.01F ? fwait : 0.01F;
    }

    public void rewind(int subSong) {
        int[] ins = new int[16];
        pos = 0;
        tins = 0;
        adlib_style = MIDI_STYLE | CMF_STYLE;
        adlib_mode = ADLIB_MELODIC;

        for (int i = 0; i < 128; ++i) {
            for (int j = 0; j < 14; ++j) {
                myinsbank[i][j] = midi_fm_instruments[i][j];
            }

            myinsbank[i][14] = 0;
            myinsbank[i][15] = 0;
        }

        for (int c = 0; c < 16; ++c) {
            ch[c].inum = 0;

            for (int i = 0; i < 11; ++i) {
                ch[c].ins[i] = myinsbank[ch[c].inum][i];
            }

            ch[c].vol = 127;
            ch[c].nshift = -25;
            ch[c].on = 1;
        }

        for (int i = 0; i < 9; ++i) {
            chp[i][0] = -1;
            chp[i][2] = 0;
        }

        deltas = 250; // just a number, not a standard
        msqtr = 500000;
        fwait = 123.0F; // gotta be a small thing... sorta like nothing
        iwait = 0;
        subsongs = 1;

        for (int i = 0; i < 16; ++i) {
            track[i].tend = 0;
            track[i].spos = 0;
            track[i].pos = 0;
            track[i].iwait = 0;
            track[i].on = 0;
            track[i].pv = 0;
        }

        curtrack = 0;
        pos = 0;
        int v = getNext(1);
        switch (type) {
        case FILE_LUCAS:
            getNext(24); //skip junk and get to the midi.
            adlib_style = LUCAS_STYLE | MIDI_STYLE;
            // note: no break, we go right into midi headers...
        case FILE_MIDI:
            if (type != FILE_LUCAS) {
                tins = 128;
            }
            getNext(11); // skip header
            deltas = getNext(2);
            logger.fine(String.format("deltas:%d\n", deltas));
            getNext(4);

            curtrack = 0;
            track[curtrack].on = 1;
            track[curtrack].tend = getNext(4);
            track[curtrack].spos = pos;
            logger.fine(String.format("tracklen:%d\n", track[curtrack].tend));
            break;
        case FILE_CMF:
            getNext(3); // ctmf
            getNextI(2); // version
            int n = getNextI(2); // instrument offset
            int m = getNextI(2); // music offset
            deltas = getNextI(2);  //ticks/qtr note
            //the stuff in the cmf is click ticks per second...
            msqtr = 1000000 / getNextI(2) * deltas;

            v = getNextI(2);
            if (v != 0) {
                title = new String(data, v, strlen(data, v));
            }

            v = getNextI(2);
            if (v != 0) {
                author = new String(data, v, strlen(data, v));
            }

            v = getNextI(2);
            if (v != 0) {
                remarks = new String(data, v, strlen(data, v));
            }

            getNext(16); // channel in use table...
            v = getNextI(2); // num instr
            if (v > 128) { // to ward of bad numbers...
                v = 128;
            }
            getNextI(2); //basic tempo

            logger.fine(String.format("\nioff:%d\nmoff%d\ndeltas:%d\nmsqtr:%d\nnumi:%d\n", n, m, deltas, msqtr, v));
            pos = n; // jump to instruments
            tins = v;

            for (int i = 0; i < v; ++i) {
                logger.fine(String.format("\n%d: ", i));

                for (int j = 0; j < 16; ++j) {
                    myinsbank[i][j] = getNext(1);
                    logger.fine(String.format("%2X ", myinsbank[i][j]));
                }
            }

            for (int i = 0; i < 16; ++i) {
                ch[i].nshift = -13;
            }

            adlib_style = CMF_STYLE;
            curtrack = 0;
            track[curtrack].on = 1;
            track[curtrack].tend = flen; // music until the end of the file
            track[curtrack].spos = m; // jump to midi music
            break;
        case FILE_SIERRA:
            myinsbank = smyinsbank.clone();
            tins = stins;
            getNext(2);
            deltas = 32;
            curtrack = 0;
            track[curtrack].on = 1;
            track[curtrack].tend = flen; // music until the end of the file

            for (int i = 0; i < 16; ++i) {
                ch[i].nshift = -13;
                ch[i].on = getNext(1);
                ch[i].inum = getNext(1);

                for (int j = 0; j < 11; ++j) {
                    ch[i].ins[j] = myinsbank[ch[i].inum][j];
                }
            }

            track[curtrack].spos = pos;
            adlib_style = SIERRA_STYLE | MIDI_STYLE;
            break;
        case FILE_ADVSIERRA:
            myinsbank = smyinsbank.clone();
            tins = stins;
            deltas = 32;
            getNext(11); // worthless empty space and "stuff" :)
            int o_sierra_pos = sierra_pos = pos;
            sierra_next_section();

            while (lookData(sierra_pos - 2) != 255) {
                sierra_next_section();
                ++subsongs;
            }

            if (subSong < 0 || subSong >= subsongs) {
                subSong = 0;
            }

            sierra_pos = o_sierra_pos;
            sierra_next_section();

            for (int i = 0; i != subSong; ++i) {
                sierra_next_section();
            }

            adlib_style = SIERRA_STYLE | MIDI_STYLE; // advanced sierra tunes use volume;
            break;
        case FILE_OLDLUCAS:
            msqtr = 250000;
            pos = 9;
            deltas = getNext(1);
            v = 8;
            pos = 0x19; // jump to instruments
            tins = v;

            for (int i = 0; i < v; ++i) {
                logger.fine(String.format("\n%d: ", i));

                for (int j = 0; j < 16; ++j) {
                    ins[j] = getNext(1);
                }

                myinsbank[i][10] = ins[2];
                myinsbank[i][0] = ins[3];
                myinsbank[i][2] = ins[4];
                myinsbank[i][4] = ins[5];
                myinsbank[i][6] = ins[6];
                myinsbank[i][8] = ins[7];
                myinsbank[i][1] = ins[8];
                myinsbank[i][3] = ins[9];
                myinsbank[i][5] = ins[10];
                myinsbank[i][7] = ins[11];
                myinsbank[i][9] = ins[12];

                for (int j = 0; j < 11; ++j) {
                    logger.fine(String.format("%2X ", myinsbank[i][j]));
                }
            }

            for (int i = 0; i < 16; ++i) {
                if (i < tins) {
                    ch[i].inum = i;

                    for (int j = 0; j < 11; ++j) {
                        ch[i].ins[j] = myinsbank[ch[i].inum][j];
                    }
                }
            }

            adlib_style = LUCAS_STYLE | MIDI_STYLE;
            curtrack = 0;
            track[curtrack].on = 1;
            track[curtrack].tend = flen; // music until the end of the file
            track[curtrack].spos = 0x98; // jump to midi music
            break;
        }

        for (int i = 0; i < 16; ++i) {
            if (track[i].on != 0) {
                track[i].pos = track[i].spos;
                track[i].pv = 0;
                track[i].iwait = 0;
            }
        }

        doing = 1;
        midi_fm_reset();
    }

    String getType() {
        switch (type) {
        case 1:
            return "LucasArts AdLib MIDI";
        case 2:
            return "General MIDI";
        case 3:
            return "Creative Music Format (CMF MIDI)";
        case 4:
            return "Sierra On-Line EGA MIDI";
        case 5:
            return "Sierra On-Line VGA MIDI";
        case 6:
            return "Lucasfilm Adlib MIDI";
        default:
            return "MIDI unknown";
        }
    }

    private int strlen(byte[] buf, int pos) {
        for (int i = pos; i < buf.length; i++) {
            if (buf[i] == 0) {
                return i - pos;
            }
        }
        return buf.length - pos;
    }
}
