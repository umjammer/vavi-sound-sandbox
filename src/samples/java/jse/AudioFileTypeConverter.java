/*
 *  Copyright (c) 1999 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package jse;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


/*
 *        AudioFileTypeConverter.java
 *
 *        This file is part of the Java Sound Examples.
 */
public class AudioFileTypeConverter {
    /**        Flag for debugging messages.
     *        If true, some messages are dumped to the console
     *        during operation.
     */
    private static final boolean DEBUG = false;

    public static void main(String[] args) {
        if (args.length == 1) {
            if (args[0].equals("-h")) {
                printUsageAndExit();
            } else if (args[0].equals("-l")) {
                listPossibleTargetTypes();
                System.exit(0);
            } else {
                printUsageAndExit();
            }
        } else if (args.length == 3) {
            if (!args[0].equals("-t")) {
                printUsageAndExit();
            }

            String strExtension = args[1];
            AudioFileFormat.Type targetFileType = findTargetType(strExtension);
            if (targetFileType == null) {
                System.out.println("Unknown target file type. Check with 'AudioFileTypeConverter -l'.");
                System.exit(1);
            }

            String strFilename = args[2];
            File file = new File(strFilename);
            AudioInputStream ais = null;
            try {
                ais = AudioSystem.getAudioInputStream(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (ais == null) {
                System.out.println("cannot open audio file");
                System.exit(1);
            }

            int nDotPos = strFilename.lastIndexOf('.');
            String strTargetFilename = null;
            if (nDotPos == -1) {
                strTargetFilename = strFilename +
                                    targetFileType.getExtension();
            } else {
                strTargetFilename = strFilename.substring(0, nDotPos) +
                                    targetFileType.getExtension();
            }
            if (DEBUG) {
                System.out.println("Target filename: " + strTargetFilename);
            }

            int nWrittenBytes = 0;
            try {
                nWrittenBytes = AudioSystem.write(ais, targetFileType,
                                                  new File(strTargetFilename));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (DEBUG) {
                System.out.println("Written bytes: " + nWrittenBytes);
            }
        } else // args.length != 3
         {
            printUsageAndExit();
        }
    }

    private static void printUsageAndExit() {
        System.out.println("AudioFileTypeConverter: usage:");
        System.out.println("\tjava AudioFileTypeConverter -l");
        System.out.println("\tjava AudioFileTypeConverter [-t <targettype>]<soundfile>");
        System.exit(0);
    }

    private static void listPossibleTargetTypes() {
        AudioFileFormat.Type[] aTypes = AudioSystem.getAudioFileTypes();
        System.out.print("Supported target types:");
        for (int i = 0; i < aTypes.length; i++) {
            System.out.print(" " + aTypes[i].getExtension());
        }
        System.out.print("\n");
    }

    private static AudioFileFormat.Type findTargetType(String strExtension) {
        AudioFileFormat.Type[] aTypes = AudioSystem.getAudioFileTypes();
        for (int i = 0; i < aTypes.length; i++) {
            if (aTypes[i].getExtension().equals(strExtension)) {
                return aTypes[i];
            }
        }
        return null;
    }
}

/* */
