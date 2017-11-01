package jse;
/*
 *        CddaRipper.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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
 */
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.tritonus.sampled.cdda.CddaURLStreamHandlerFactory;


/**
  command-line extractor/player
 */
public class CddaRipper {
    static {
        URL.setURLStreamHandlerFactory(new CddaURLStreamHandlerFactory());
    }

    /**        Flag for debugging messages.
     *        If true, some messages are dumped to the console
     *        during operation.
     */
    private static boolean DEBUG = false;

    public static void main(String[] args) {
        /**        CDROM drive number.
                Defaults to first drive. [how is the order constituted?]
                Not used for now. Hardcoded default to /dev/cdrom.
        */
        int nDrive = 0;

        boolean bTocOnly = true;
        int nTrack = 0;
        File outputFile = new File("output.wav");

        if (args.length < 1) {
            bTocOnly = true;
        } else if (args.length == 1) {
            nTrack = Integer.parseInt(args[0]);
            bTocOnly = false;
        }

        // TODO: should not be hardcoded
        String strDrive = "/dev/cdrom";

        if (bTocOnly) {
            URL tocURL = null;
            try {
                tocURL = new URL("cdda:" + strDrive);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            InputStream tocInputStream = null;
            try {
                tocInputStream = tocURL.openStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            output(tocInputStream);
        }

//         System.out.println("First track: " + anValues[0]);
//         System.out.println("last track: " + anValues[1]);
//         int    nTracks = anValues[1] - anValues[0] + 1;
//         for (int i = 0; i < nTracks; i++)
//         {
//             System.out.println("Track " + (i + anValues[0]) + " start: " + anStart[i]);
//             System.out.println("Track " + (i + anValues[0]) + " type: " + anType[i]);
//             System.out.println("Track " + (i + anValues[0]) + " length (s): " + (anStart[i + 1] - anStart[i])/75);
//         }
        if (!bTocOnly) {
            URL trackURL = null;
            try {
                trackURL = new URL("cdda://" + strDrive + "#" + nTrack);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            InputStream trackInputStream = null;
            try {
                trackInputStream = trackURL.openStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            AudioInputStream audioInputStream = (AudioInputStream) trackInputStream;

            try {
                if (DEBUG) {
                    out("before write()");
                }

                int nWritten = AudioSystem.write(audioInputStream,
                                                 AudioFileFormat.Type.WAVE,
                                                 outputFile);
                if (DEBUG) {
                    out("after write()");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void output(InputStream inputStream) {
        byte[] buffer = new byte[4096];
        OutputStream outputStream = System.out;

        try {
            int nBytesRead = 0;
            nBytesRead = inputStream.read(buffer);
            while (nBytesRead >= 0) {
                outputStream.write(buffer, 0, nBytesRead);
                nBytesRead = inputStream.read(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}

/* */
