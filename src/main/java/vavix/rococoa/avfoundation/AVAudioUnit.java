/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;

import org.rococoa.Foundation;
import org.rococoa.ID;
import org.rococoa.ObjCBlock;
import org.rococoa.ObjCBlocks.BlockLiteral;
import org.rococoa.ObjCClass;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSError;

import static java.lang.System.getLogger;
import static org.rococoa.ObjCBlocks.block;
import static vavix.rococoa.avfoundation.AudioToolbox.AudioUnitPropertyID;
import static vavix.rococoa.avfoundation.AudioToolbox.AudioUnitScope;


/**
 * @interface AVAudioUnit : AVAudioNode
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/??/?? umjammer initial version <br>
 */
public abstract class AVAudioUnit extends AVAudioNode {

    private static final Logger logger = getLogger(AVAudioUnit.class.getName());

    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioUnit", _Class.class);

    private interface _Class extends ObjCClass {
        AVAudioUnit alloc();
        /**
         * @param options AudioComponentInstantiationOptions
         * @param completionHandler BiFunction<AVAudioUnit, NSError, Void>
         */
        void instantiateWithComponentDescription_options_completionHandler(AudioComponentDescription.ByValue audioComponentDescription,
                                                                           int /* AudioComponentInstantiationOptions */ options,
                                                                           BlockLiteral completionHandler);
    }

    // AudioComponentInstantiationOptions
    public static final int kAudioComponentInstantiation_LoadInProcess = 2;
    public static final int kAudioComponentInstantiation_LoadOutOfProcess = 1;

    public static AVAudioUnit instantiate(AudioComponentDescription desc, int options) {
        AtomicReference<AVAudioUnit> audioUnit = new AtomicReference<>();
        AtomicReference<NSError> error = new AtomicReference<>();
        CountDownLatch cdl = new CountDownLatch(1);
        BlockLiteral completionHandler = block((CompletionHandler) (block, audioUnitId, errorId) -> {
logger.log(Level.DEBUG, audioUnitId + ", " + errorId + ", " + errorId.isNull());
            if (errorId.isNull()) {
logger.log(Level.DEBUG, "audioUnit: " + Rococoa.wrap(audioUnitId, AVAudioUnit.class));
                audioUnit.set(Rococoa.wrap(audioUnitId, AVAudioUnit.class));
            } else {
                error.set(Rococoa.wrap(errorId, NSError.class));
            }
            cdl.countDown();
        });
        CLASS.instantiateWithComponentDescription_options_completionHandler(desc.byValue(), options, completionHandler);
        try { cdl.await(); } catch (InterruptedException ignore) { }
        Foundation.getRococoaLibrary().releaseObjCBlock(completionHandler.getPointer());
        if (error.get() != null) {
            throw new IllegalStateException(error.get().description());
        }
        return audioUnit.get();
    }

    public interface CompletionHandler extends ObjCBlock {
        void complete(BlockLiteral block, ID /* AVAudioUnit */ audioUnit, ID /* NSError */ error);
    }

    /**
     * @return AudioUnit
     */
    public abstract Pointer audioUnit();

//#region parameters

    /**
     * Every parameter of the global scope, in the order the audio unit reports them.
     * This is how you find out the id, the range and the default of what you want to tweak.
     */
    public List<AudioUnitParameterInfo> getParameters() {
        IntByReference size = new IntByReference();
        int status = AudioToolbox.instance.AudioUnitGetPropertyInfo(audioUnit(),
                AudioUnitPropertyID.kAudioUnitProperty_ParameterList.id,
                AudioUnitScope.kAudioUnitScope_Global.ordinal(),
                0,
                size,
                null);
        if (status != 0 || size.getValue() == 0) {
            return List.of();
        }
        Memory ids = new Memory(size.getValue());
        status = AudioToolbox.instance.AudioUnitGetProperty(audioUnit(),
                AudioUnitPropertyID.kAudioUnitProperty_ParameterList.id,
                AudioUnitScope.kAudioUnitScope_Global.ordinal(),
                0,
                ids,
                size);
        if (status != 0) {
            throw new IllegalStateException("AudioUnitGetProperty(ParameterList): " + status);
        }
        List<AudioUnitParameterInfo> result = new ArrayList<>();
        for (int id : ids.getIntArray(0, size.getValue() / Integer.BYTES)) {
            AudioUnitParameterInfo info = new AudioUnitParameterInfo();
            IntByReference infoSize = new IntByReference(info.size());
            // the parameter id goes in as the element, not as the scope
            status = AudioToolbox.instance.AudioUnitGetProperty(audioUnit(),
                    AudioUnitPropertyID.kAudioUnitProperty_ParameterInfo.id,
                    AudioUnitScope.kAudioUnitScope_Global.ordinal(),
                    id,
                    info.getPointer(),
                    infoSize);
            if (status != 0) {
logger.log(Level.WARNING, "AudioUnitGetProperty(ParameterInfo, " + id + "): " + status);
                continue;
            }
            info.read();
            info.setId(id);
            result.add(info);
        }
        return result;
    }

    /** @param id AudioUnitParameterID, see {@link #getParameters()} */
    public void setParameter(int id, float value) {
        int status = AudioToolbox.instance.AudioUnitSetParameter(audioUnit(),
                id,
                AudioUnitScope.kAudioUnitScope_Global.ordinal(),
                0,
                value,
                0);
        if (status != 0) {
            throw new IllegalArgumentException("AudioUnitSetParameter(" + id + ", " + value + "): " + status);
        }
    }

    /**
     * Sets a parameter by its display name, ignoring case, so you do not have to look the
     * numeric id up. ex. {@code setParameter("dry/wet mix", 20)} on AUMatrixReverb.
     *
     * @throws IllegalArgumentException no such parameter, or the value is out of its range
     */
    public void setParameter(String name, float value) {
        AudioUnitParameterInfo parameter = findParameter(name);
        if (value < parameter.minValue || value > parameter.maxValue) {
            throw new IllegalArgumentException("out of range: " + parameter + ", value: " + value);
        }
        setParameter(parameter.id(), value);
    }

    /** @param id AudioUnitParameterID, see {@link #getParameters()} */
    public float getParameter(int id) {
        FloatByReference value = new FloatByReference();
        int status = AudioToolbox.instance.AudioUnitGetParameter(audioUnit(),
                id,
                AudioUnitScope.kAudioUnitScope_Global.ordinal(),
                0,
                value);
        if (status != 0) {
            throw new IllegalArgumentException("AudioUnitGetParameter(" + id + "): " + status);
        }
        return value.getValue();
    }

    /** @throws IllegalArgumentException no such parameter */
    public float getParameter(String name) {
        return getParameter(findParameter(name).id());
    }

    /** @throws IllegalArgumentException no such parameter */
    private AudioUnitParameterInfo findParameter(String name) {
        return getParameters().stream()
                .filter(p -> p.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no such parameter: " + name + " of " + name()));
    }

//#endregion

    public abstract AudioComponentDescription.ByValue audioComponentDescription();

    public abstract String manufacturerName();

    public abstract String name();

    public abstract int version();

    @Override
    public abstract AUAudioUnit AUAudioUnit();
}
