package jse;
/*
 *        AudioDecoder.java
 *
 *        This file is part of the Java Sound Examples.
 */
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
import java.io.IOException;
import java.io.File;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


/*        +DocBookXML
        <title>Decoding an encoded audio file</title>

        <formalpara><title>Purpose</title>
        <para>
        Decodes an encoded audio file, writes the result as a
        PCM file.
        </para></formalpara>

        <formalpara><title>Level</title>
        <para>newbie</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis>
        <command>java AudioDecoder</command>
        <arg choice="plain"><replaceable class="parameter">encodedfile</replaceable></arg>
        <arg choice="plain"><replaceable class="parameter">pcmfile</replaceable></arg>
        </cmdsynopsis>
        </para>
        </formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><option><replaceable class="parameter">encodedfile</replaceable></option></term>
        <listitem><para>the name of the encoded input file.</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option><replaceable class="parameter">pcmfile</replaceable></option></term>
        <listitem><para>the name of the PCM output file.</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>The output file type and audio format can't be selected.
        </para>
        <para>
        Compressed formats can be handled depending on the
        capabilities of the Java Sound implementation it is run
        with.  A-law and &mu;-law can be handled in any known Java
        Sound implementation.  mp3 and GSM 06.10 can be handled
        by Tritonus. If you want to play these formats with the
        Sun jdk1.3, you have to install the respective plug-ins
        from <ulink url
        ="http://www.tritonus.org/plugins.html">Tritonus
        Plug-ins</ulink>.
        </para></formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="AudioDecoder.java.html">AudioDecoder.java</ulink>
        </para>
        </formalpara>

-DocBookXML
*/
public class AudioDecoder {
    public static void main(String[] args) {
        if (args.length != 2) {
            printUsageAndExit();
        }

        File encodedFile = new File(args[0]);
        File pcmFile = new File(args[1]);
        AudioInputStream ais = null;
        try {
            ais = AudioSystem.getAudioInputStream(encodedFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ais == null) {
            System.out.println("cannot open audio file");
            System.exit(1);
        }

        AudioFormat.Encoding targetEncoding = AudioFormat.Encoding.PCM_SIGNED;
        AudioInputStream pcmAIS = AudioSystem.getAudioInputStream(targetEncoding,
                                                                  ais);
        AudioFileFormat.Type fileType = AudioFileFormat.Type.AU;
        int nWrittenFrames = 0;
        try {
            nWrittenFrames = AudioSystem.write(pcmAIS, fileType, pcmFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printUsageAndExit() {
        System.out.println("AudioDecoder: usage:");
        System.out.println("\tjava AudioDecoder <encodedfile> <pcmfile>");
        System.exit(1);
    }
}

/* */
