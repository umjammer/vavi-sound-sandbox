/*
 * Copyright (c) 2004 - 2006 by Matthias Pfisterer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.klab.packetcast.audio;


import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


/**
 * <titleabbrev>AmplitudeConverter</titleabbrev>
    <title>Change the amplitude (volume) of an audio file</title>

    <formalpara><title>Purpose</title>
    <para>Change the amplitude (volume) of an audio file.</para>
    </formalpara>

    <formalpara><title>Usage</title>
    <para>
    <cmdsynopsis>
    <command>java AmplitudeConverter</command>
    <arg choice="plain"><option>-h</option></arg>
    </cmdsynopsis>
    <cmdsynopsis>
    <command>java AmplitudeConverter</command>
    <group>
    <arg><option>--lin</option></arg>
    <arg><option>--log</option></arg>
    </group>
    <arg choice="plain">
    <replaceable class="parameter">amplitude</replaceable>
    </arg>
    <arg choice="plain">
    <replaceable class="parameter">sourcefile</replaceable>
    </arg>
    <arg choice="plain">
    <replaceable class="parameter">targetfile</replaceable>
    </arg>
    </cmdsynopsis>
    </para></formalpara>

    <formalpara><title>Parameters</title>
    <variablelist>
    <varlistentry>
    <term><option>-h</option></term>
    <listitem><para>displays help message</para></listitem>
    </varlistentry>
    <varlistentry>
    <term><option>--lin</option></term>
    <listitem><para>the amplitude will be given in a linear scale</para></listitem>
    </varlistentry>
    <varlistentry>
    <term><option>--log</option></term>
    <listitem><para>the amplitude will be given in a logarithmic scale</para></listitem>
    </varlistentry>
    <varlistentry>
    <term><option><replaceable class="parameter">amplitude</replaceable></option></term>
    <listitem><para>the amplitude to apply to the audio data. If <option>--lin</option> is chosen, this is a simple factor, e.g. 2.0 to double te amplitude. If <option>--log</option> is chosen, this is a value in dB, e.g. 6.0 to double the amplitude.</para></listitem>
    </varlistentry>
    <varlistentry>
    <term><replaceable class="parameter">sourcefile</replaceable></term>
    <listitem><para>the file name of the audio file that should be
    read</para></listitem>
    </varlistentry>
    <varlistentry>
    <term><replaceable class="parameter">targetfile</replaceable></term>
    <listitem><para>the file name of the audio file that should be
    written</para></listitem>
    </varlistentry>
    </variablelist>
    </formalpara>

    <formalpara><title>Bugs, limitations</title>
    <para>You need the DSP package from Tritonus to compile and run this example.</para>
    </formalpara>

    <formalpara><title>Source code</title>
    <para>
    <ulink url="AmplitudeConverter.java.html">AmplitudeConverter.java</ulink>,
    <ulink url="http://www.tritonus.org/plugins.html">Tritonus DSP package</ulink>
    </para>
    </formalpara>
*/
public class AmplitudeConverter {
    /**
     * Flag for debugging messages.
     * If true, some messages are dumped to the console
     * during operation.    
     */
//    private static final boolean DEBUG = false;

    public static void main(String[] args) throws Exception {
        boolean bAmplitudeIsLog = false;
        int nArgIndex = -1;
        if (args.length == 1) {
            if (args[0].equals("-h")) {
                printUsageAndExit();
            } else {
                printUsageAndExit();
            }
        } else if (args.length == 4) {
            if (args[0].equals("--lin")) {
                bAmplitudeIsLog = false;
            } else if (args[0].equals("--log")) {
                bAmplitudeIsLog = true;
            } else {
                printUsageAndExit();
            }
            nArgIndex = 1;
        } else if (args.length == 3) {
            nArgIndex = 0;
        } else {
            printUsageAndExit();
        }
        float fAmplitude = Float.parseFloat(args[nArgIndex + 0]);
        String strSourceFilename = args[nArgIndex + 1];
        String strTargetFilename = args[nArgIndex + 2];
        File sourceFile = new File(strSourceFilename);
        File targetFile = new File(strTargetFilename);

        AudioInputStream sourceAudioInputStream = AudioSystem.getAudioInputStream(sourceFile);
        if (sourceAudioInputStream == null) {
            out("cannot open audio file");
            System.exit(1);
        }

        AudioFileFormat aff = AudioSystem.getAudioFileFormat(sourceFile);
        AudioFileFormat.Type targetType = aff.getType();

        AmplitudeAudioInputStream amplifiedAudioInputStream = new AmplitudeAudioInputStream(sourceAudioInputStream);

        // Here, we set the desired amplification.
        if (bAmplitudeIsLog) {
            amplifiedAudioInputStream.setAmplitudeLog(fAmplitude);
        } else {
            amplifiedAudioInputStream.setAmplitudeLinear(fAmplitude);
        }

        // And finally, we are writing the amplified stream
        // to a new file.
        AudioSystem.write(amplifiedAudioInputStream, targetType, targetFile);
    }

    private static void printUsageAndExit() {
        out("AmplitudeConverter: usage:");
        out("\tjava AmplitudeConverter -h");
        out("\tjava AmplitudeConverter [--lin|--log] <amplitude> <sourcefile> <targetfile>");
        System.exit(0);
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}

