/*
 *  Copyright (c) 1999, 2000 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import static java.lang.System.getLogger;


/*        +DocBookXML
        <title>Getting information about an audio file</title>

        <formalpara><title>Purpose</title>
        <para>Displays general information about an audio file: file type,
        format of audio data, length of audio data, total length of the
        file.</para>
        </formalpara>

        <formalpara><title>Level</title>
        <para>newbie</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis>
        <command>java AudioFileInfo</command>
        <group>
        <arg><option>-f</option></arg>
        <arg><option>-u</option></arg>
        <arg><option>-s</option></arg>
        </group>
        <arg><option>-i</option></arg>
        <arg><replaceable class="parameter">audiofile</replaceable></arg>
        </cmdsynopsis>
        </para>
        </formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><option>-s</option></term>
        <listitem><para>use standard input as source for the audio file. If this option is given, <replaceable class="parameter">audiofile</replaceable> is not required.</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-f</option></term>
        <listitem><para>interpret <replaceable class="parameter">audiofile</replaceable> as filename. If this option is given, <replaceable class="parameter">audiofile</replaceable> is required.</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-u</option></term>
        <listitem><para>interpret <replaceable class="parameter">audiofile</replaceable> as URL. If this option is given, <replaceable class="parameter">audiofile</replaceable> is required.</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-i</option></term>
        <listitem><para>display additional information</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><replaceable class="parameter">audiofile</replaceable></term>
        <listitem><para>the file name  or URL of the audio
        file that information should be displayed for. This is required if
        <option>-s</option> is not given.</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>
        Some combination of options do not work. Both Sun's
        implementation and <ulink url="http://www.tritonus.org/">Tritonus</ulink> show some
        information only with option <option>-i</option>.
        </para></formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="AudioFileInfo.java.html">AudioFileInfo.java</ulink>
        </para></formalpara>

-DocBookXML
*/

/**
 * AudioFileInfo.java
 * <p>
 * This file is part of the Java Sound Examples.
 */
public class AudioFileInfo {

    private static final Logger logger = getLogger(AudioFileInfo.class.getName());

    private static final int LOAD_METHOD_STREAM = 1;
    private static final int LOAD_METHOD_FILE = 2;
    private static final int LOAD_METHOD_URL = 3;

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsageAndExit();
        }

        int nLoadMethod = LOAD_METHOD_FILE;
        boolean bCheckAudioInputStream = false;
        int nCurrentArg = 0;
        while (nCurrentArg < args.length) {
            switch (args[nCurrentArg]) {
                case "-h" -> printUsageAndExit();
                case "-s" -> nLoadMethod = LOAD_METHOD_STREAM;
                case "-f" -> nLoadMethod = LOAD_METHOD_FILE;
                case "-u" -> nLoadMethod = LOAD_METHOD_URL;
                case "-i" -> bCheckAudioInputStream = true;
            }

            nCurrentArg++;
        }

        String strSource = args[nCurrentArg - 1];
        String strFilename = null;
        AudioFileFormat aff = null;
        AudioInputStream ais = null;
        try {
            switch (nLoadMethod) {
                case LOAD_METHOD_STREAM:

                    InputStream inputStream = System.in;
                    aff = AudioSystem.getAudioFileFormat(inputStream);
                    strFilename = "<standard input>";
                    if (bCheckAudioInputStream) {
                        ais = AudioSystem.getAudioInputStream(inputStream);
                    }
                    break;
                case LOAD_METHOD_FILE:

                    File file = new File(strSource);
                    aff = AudioSystem.getAudioFileFormat(file);
                    strFilename = file.getCanonicalPath();
                    if (bCheckAudioInputStream) {
                        ais = AudioSystem.getAudioInputStream(file);
                    }
                    break;
                case LOAD_METHOD_URL:

                    URL url = new URL(strSource);
                    aff = AudioSystem.getAudioFileFormat(url);
                    strFilename = url.toString();
                    if (bCheckAudioInputStream) {
                        ais = AudioSystem.getAudioInputStream(url);
                    }
                    break;
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            System.exit(1);
        }
        if (aff == null) {
            System.out.println("Cannot determine format");
        } else {
            AudioFormat format = aff.getFormat();
            System.out.println("---------------------------------------------------------------------------");
            System.out.println("Source: " + strFilename);
            System.out.println("Type: " + aff.getType());
            System.out.println("AudioFormat: " + format);
            System.out.println("---------------------------------------------------------------------------");

            String strAudioLength = null;
            if (aff.getFrameLength() != AudioSystem.NOT_SPECIFIED) {
                strAudioLength = aff.getFrameLength() + " frames (= " +
                        (aff.getFrameLength() * format.getFrameSize()) + " bytes)";
            } else {
                strAudioLength = "unknown";
            }
            System.out.println("Length of audio data: " + strAudioLength);

            String strFileLength = null;
            if (aff.getByteLength() != AudioSystem.NOT_SPECIFIED) {
                strFileLength = aff.getByteLength() + " bytes)";
            } else {
                strFileLength = "unknown";
            }
            System.out.println("Total length of file (including headers): " + strFileLength);
            if (bCheckAudioInputStream) {
                System.out.println("[AudioInputStream says:] Length of audio data: " +
                        ais.getFrameLength() + " frames (= " +
                        (ais.getFrameLength() * ais.getFormat() .getFrameSize()) + " bytes)");
            }
            System.out.println("---------------------------------------------------------------------------");
        }
    }

    private static void printUsageAndExit() {
        System.out.println("AudioFileInfo: usage:");
        System.out.println("\tjava AudioFileInfo [-s|-f|-u] [-i] <audiofile>");
        System.exit(1);
    }
}
