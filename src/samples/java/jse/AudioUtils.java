/*
 * Copyright (c) 2000 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
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
 */

package jse;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import static java.lang.System.getLogger;


/**
 * AudioUtils.java
 *
 * @see AudioStream For an example of usage
 * <p>
 * This file is part of the Java Sound Examples.
 */
public class AudioUtils {

    private static final Logger logger = getLogger(AudioUtils.class.getName());

    public static Iterator<AudioFormat> getSupportedSourceDataLineFormats(Mixer mixer) {
        Line.Info[] aLineInfos = mixer.getSourceLineInfo(new Line.Info(SourceDataLine.class));
        return getSupportedSourceDataLineFormatsImpl(aLineInfos);
    }

    public static Iterator<AudioFormat> getSupportedSourceDataLineFormats() {
        Line.Info[] aLineInfos = AudioSystem.getSourceLineInfo(new Line.Info(SourceDataLine.class));
        return getSupportedSourceDataLineFormatsImpl(aLineInfos);
    }

    private static Iterator<AudioFormat> getSupportedSourceDataLineFormatsImpl(Line.Info[] aLineInfos) {
        List<AudioFormat> formats = new ArrayList<>();
        for (Line.Info aLineInfo : aLineInfos) {
            if (aLineInfo instanceof DataLine.Info) {
                AudioFormat[] aFormats = ((DataLine.Info) aLineInfo).getFormats();
                for (AudioFormat aFormat : aFormats) {
                    if (!formats.contains(aFormat)) {
                        formats.add(aFormat);
                    }
                }
            } else {
                // No chance to get useful information,
                // so do nothing.
            }
        }
        return formats.iterator();
    }

    public static AudioFormat getSuitableTargetFormat(AudioFormat sourceFormat) {
        Iterator<AudioFormat> possibleFormats = getSupportedSourceDataLineFormats();
        return getSuitableTargetFormatImpl(possibleFormats, sourceFormat);
    }

    public static AudioFormat getSuitableTargetFormat(Mixer mixer, AudioFormat sourceFormat) {
        Iterator<AudioFormat> possibleFormats = getSupportedSourceDataLineFormats(mixer);
        return getSuitableTargetFormatImpl(possibleFormats, sourceFormat);
    }

    public static AudioFormat getSuitableTargetFormatImpl(Iterator<AudioFormat> possibleTargetFormats, AudioFormat sourceFormat) {
        // TODO: should use some preference algorithm to use best possible formats.
        while (possibleTargetFormats.hasNext()) {
            AudioFormat possibleTargetFormat = possibleTargetFormats.next();
            if (AudioSystem.isConversionSupported(possibleTargetFormat, sourceFormat)) {
                return possibleTargetFormat;
            }
        }

        // No suitable format found.
        return null;
    }

    public static AudioInputStream getSuitableAudioInputStream(AudioInputStream sourceAudioInputStream) {
        AudioFormat targetFormat = getSuitableTargetFormat(sourceAudioInputStream.getFormat());
        return getSuitableAudioInputStreamImpl(sourceAudioInputStream, targetFormat);
    }

    public static AudioInputStream getSuitableAudioInputStream(Mixer mixer, AudioInputStream sourceAudioInputStream) {
        AudioFormat targetFormat = getSuitableTargetFormat(mixer, sourceAudioInputStream.getFormat());
        return getSuitableAudioInputStreamImpl(sourceAudioInputStream, targetFormat);
    }

    public static AudioInputStream getSuitableAudioInputStreamImpl(AudioInputStream sourceAudioInputStream, AudioFormat targetFormat) {
        logger.log(Level.DEBUG, "AudioUtils.getSuitableAudioInputStreamImpl(): target format: " + targetFormat);
        if (targetFormat != null) {
            logger.log(Level.DEBUG, "AudioUtils.getSuitableAudioInputStreamImpl(): trying to do a conversion.");
            return AudioSystem.getAudioInputStream(targetFormat, sourceAudioInputStream);
        } else {
            logger.log(Level.DEBUG, "AudioUtils.getSuitableAudioInputStreamImpl(): returning null as resulting AudioInputStream.");
            return null;
        }
    }
}
