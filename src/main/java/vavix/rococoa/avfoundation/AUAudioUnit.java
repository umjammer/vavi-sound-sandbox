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

import org.rococoa.Foundation;
import org.rococoa.ID;
import org.rococoa.ObjCBlock;
import org.rococoa.ObjCBlocks.BlockLiteral;
import org.rococoa.ObjCClass;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.Rococoa;
import org.rococoa.RunOnMainThread;
import org.rococoa.cocoa.foundation.NSError;
import org.rococoa.cocoa.foundation.NSObject;
import vavix.rococoa.avfoundation.AVAudioUnit.CompletionHandler;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

import static java.lang.System.getLogger;
import static org.rococoa.ObjCBlocks.block;


/**
 * @interface AUAudioUnit
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/??/?? umjammer initial version <br>
 */
public abstract class AUAudioUnit extends NSObject {

    private static final Logger logger = getLogger(AUAudioUnit.class.getName());

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AUAudioUnit", _Class.class);

    private interface _Class extends ObjCClass {
        /**
         * @param options AudioComponentInstantiationOptions
         * @param completionHandler BiFunction<AVAudioUnit, NSError, Void>
         */
        void instantiateWithComponentDescription_options_completionHandler(AudioComponentDescription.ByValue audioComponentDescription,
                                                                           int /* AudioComponentInstantiationOptions */ options,
                                                                           BlockLiteral /* CompletionHandler */ completionHandler);
    }

    public static AUAudioUnit instantiate(AudioComponentDescription desc, int /* AudioComponentInstantiationOptions */ options) {
        AtomicReference<AUAudioUnit> audioUnit = new AtomicReference<>();
        AtomicReference<NSError> error = new AtomicReference<>();
        CountDownLatch cdl = new CountDownLatch(1);
        BlockLiteral completionHandler = block((AVAudioUnit.CompletionHandler) (block, audioUnitId, errorId) -> {
logger.log(Level.DEBUG, audioUnitId + ", " + errorId + ", " + errorId.isNull());
            if (errorId.isNull()) {
logger.log(Level.DEBUG, "audioUnit: " + Rococoa.wrap(audioUnitId, AUAudioUnit.class));
                audioUnit.set(Rococoa.wrap(audioUnitId, AUAudioUnit.class));
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

    public abstract AUAudioUnit initWithComponentDescription_error(AudioComponentDescription desc, ObjCObjectByReference error);

    /**
     * @param options AudioComponentInstantiationOptions
     */
    public abstract AUAudioUnit initWithComponentDescription_options_error(AudioComponentDescription.ByValue desc, int options, ObjCObjectByReference error);

    public abstract void requestViewControllerWithCompletionHandler(BlockLiteral /* AUViewControllerBase */ completionHandler);

    public interface CompletionHandler extends ObjCBlock {
        // completionHandler NSViewController
        @RunOnMainThread
        void complete(BlockLiteral block, ID /* AUAudioUnit */ audioUnit, ID /* NSError */ error);
    }

    public interface AUViewControllerBase extends ObjCBlock {
        @RunOnMainThread
        void complete(BlockLiteral block, ID /* NSViewController */ viewController);
    }
}
