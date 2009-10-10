/*
 *  Copyright (c) 1999, 2000 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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

package jse;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/*
 * +DocBookXML
 * <title>Encoding an audio file to &mu;-law</title>
 * 
 * <formalpara><title>Purpose</title>
        <para>Encodes a PCM audio file, writes the result as an
        <filename>.au</filename>-file, &mu;-law encoded.
        </para></formalpara>

        <formalpara><title>Level</title>
        <para>newbie</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis>
        <command>java UlawEncoder</command>
        <arg choice="plain"><replaceable class="parameter">pcmfile</replaceable></arg>
        <arg choice="plain"><replaceable class="parameter">ulawfile</replaceable></arg>
        </cmdsynopsis>
        </para>
        </formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><option><replaceable class="parameter">pcmfile</replaceable></option></term>
        <listitem><para>the name of the PCM input file.</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option><replaceable class="parameter">ulawfile</replaceable></option></term>
        <listitem><para>the name of the &mu;-law output file.</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>Does not work with the Sun jdk1.3 (see bug #4391108).
        According to Florian, this bug is fixed in jdk1.4.</para>
        </formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="UlawEncoder.java.html">UlawEncoder.java</ulink>
        </para>
        </formalpara>

-DocBookXML
*/

/**
 * UlawEncoder.
 *
 * This file is part of the Java Sound Examples.
 */
public class UlawEncoder {
    public static void main(String[] args) {
        if (args.length != 2) {
            printUsageAndExit();
        }

        File pcmFile = new File(args[0]);
        File ulawFile = new File(args[1]);
        AudioInputStream ais = null;
        try {
            ais = AudioSystem.getAudioInputStream(pcmFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ais == null) {
            System.out.println("cannot open audio file");
            System.exit(1);
        }

        AudioFormat.Encoding targetEncoding = AudioFormat.Encoding.ULAW;
        AudioInputStream ulawAudioInputStreamAIS = AudioSystem.getAudioInputStream(targetEncoding, ais);
        AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
        int nWrittenFrames = 0;
        try {
            nWrittenFrames = AudioSystem.write(ulawAudioInputStreamAIS, fileType, ulawFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printUsageAndExit() {
        System.out.println("UlawEncoder: usage:");
        System.out.println("\tjava UlawEncoder <pcmfile> <ulawfile>");
        System.exit(1);
    }
}
