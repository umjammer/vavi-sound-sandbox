/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import org.rococoa.Foundation;
import org.rococoa.IDByReference;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;


/**
 * AudioToolbox.
 *
 * AudioUnit = AudioComponentInstance = ComponentInstanceRecord
 * AudioComponent = OpaqueAudioComponent
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/02 umjammer initial version <br>
 */
public interface AudioToolbox extends com.sun.jna.Library {

    AudioToolbox instance = com.sun.jna.Native.load("AudioToolbox", AudioToolbox.class);

    /**
     * @param inUnit AudioUnit
     * @return OSStatus
     */
    int MusicDeviceMIDIEvent(Pointer inUnit, int inStatus, int inData1, int inData2, int inOffsetSampleFrame);

    enum AudioUnitPropertyID {
        kMusicDeviceProperty_SoundBankURL(1100);
        final int id;
        AudioUnitPropertyID(int id) {
            this.id = id;
        }
    }

    enum AudioUnitScope {
        kAudioUnitScope_Global
    }

    /**
     * @param inUnit AudioUnit
     * @param inID AudioUnitPropertyID
     * @param inScope AudioUnitScope
     * @param inData TODO how can we tell objc pointer
     * @return OSStatus
     */
    int AudioUnitSetProperty(Pointer inUnit, int inID, int inScope, int inElement, Pointer inData, int inDataSize);

    /**
     * @param inUnit AudioComponent
     * @param inID AudioUnitPropertyID
     * @param inScope AudioUnitScope
     * @param inElement AudioUnitElement
     * @return OSStatus
     */
    int AudioUnitGetProperty(Pointer inUnit, int inID, int inScope, int inElement, Pointer outData, IntByReference ioDataSize);

    int AudioComponentCount(AudioComponentDescription inDesc);

    /**
     * @return AudioComponent
     */
    Pointer AudioComponentFindNext(Pointer inComponent, AudioComponentDescription inDesc);

    /**
     * @param inComponent AudioComponent
     * @param outName CFStringRef
     * @return OSStatus
     */
    int AudioComponentCopyName(Pointer inComponent, IDByReference outName);

    static String AudioComponentName(Pointer inComponent) {
        IDByReference outName = new IDByReference();
        int r = instance.AudioComponentCopyName(inComponent, outName);
        if (r != 0) {
            throw new IllegalStateException(String.valueOf(r));
        }
//Debug.println(r + ", " + outName);
        return Foundation.toString(outName.getValue());
    }
}

/* */
