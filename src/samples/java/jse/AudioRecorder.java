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
package jse;

// IDEA: example 'recording and playback with byte arrays' (using ByteArrayIn/OutputStream)
import gnu.getopt.Getopt;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;


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
        the '-e' and '-i' options. You will notice that the
        delays change, too.
        </para></formalpara>

        <formalpara><title>Level</title>
        <para>Command-line program</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <synopsis>java AudioRecorder -l</synopsis>
        <synopsis>java AudioRecorder [-M &lt;mixername&gt;] [-e &lt;buffersize&gt;] [-i &lt;buffersize&gt;] &lt;audiofile&gt;</synopsis>
        </para></formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><option>-l</option></term>
        <listitem><para>lists the available mixers</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-M &lt;mixername&gt;</option></term>
        <listitem><para>selects a mixer to play on</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-e &lt;buffersize&gt;</option></term>
        <listitem><para>the buffer size to use in the application ("extern")</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-i &lt;buffersize&gt;</option></term>
        <listitem><para>the buffer size to use in Java Sound ("intern")</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>
        There is no way to stop the program besides brute force
        (ctrl-C). There is no way to set the audio quality.
        </para></formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="AudioRecorder.java.html">AudioRecorder.java</ulink>,
        <ulink url="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</ulink>
        </para>
        </formalpara>

-DocBookXML
*/
/*
 * AudioRecorder.java
 *
 * This file is part of the Java Sound Examples.
 */
public class AudioRecorder {
    private static boolean DEBUG = false;
    private static final int DEFAULT_EXTERNAL_BUFFER_SIZE = 128000;
    private static final SupportedFormat[] SUPPORTED_FORMATS = {
                                                                   new SupportedFormat("s8",
                                                                                       AudioFormat.Encoding.PCM_SIGNED,
                                                                                       8,
                                                                                       true),
                                                                   new SupportedFormat("u8",
                                                                                       AudioFormat.Encoding.PCM_UNSIGNED,
                                                                                       8,
                                                                                       true),
                                                                   new SupportedFormat("s16_le",
                                                                                       AudioFormat.Encoding.PCM_SIGNED,
                                                                                       16,
                                                                                       false),
                                                                   new SupportedFormat("s16_be",
                                                                                       AudioFormat.Encoding.PCM_SIGNED,
                                                                                       16,
                                                                                       true),
                                                                   new SupportedFormat("u16_le",
                                                                                       AudioFormat.Encoding.PCM_UNSIGNED,
                                                                                       16,
                                                                                       false),
                                                                   new SupportedFormat("u16_be",
                                                                                       AudioFormat.Encoding.PCM_UNSIGNED,
                                                                                       16,
                                                                                       true),
                                                                   new SupportedFormat("s24_le",
                                                                                       AudioFormat.Encoding.PCM_SIGNED,
                                                                                       24,
                                                                                       false),
                                                                   new SupportedFormat("s24_be",
                                                                                       AudioFormat.Encoding.PCM_SIGNED,
                                                                                       24,
                                                                                       true),
                                                                   new SupportedFormat("u24_le",
                                                                                       AudioFormat.Encoding.PCM_UNSIGNED,
                                                                                       24,
                                                                                       false),
                                                                   new SupportedFormat("u24_be",
                                                                                       AudioFormat.Encoding.PCM_UNSIGNED,
                                                                                       24,
                                                                                       true),
                                                                   new SupportedFormat("s32_le",
                                                                                       AudioFormat.Encoding.PCM_SIGNED,
                                                                                       32,
                                                                                       false),
                                                                   new SupportedFormat("s32_be",
                                                                                       AudioFormat.Encoding.PCM_SIGNED,
                                                                                       32,
                                                                                       true),
                                                                   new SupportedFormat("u32_le",
                                                                                       AudioFormat.Encoding.PCM_UNSIGNED,
                                                                                       32,
                                                                                       false),
                                                                   new SupportedFormat("u32_be",
                                                                                       AudioFormat.Encoding.PCM_UNSIGNED,
                                                                                       32,
                                                                                       true),
                                                               };

    /*
     * This set of types is used if AudioSystem.getAudioFileTypes()
     * returns an array of length 0. This is necessary because the
     * Sun jdk1.3.0 does so (Yes, it is a bug).
     */
    private static final AudioFileFormat.Type[] DEFAULT_TYPES = {
                                                                    AudioFileFormat.Type.WAVE,
                                                                    AudioFileFormat.Type.AU,
                                                                    AudioFileFormat.Type.AIFF,
                                                                    AudioFileFormat.Type.AIFC,
                                                                    AudioFileFormat.Type.SND,
                                                                };

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsageAndExit();
        }

        // int nQuality = QUALITY_DEFAULT;
        String strExtension = null;

        /*
         * Parsing of command-line options takes place...
         */
        String strMixerName = null;
        int nExternalBufferSize = DEFAULT_EXTERNAL_BUFFER_SIZE;
        int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;
        String strFormat = "s16_le";
        int nChannels = 2;
        float fRate = 44100.0F;

        /*
         * Parsing of command-line options takes place...
         */
        Getopt g = new Getopt("AudioRecorder", args, "hlLM:e:i:f:c:r:t:D");
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
            case 'h':
                printUsageAndExit();
            case 'l':
                listMixersAndExit();
            case 'L':
                listPossibleTargetTypes();
                System.exit(0);
            case 'M':
                strMixerName = g.getOptarg();
                if (DEBUG) {
                    out("AudioRecorder.main(): mixer name: " + strMixerName);
                }
                break;
            case 'e':
                nExternalBufferSize = Integer.parseInt(g.getOptarg());
                break;
            case 'i':
                nInternalBufferSize = Integer.parseInt(g.getOptarg());
                break;
            case 'f':
                strFormat = g.getOptarg().toLowerCase();
                break;
            case 'c':
                nChannels = Integer.parseInt(g.getOptarg());
                break;
            case 'r':
                fRate = Float.parseFloat(g.getOptarg());
                break;
            case 't':
                strExtension = g.getOptarg();
                break;
            case 'D':
                DEBUG = true;
                break;
            case '?':
                printUsageAndExit();
            default:
                out("getopt() returned " + c);
                break;
            }
        }

        /*
         *        We make shure that there is only one more argument, which
         *        we take as the filename of the soundfile to store to.
         */
        String strFilename = null;
        for (int i = g.getOptind(); i < args.length; i++) {
            if (strFilename == null) {
                strFilename = args[i];
            } else {
                printUsageAndExit();
            }
        }
        if (strFilename == null) {
            printUsageAndExit();
        }

        File outputFile = new File(strFilename);

        AudioFormat.Encoding encoding = null;
        int nBitsPerSample = 0;
        boolean bBigEndian = true;
        if (strFormat.equals("phone")) {
            /*
             *        8 kHz, 8 bit, mono
             */
            encoding = AudioFormat.Encoding.PCM_SIGNED;
            fRate = 8000.0F;
            nBitsPerSample = 8;
            nChannels = 1;
            bBigEndian = true;
        } else if (strFormat.equals("radio")) {
            /*
             *        22.05 kHz, 8 bit, mono
             */
            encoding = AudioFormat.Encoding.PCM_SIGNED;
            fRate = 22050.0F;
            nBitsPerSample = 8;
            nChannels = 1;
            bBigEndian = true;
        } else if (strFormat.equals("cd")) {
            /*
             *        44.1 kHz, 16 bit, stereo, little-endian
             */
            encoding = AudioFormat.Encoding.PCM_SIGNED;
            fRate = 44100.0F;
            nBitsPerSample = 16;
            nChannels = 2;
            bBigEndian = false;
        } else if (strFormat.equals("dat")) {
            /*
             *        48 kHz, 16 bit, stereo, little-endian
             */
            encoding = AudioFormat.Encoding.PCM_SIGNED;
            fRate = 48000.0F;
            nBitsPerSample = 16;
            nChannels = 2;
            bBigEndian = false;
        } else {
            int nOutputFormatIndex = -1;
            for (int i = 0; i < SUPPORTED_FORMATS.length; i++) {
                if (SUPPORTED_FORMATS[i].getName().equals(strFormat)) {
                    nOutputFormatIndex = i;
                    break;
                }
            }
            if (nOutputFormatIndex != -1) {
                encoding = SUPPORTED_FORMATS[nOutputFormatIndex].getEncoding();

                // TODO:
            } else {
                out("warning: output format " + strFormat +
                    " not supported; using default output format");
            }
        }

        int nFrameSize = (nBitsPerSample / 8) * nChannels;
        AudioFormat audioFormat = new AudioFormat(encoding, fRate,
                                                  nBitsPerSample, nChannels,
                                                  nFrameSize, fRate, bBigEndian);

        AudioFileFormat.Type targetType = AudioFileFormat.Type.AU;
        if (strExtension != null) {
            targetType = findTargetType(strExtension);
            if (targetType == null) {
                out("hell, not a supported format!");
                targetType = AudioFileFormat.Type.AU;
            }
        }

        TargetDataLine line = null;
        line = getTargetDataLine(strMixerName, audioFormat, nInternalBufferSize);
        if (line == null) {
            out("can't get TargetDataLine, exiting.");
            System.exit(1);
        }

        DirectRecordingStream recorder = null;
        recorder = new DirectRecordingStream(line, targetType, outputFile);

        out("Press ENTER to start the recording.");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recorder.start();
        out("Recording...");
        out("Press ENTER to stop the recording.");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recorder.stopRecording();
        out("Recording stopped.");

        // System.exit(0);
    }

    private static void printUsageAndExit() {
        out("AudioRecorder: usage:");
        out("\tjava AudioRecorder -l");
        out("\tjava AudioRecorder -L");
        out("\tjava AudioRecorder [-f <format>] [-c <numchannels>] [-r <samplingrate>] [-t <targettype>] <soundfile>");
        System.exit(0);
    }

    private static void listPossibleTargetTypes() {
        AudioFileFormat.Type[] aTypes = AudioSystem.getAudioFileTypes();

        /*
         *        Workaround for a bug in the Sun jdk1.3.0.
         */
        if (aTypes.length == 0) {
            aTypes = DEFAULT_TYPES;
        }
        System.out.print("Supported target types:");
        for (int i = 0; i < aTypes.length; i++) {
            System.out.print(" " + aTypes[i].getExtension());
        }
        System.out.print("\n");
    }

    /**        Trying to get an audio file type for the passed extension.
     *        This works by examining all available file types. For each
     *        type, if the extension this type promisses to handle matches
     *        the extension we are trying to find a type for, this type is
     *        returned.
     *        If no appropriate type is found, null is returned.
     */
    private static AudioFileFormat.Type findTargetType(String strExtension) {
        AudioFileFormat.Type[] aTypes = AudioSystem.getAudioFileTypes();

        /*
         *        Workaround for a bug in the Sun jdk1.3.0.
         */
        if (aTypes.length == 0) {
            aTypes = DEFAULT_TYPES;
        }
        for (int i = 0; i < aTypes.length; i++) {
            if (aTypes[i].getExtension().equals(strExtension)) {
                return aTypes[i];
            }
        }
        return null;
    }

    private static void listMixersAndExit() {
        out("Available Mixers:");

        Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
        for (int i = 0; i < aInfos.length; i++) {
            out(aInfos[i].getName());
        }
        if (aInfos.length == 0) {
            out("[No mixers available]");
        }
        System.exit(0);
    }

    /*
     *        This method tries to return a Mixer.Info whose name
     *        matches the passed name. If no matching Mixer.Info is
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

    private static TargetDataLine getTargetDataLine(String strMixerName,
                                                    AudioFormat audioFormat,
                                                    int nBufferSize) {
        /*
         *        Asking for a line is a rather tricky thing.
         *        We have to construct an Info object that specifies
         *        the desired properties for the line.
         *        First, we have to say which kind of line we want. The
         *        possibilities are: SourceDataLine (for playback), Clip
         *        (for repeated playback)        and TargetDataLine (for
         *         recording).
         *        Here, we want to do normal capture, so we ask for
         *        a TargetDataLine.
         *        Then, we have to pass an AudioFormat object, so that
         *        the Line knows which format the data passed to it
         *        will have.
         *        Furthermore, we can give Java Sound a hint about how
         *        big the internal buffer for the line should be. This
         *        isn't used here, signaling that we
         *        don't care about the exact size. Java Sound will use
         *        some default value for the buffer size.
         */
        TargetDataLine line = null;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class,
                                               audioFormat, nBufferSize);
        try {
            if (strMixerName != null) {
                Mixer.Info mixerInfo = getMixerInfo(strMixerName);
                if (mixerInfo == null) {
                    out("AudioPlayer: mixer not found: " + strMixerName);
                    System.exit(1);
                }

                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                line = (TargetDataLine) mixer.getLine(info);
            } else {
                line = (TargetDataLine) AudioSystem.getLine(info);
            }

            /*
             *        The line is there, but it is not yet ready to
             *        receive audio data. We have to open the line.
             */
            line.open(audioFormat, nBufferSize);
        } catch (LineUnavailableException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        return line;
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }

///////////// inner classes ////////////////////
    private static class SupportedFormat {
        private String m_strName;
        private AudioFormat.Encoding m_encoding;
        private int m_nSampleSize;
        private boolean m_bBigEndian;

        // sample size is in bits
        public SupportedFormat(String strName, AudioFormat.Encoding encoding,
                               int nSampleSize, boolean bBigEndian) {
            m_strName = strName;
            m_encoding = encoding;
            m_nSampleSize = nSampleSize;
        }

        public String getName() {
            return m_strName;
        }

        public AudioFormat.Encoding getEncoding() {
            return m_encoding;
        }

        public int getSampleSize() {
            return m_nSampleSize;
        }

        public boolean getBigEndian() {
            return m_bBigEndian;
        }
    }
}
