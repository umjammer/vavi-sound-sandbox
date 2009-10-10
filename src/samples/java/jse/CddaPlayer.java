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

package jse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.tritonus.sampled.cdda.CddaURLStreamHandlerFactory;


/**
  command-line player
 */
public class CddaPlayer {
    static {
        URL.setURLStreamHandlerFactory(new CddaURLStreamHandlerFactory());
    }

    /**        Flag for debugging messages.
     *        If true, some messages are dumped to the console
     *        during operation.
     */
    private static boolean DEBUG = false;
    private static int DEFAULT_EXTERNAL_BUFFER_SIZE = 128000;

    public static void main(String[] args) {
        int nExternalBufferSize = DEFAULT_EXTERNAL_BUFFER_SIZE;
        int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;

        /**        CDROM drive number.
                Defaults to first drive. [how is the order constituted?]
                Not used for now. Hardcoded default to /dev/cdrom.
        */
        int nDrive = 0;

        boolean bTocOnly = true;
        int nTrack = 0;

        // File		outputFile = new File("output.wav");
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

            SourceDataLine line = null;
            AudioFormat audioFormat = audioInputStream.getFormat();
            Line.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

            try {
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open();
                line.start();
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }

            int nBytesRead = 0;
            byte[] abData = new byte[nExternalBufferSize];
            while (nBytesRead != -1) {
                try {
                    nBytesRead = audioInputStream.read(abData, 0, abData.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (DEBUG) {
                    System.out.println("AudioPlayer.main(): read from AudioInputStream (bytes): " +
                                       nBytesRead);
                }
                if (nBytesRead >= 0) {
                    int nBytesWritten = line.write(abData, 0, nBytesRead);
                    if (DEBUG) {
                        System.out.println("AudioPlayer.main(): written to SourceDataLine (bytes): " +
                                           nBytesWritten);
                    }
                }
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
