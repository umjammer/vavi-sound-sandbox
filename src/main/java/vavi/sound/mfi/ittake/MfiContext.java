/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.ittake;


/**
 * midi context for the converter.
 * 
 * @author ittake
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030915 nsano initial version <br>
 */
public class MfiContext {

    /** */
    public int toMLDNote(int midiNote) {
        return midiNote - 33;
    }

    /** */
    public long toMLDTime(long midiAbs) {
        return (midiAbs * mfiResolution) / midiResolution;
    }

    /** */
    public long getLimitDeltaTime() {
        return (255L * midiResolution) / mfiResolution;
    }

    /** */
    private long mfiResolution;

    /** */
    public long getMfiResolution() {
        return mfiResolution;
    }

    /** */
    public void setMfiResolution(long mfiResolution) {
        this.mfiResolution = mfiResolution;
    }

    /** */
    private int midiResolution;

    /** */
    public int getMidiResolution() {
        return midiResolution;
    }

    /** */
    public void setMidiResolution(int midiResolution) {
        this.midiResolution = midiResolution;
    }
}

/* */
