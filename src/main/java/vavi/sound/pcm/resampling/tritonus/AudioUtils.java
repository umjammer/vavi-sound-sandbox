/*
 * Copyright (c) 2000 by Matthias Pfisterer
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package vavi.sound.pcm.resampling.tritonus;

import java.util.Iterator;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Mixer;


/**
 * AudioUtils.java
 *
 * This file is part of Tritonus: http://www.tritonus.org/
 */
public class AudioUtils {

    public static long getLengthInBytes(AudioInputStream audioInputStream) {
        return getLengthInBytes(audioInputStream.getFormat(), audioInputStream.getFrameLength());

//        long lLengthInFrames = audioInputStream.getFrameLength();
//        int nFrameSize = audioInputStream.getFormat().getFrameSize();
//        if (lLengthInFrames >= 0 && nFrameSize >= 1) {
//            return lLengthInFrames * nFrameSize;
//        } else {
//            return AudioSystem.NOT_SPECIFIED;
//        }
    }

    /**
     * if the passed value for lLength is
     * AudioSystem.NOT_SPECIFIED (unknown
     * length), the length in bytes becomes
     * AudioSystem.NOT_SPECIFIED, too.
     */
    public static long getLengthInBytes(AudioFormat audioFormat, long lLengthInFrames) {
        int nFrameSize = audioFormat.getFrameSize();
        if (lLengthInFrames >= 0 && nFrameSize >= 1) {
            return lLengthInFrames * nFrameSize;
        } else {
            return AudioSystem.NOT_SPECIFIED;
        }
    }

    public static boolean containsFormat(AudioFormat sourceFormat, Iterator<?> possibleFormats) {
        while (possibleFormats.hasNext()) {
            AudioFormat format = (AudioFormat) possibleFormats.next();
            if (AudioFormats.matches(format, sourceFormat)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Conversion milliseconds -> bytes
     */

    public static long millis2Bytes(long ms, AudioFormat format) {
        return millis2Bytes(ms, format.getFrameRate(), format.getFrameSize());
    }

    public static long millis2Bytes(long ms, double frameRate, int frameSize) {
        return (long) (ms * frameRate / 1000 * frameSize);
    }

    public static long millis2Bytes(double ms, AudioFormat format) {
        return millis2Bytes(ms, format.getFrameRate(), format.getFrameSize());
    }

    public static long millis2Bytes(double ms, double frameRate, int frameSize) {
        return ((long) (ms * frameRate / 1000.0)) * frameSize;
    }

    /**
     * Conversion milliseconds -> bytes (bytes will be frame-aligned)
     */
    public static long millis2BytesFrameAligned(long ms, AudioFormat format) {
        return millis2BytesFrameAligned(ms, format.getFrameRate(), format.getFrameSize());
    }

    public static long millis2BytesFrameAligned(long ms, float frameRate, int frameSize) {
        return ((long) (ms * frameRate / 1000)) * frameSize;
    }

    /**
     * Conversion milliseconds -> frames
     */
    public static long millis2Frames(long ms, AudioFormat format) {
        return millis2Frames(ms, format.getFrameRate());
    }

    public static long millis2Frames(long ms, float frameRate) {
        return (long) (ms * frameRate / 1000);
    }

    /**
     * Conversion bytes -> milliseconds
     */
    public static long bytes2Millis(long bytes, AudioFormat format) {
        return (long) (bytes / format.getFrameRate() * 1000 / format.getFrameSize());
    }

    /**
     * Conversion frames -> milliseconds
     */
    public static long frames2Millis(long frames, AudioFormat format) {
        return (long) (frames / format.getFrameRate() * 1000);
    }

    /**
     *
     * @param sr1 the first sample rate to compare
     * @param sr2 the second sample rate to compare
     * @return true if the sample rates are (almost) identical
     */
    public static boolean sampleRateEquals(float sr1, float sr2) {
        return Math.abs(sr1 - sr2) < 0.0000001;
    }

    /**
     * @param format the audio format to test
     * @return true if the format is either PCM_SIGNED or PCM_UNSIGNED
     */
    public static boolean isPCM(AudioFormat format) {
        return format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)
               || format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED);
    }

    /**
     * Return if the passed mixer info is the Java Sound Audio Engine.
     * 
     * @param mixerInfo the mixer info to query
     * @return true if the mixer info describes the Java Sound Audio Engine
     */
    public static boolean isJavaSoundAudioEngine(Mixer.Info mixerInfo) {
        return mixerInfo.getName().equals("Java Sound Audio Engine");
    }

    /**
     * tries to guess if this program is running on a big endian platform
     * 
     * @return
     */
    public static boolean isSystemBigEndian() {
        return java.nio.ByteOrder.nativeOrder().equals(java.nio.ByteOrder.BIG_ENDIAN);
    }

    //$$fb 2000-07-18: added these debugging functions
    public static String NS_or_number(int number) {
        return (number == AudioSystem.NOT_SPECIFIED) ? "NOT_SPECIFIED" : String.valueOf(number);
    }

    public static String NS_or_number(float number) {
        return (number == AudioSystem.NOT_SPECIFIED) ? "NOT_SPECIFIED" : String.valueOf(number);
    }

    /**
     * For debugging purposes.
     */
    public static String format2ShortStr(AudioFormat format) {
        return format.getEncoding() + "-" +
                NS_or_number(format.getChannels()) + "ch-" +
                NS_or_number(format.getSampleSizeInBits()) + "bit-" +
                NS_or_number(((int) format.getSampleRate())) + "Hz-" +
                (format.isBigEndian() ? "be" : "le");
    }

}
