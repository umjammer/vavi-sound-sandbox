package jse;
/*
 *        CaptureStreamRecorder.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;


public class CaptureStreamRecorder {
    /*
     *        Codes for the audio quality that should be used
     *        in recording.
     */
    private static final int QUALITY_NONE = 0;
    private static final int QUALITY_PHONE = 1;
    private static final int QUALITY_RADIO = 2;
    private static final int QUALITY_CD = 3;
    private static final int QUALITY_DEFAULT = 3;
    private TargetDataLine m_line;
    private OutputStream m_outputStream;
    private boolean m_bRecording;

    public static void main(String[] args) {
        int nQuality = QUALITY_DEFAULT;
        boolean bCheckAudioInputStream = false;
        int nCurrentArg = 0;
        while (nCurrentArg < (args.length - 1)) {
            if (args[nCurrentArg].equals("-p")) {
                nQuality = QUALITY_PHONE;
            } else if (args[nCurrentArg].equals("-r")) {
                nQuality = QUALITY_RADIO;
            } else if (args[nCurrentArg].equals("-c")) {
                nQuality = QUALITY_CD;
            }

/*        TODO: option to override the file format
                        else if (args[nCurrentArg].equals("-i"))
                        {
                                bCheckAudioInputStream = true;
                        }
*/
            nCurrentArg++;
        }

        String strFilename = args[nCurrentArg];
        AudioFormat format = null;
        switch (nQuality) {
        case QUALITY_PHONE:

            /*
             *        8 kHz, 8 bit, mono
             */
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000.0F,
                                     8, 1, 1, 8000.0F, true);
            break;
        case QUALITY_RADIO:

            /*
             *        22.05 kHz, 8 bit, mono
             */
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 22050.0F,
                                     8, 1, 1, 22050.0F, true);
            break;
        case QUALITY_CD:default:

            /*
             *        44.1 kHz, 16 bit, stereo
             */
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0F,
                                     16, 2, 4, 44100.0F, true);
            break;
        }

        /*
         *        We try to use a file format according to the extension
         *        of the filename the user specified. If this is not
         *        possible, we default to the .au file format.
         */
        AudioFileFormat.Type fileType = AudioFileFormat.Type.AU;
        AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();
        for (int i = 0; i < types.length; i++) {
            if (strFilename.toLowerCase().endsWith(types[i].getExtension())) {
                fileType = types[i];
            }
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        CaptureStream captureStream = null;
        try {
            captureStream = new CaptureStream(format, byteArrayOutputStream);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        System.out.println("Press ENTER to start the recording.");
        try {
            System.in.read();
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        captureStream.start();
        System.out.println("Recording...");
        System.out.println("Press ENTER to stop the recording.");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        captureStream.stopRecording();
        System.out.println("Recording stopped.");

        // TODO: flush() ??

        /*
         *        We close the ByteArrayOutputStream
         */
        try {
            byteArrayOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] abData = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(abData);

        AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream,
                                                                 format,
                                                                 abData.length / format.getFrameSize());

/*                AudioFileFormat        audioFileFormat = new AudioFileFormat(fileType, format, abData.length / format.getFrameSize());
 */
        try {
            AudioSystem.write(audioInputStream, fileType, new File(strFilename));
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         *        We're done.
         */
        System.exit(0);
    }
}

/* */
