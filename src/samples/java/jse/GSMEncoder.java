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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.tritonus.share.sampled.AudioFileTypes;
import org.tritonus.share.sampled.Encodings;

import static java.lang.System.getLogger;


/*        +DocBookXML
        <title>Encoding an audio file to GSM 06.10</title>

        <formalpara><title>Purpose</title>
        <para>
        Encodes a PCM audio file, writes the result as a
        GSM 06.10 file.
        </para></formalpara>

        <formalpara><title>Level</title>
        <para>advanced</para></formalpara>

        <formalpara>
        <title>Usage</title>
        <para>
        <cmdsynopsis>
        <command>java GSMEncoder</command>
        <arg choice="plain"><replaceable class="parameter">pcmfile</replaceable></arg>
        <arg choice="plain"><replaceable class="parameter">gsmfile</replaceable></arg>
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
        <term><option><replaceable class="parameter">gsmfile</replaceable></option></term>
        <listitem><para>the name of the GSM output file.</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>
        The input file has to be 8 kHz, 16 bit linear signed, mono.
        GSM 06.10 can only be handled natively
        by Tritonus. If you want to use this format with the
        Sun jdk1.3, you have to install the respective plug-in
        from <ulink url
        ="http://www.tritonus.org/plugins.html">Tritonus
        Plug-ins</ulink>.
        </para>
        </formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="GSMEncoder.java.html">GSMEncoder.java</ulink>
        </para>
        </formalpara>

-DocBookXML
*/

/**
 * GSMEncoder.java
 *
 * This file is part of the Java Sound Examples.
 * TODO: try a single conversion to 8kHz, 16 bit linear signed, mono
 */
public class GSMEncoder {

    private static final Logger logger = getLogger(GSMEncoder.class.getName());

    public static void main(String[] args) {
        if (args.length != 2) {
            printUsageAndExit();
        }

        File pcmFile = new File(args[0]);
        File gsmFile = new File(args[1]);
        AudioInputStream ais = null;
        try {
            ais = AudioSystem.getAudioInputStream(pcmFile);
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
        if (ais == null) {
            System.out.println("cannot open audio file");
            System.exit(1);
        }

        AudioFormat.Encoding targetEncoding = Encodings.getEncoding("GSM0610");
        AudioInputStream gsmAIS = AudioSystem.getAudioInputStream(targetEncoding,
                                                                  ais);
        AudioFileFormat.Type fileType = AudioFileTypes.getType("GSM", ".gsm");
        int nWrittenFrames = 0;
        try {
            nWrittenFrames = AudioSystem.write(gsmAIS, fileType, gsmFile);
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    private static void printUsageAndExit() {
        System.out.println("GSMEncoder: usage:");
        System.out.println("\tjava GSMEncoder <pcmfile> <gsmfile>");
        System.exit(1);
    }
}
