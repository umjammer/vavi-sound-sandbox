/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.jna.Pointer;

import org.rococoa.Foundation;
import org.rococoa.ID;
import org.rococoa.ObjCBlock;
import org.rococoa.ObjCBlocks.BlockLiteral;
import org.rococoa.ObjCClass;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSError;

import static java.lang.System.getLogger;
import static org.rococoa.ObjCBlocks.block;


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

    public abstract AudioComponentDescription.ByValue audioComponentDescription();

    public abstract String manufacturerName();

    public abstract String name();

    public abstract int version();

    @Override
    public abstract AUAudioUnit AUAudioUnit();
}