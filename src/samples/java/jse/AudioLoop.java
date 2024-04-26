package jse;
/*
 *        AudioLoop.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999 - 2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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
import gnu.getopt.Getopt;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;


// TODO: params for audio quality, optionally use compression and decompression in the loop (see ~/AudioLoop.java)

/*        +DocBookXML
        <title>Recording and playing back the recorded data immediately</title>

        <formalpara><title>Purpose</title>
        <para>
        This program opens two lines: one for recording and one
        for playback. In an infinite loop, it reads data from
        the recording line and writes them to the playback line.
        You can use this to measure the delays inside Java Sound:
        Speak into the microphone and wait untill you hear
        yourself in the speakers.  This can be used to
        experience the effect of changing the buffer sizes: use
        the <option>-e</option> and <option>-i</option> options.
        You will notice that the
        delays change, too.
        </para></formalpara>

        <formalpara><title>Level</title>
        <para>advanced</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis>
        <command>java AudioLoop</command>
        <arg choice="plain"><option>-l</option></arg>
        </cmdsynopsis>
        <cmdsynopsis>
        <command>java AudioLoop</command>
        <arg><option>-M <replaceable>mixername</replaceable></option></arg>
        <arg><option>-e <replaceable>buffersize</replaceable></option></arg>
        <arg><option>-i <replaceable>buffersize</replaceable></option></arg>
        </cmdsynopsis>
        </para></formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><option>-l</option></term>
        <listitem><para>lists the available mixers</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-M <replaceable>mixername</replaceable></option></term>
        <listitem><para>selects a mixer to play on</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-e <replaceable>buffersize</replaceable></option></term>
        <listitem><para>the buffer size to use in the application ("extern")</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-i <replaceable>buffersize</replaceable></option></term>
        <listitem><para>the buffer size to use in Java Sound ("intern")</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>
        There is no way to stop the program besides brute force
        (ctrl-C). There is no way to set the audio quality.
        </para>

        <para>The example requires that the soundcard and its driver
        as well as the Java Sound implementation support full-duplex
        operation. In Linux either use <ulink
        url="http://www.tritonus.org/">Tritonus</ulink> or enable
        full-duplex in Sun's Java Sound implementation (search the
        archive of java-linux).</para>
        </formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="AudioLoop.java.html">AudioLoop.java</ulink>,
        <ulink url="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</ulink>
        </para>
        </formalpara>

-DocBookXML
*/
public class AudioLoop extends Thread {
    /**        Flag for debugging messages.
     *        If true, some messages are dumped to the console
     *        during operation.
     */
    private static boolean DEBUG = true;
    private static final int DEFAULT_INTERNAL_BUFSIZ = 40960;
    private static final int DEFAULT_EXTERNAL_BUFSIZ = 40960;
    private TargetDataLine m_targetLine;
    private SourceDataLine m_sourceLine;
    private boolean m_bRecording;
    private int m_nExternalBufferSize;

    /*
     *        We have to pass an AudioFormat to describe in which
     *        format the audio data should be recorded and played.
     */
    public AudioLoop(AudioFormat format, int nInternalBufferSize,
                     int nExternalBufferSize, String strMixerName)
        throws LineUnavailableException {
        Mixer mixer = null;
        if (strMixerName != null) {
            Mixer.Info mixerInfo = getMixerInfo(strMixerName);
            mixer = AudioSystem.getMixer(mixerInfo);
        }

        /*
         *        We retrieve and open the recording and the playback line.
         */
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class,
                                                     format, nInternalBufferSize);
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class,
                                                     format, nInternalBufferSize);
        if (mixer != null) {
            m_targetLine = (TargetDataLine) mixer.getLine(targetInfo);
            m_sourceLine = (SourceDataLine) mixer.getLine(sourceInfo);
        } else {
            m_targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
            m_sourceLine = (SourceDataLine) AudioSystem.getLine(sourceInfo);
        }
        m_targetLine.open(format, nInternalBufferSize);
        m_sourceLine.open(format, nInternalBufferSize);
        m_nExternalBufferSize = nExternalBufferSize;
    }

    public void start() {
        m_targetLine.start();
        m_sourceLine.start();

        // start thread
        super.start();
    }

/*
  public void stopRecording()
  {
  m_line.stop();
  m_line.close();
  m_bRecording = false;
  }
*/
    public void run() {
        byte[] abBuffer = new byte[m_nExternalBufferSize];
        int nBufferSize = abBuffer.length;
        m_bRecording = true;
        while (m_bRecording) {
            if (DEBUG) {
                System.out.println("Trying to read: " + nBufferSize);
            }

            /*
             *        read a block of data from the recording line.
             */
            int nBytesRead = m_targetLine.read(abBuffer, 0, nBufferSize);
            if (DEBUG) {
                System.out.println("Read: " + nBytesRead);
            }

            /*
             *        And now, we write the block to the playback
             *        line.
             */
            m_sourceLine.write(abBuffer, 0, nBytesRead);
        }
    }

    public static void main(String[] args) {
        String strMixerName = null;
        float fFrameRate = 44100.0F;
        int nInternalBufferSize = DEFAULT_INTERNAL_BUFSIZ;
        int nExternalBufferSize = DEFAULT_EXTERNAL_BUFSIZ;

        Getopt g = new Getopt("AudioLoop", args, "hlr:i:e:M:D");
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
            case 'h':
                printUsageAndExit();
            case 'l':
                listMixersAndExit();
            case 'r':
                fFrameRate = Float.parseFloat(g.getOptarg());

/*
  if (DEBUG)
  {
  System.out.println("AudioPlayer.main(): mixer name: " + strMixerName);
  }
*/
                break;
            case 'i':
                nInternalBufferSize = Integer.parseInt(g.getOptarg());

/*
  if (DEBUG)
  {
  System.out.println("AudioPlayer.main(): mixer name: " + strMixerName);
  }
*/
                break;
            case 'e':
                nExternalBufferSize = Integer.parseInt(g.getOptarg());

/*
  if (DEBUG)
  {
  System.out.println("AudioPlayer.main(): mixer name: " + strMixerName);
  }
*/
                break;
            case 'M':
                strMixerName = g.getOptarg();
                if (DEBUG) {
                    System.out.println("AudioPlayer.main(): mixer name: " +
                                       strMixerName);
                }
                break;
            case 'D':
                DEBUG = true;
                break;
            case '?':
                printUsageAndExit();
            default:
                System.out.println("getopt() returned " + c);
                break;
            }
        }

        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                  fFrameRate, 16, 2, 4,
                                                  fFrameRate, false);
        AudioLoop audioLoop = null;
        try {
            audioLoop = new AudioLoop(audioFormat, nInternalBufferSize,
                                      nExternalBufferSize, strMixerName);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        }
        audioLoop.start();
    }

    private static void printUsageAndExit() {
        System.out.println("AudioPlayer: usage:");
        System.out.println("\tjava AudioPlayer -l");
        System.out.println("\tjava AudioPlayer [-M <mixername>] [-e <buffersize>] [-i <buffersize>]");
        System.exit(1);
    }

    private static void listMixersAndExit() {
        System.out.println("Available Mixers:");

        Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
        for (int i = 0; i < aInfos.length; i++) {
            System.out.println(aInfos[i].getName());
        }
        if (aInfos.length == 0) {
            System.out.println("[No mixers available]");
        }
        System.exit(0);
    }

    /*
     *        This method tries to return a MidiDevice.Info whose name
     *        matches the passed name. If no matching MidiDevice.Info is
     *        found, null is returned.
     */
    private static Mixer.Info getMixerInfo(String strMixerName) {
        Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
        for (int i = 0; i < aInfos.length; i++) {
            if (aInfos[i].getName().equals(strMixerName)) {
                return aInfos[i];
            }
        }
        return null;
    }
}
