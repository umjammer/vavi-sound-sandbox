//
//  sf2convert
//  SoundFont Conversion/Compression Utility
//
//  Copyright (C)
//  2010 Werner Schweer and others (MuseScore)
//  2015 Davy Triponney (Polyphone)
//  2017 Cognitone (Juce port, converter)
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License version 2.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//

package vavi.sound.sf;

import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vavi.sound.sf.SFont.FileType;
import vavi.sound.sf.SFont.SoundFont;

import static java.lang.System.getLogger;


/**
 * SoundFont Conversion/Compression Utility.
 *
 * @author Werner Schweer and others (MuseScore)
 * @author Davy Triponney (Polyphone)
 * @author Cognitone (Juce port, converter)
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2025/04/25 umjammer port to java <br>
 * @see "https://github.com/git-moss/ConvertWithMoss"
 */
class Convert {

    private static final Logger logger = getLogger(Convert.class.getName());

    /**
     * usage
     */
    static void usage() {
        System.err.println("sf2convert - SoundFont Compression Utility, 2017 Cognitone");
        System.err.printf("usage: %s [-flags] infile outfile\n", "Convert");
        System.err.println("flags:");
        System.err.println("   -zf    compress source file using FLAC (SF4 format)");
        System.err.println("   -zf0   ditto w/quality=low");
        System.err.println("   -zf1   ditto w/quality=medium");
        System.err.println("   -zf2   ditto w/quality=high (default)");

        System.err.println("   -zo    compress source file using Ogg Vorbis (SF3 format)");
        System.err.println("   -zo0   ditto w/quality=low");
        System.err.println("   -zo1   ditto w/quality=medium");
        System.err.println("   -zo2   ditto w/quality=high (default)");

        System.err.println("   -x     expand source file to SF2 format");
        System.err.println("   -d     dump presets");
    }

    /**
     * main
     */
    public static void main(String[] args) throws Exception {
        boolean dump = false;
        boolean convert = false;
        FileType format = FileType.SF2Format;
        int quality = 2;
        boolean any = false;

        List<String> commandLine = new ArrayList<>(Arrays.asList(args));
        // Lacking getopt() on Windows, this is a quick & simple hack to pasre command line options
        while (commandLine.size() > 2) {
            String token = commandLine.getFirst();
            if (token.startsWith("-")) {
                if (token.indexOf('x') > 0) {
                    convert = true;
                    format = FileType.SF2Format;
                    any = true;
                }
                if (token.indexOf('z') > 0) {
                    convert = true;
                    format = FileType.SF3Format;
                    any = true;
                }
                if (token.indexOf('o') > 0) {
                    convert = true;
                    format = FileType.SF3Format;
                    any = true;
                }
                if (token.indexOf('f') > 0) {
                    convert = true;
                    format = FileType.SF4Format;
                    any = true;
                }
                if (token.indexOf('d') > 0) {
                    dump = true;
                    any = true;
                }
                if (token.indexOf('0') > 0) {
                    quality = 0;
                }
                if (token.indexOf('1') > 0) {
                    quality = 1;
                }
                if (token.indexOf('2') > 0) {
                    quality = 2;
                }
                //DBG(token);
                commandLine.removeFirst();
            }
        }

        if (commandLine.size() != 2 && !any) {
            usage();
            System.exit(1);
        }

        File inFilename = new File(commandLine.get(0));
        File outFilename = new File(commandLine.get(1));

        SoundFont sf = new SoundFont(inFilename);
        logger.log(Level.DEBUG, "Reading " + inFilename.getAbsolutePath());

        if (!sf.read()) {
            logger.log(Level.DEBUG, "Error reading file");
            return;
        }

        if (dump)
            sf.dumpPresets();

        if (convert) {
            logger.log(Level.DEBUG, "Writing " + outFilename.getAbsolutePath());
            sf.write(outFilename, format, quality);
        }
    }
}
