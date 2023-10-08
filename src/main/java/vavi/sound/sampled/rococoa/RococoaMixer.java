/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;


/**
 * RococoaMixer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/21 umjammer initial version <br>
 */
public class RococoaMixer implements Mixer {

    public static Mixer.Info mixerInfo = new Mixer.Info(
        "Rococoa Mixer",
        "vavi",
        "Mixer for Rococoa",
        "0.0.1") {};

    /** TODO how about multiple clips */
    private final RococoaClip clip = new RococoaClip();

    @Override
    public javax.sound.sampled.Line.Info getLineInfo() {
        return clip.getLineInfo();
    }

    @Override
    public void open() throws LineUnavailableException {
        clip.open();
    }

    @Override
    public void close() {
        clip.close();
    }

    @Override
    public boolean isOpen() {
        return clip.isOpen();
    }

    @Override
    public Control[] getControls() {
        return clip.getControls();
    }

    @Override
    public boolean isControlSupported(Type control) {
        return clip.isControlSupported(control);
    }

    @Override
    public Control getControl(Type control) {
        return clip.getControl(control);
    }

    @Override
    public void addLineListener(LineListener listener) {
        clip.addLineListener(listener);
    }

    @Override
    public void removeLineListener(LineListener listener) {
        clip.removeLineListener(listener);
    }

    @Override
    public Info getMixerInfo() {
        return mixerInfo;
    }

    @Override
    public javax.sound.sampled.Line.Info[] getSourceLineInfo() {
        return new javax.sound.sampled.Line.Info[0];
    }

    @Override
    public javax.sound.sampled.Line.Info[] getTargetLineInfo() {
        return new javax.sound.sampled.Line.Info[] {
            clip.getLineInfo()
        };
    }

    @Override
    public javax.sound.sampled.Line.Info[] getSourceLineInfo(javax.sound.sampled.Line.Info info) {
        return getSourceLineInfo();
    }

    @Override
    public javax.sound.sampled.Line.Info[] getTargetLineInfo(javax.sound.sampled.Line.Info info) {
        if (info == clip.getLineInfo()) {
            return getTargetLineInfo();
        } else {
            return new javax.sound.sampled.Line.Info[0];
        }
    }

    @Override
    public boolean isLineSupported(javax.sound.sampled.Line.Info info) {
        return info == clip.getLineInfo();
    }

    @Override
    public Line getLine(javax.sound.sampled.Line.Info info) throws LineUnavailableException {
        if (info == clip.getLineInfo()) {
            return clip;
        } else {
            return null;
        }
    }

    @Override
    public int getMaxLines(javax.sound.sampled.Line.Info info) {
        if (info == clip.getLineInfo()) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public Line[] getSourceLines() {
        // TODO should return default source data line?
        return new Line[0];
    }

    @Override
    public Line[] getTargetLines() {
        if (isOpen()) {
            return new Line[] {
                clip
            };
        } else {
            return new Line[0];
        }
    }

    @Override
    public void synchronize(Line[] lines, boolean maintainSync) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void unsynchronize(Line[] lines) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isSynchronizationSupported(Line[] lines, boolean maintainSync) {
        // TODO Auto-generated method stub
        return false;
    }
}

/* */
