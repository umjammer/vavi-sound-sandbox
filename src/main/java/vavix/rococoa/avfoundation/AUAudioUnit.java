/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.RunOnMainThread;
import org.rococoa.cocoa.foundation.NSError;
import org.rococoa.cocoa.foundation.NSObject;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

import vavi.util.Debug;


/**
 * @interface AUAudioUnit : NSObject
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/??/?? umjammer initial version <br>
 */
public abstract class AUAudioUnit extends NSObject {

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AUAudioUnit", _Class.class);

    public interface _Class extends ObjCClass {
        AUAudioUnit alloc();
        /**
         * @param options AudioComponentInstantiationOptions
         * @param completionHandler BiFunction<AVAudioUnit, NSError, Void>
         */
        void instantiateWithComponentDescription_options_completionHandler(AudioComponentDescription.ByValue audioComponentDescription,
                                                                           int options,
                                                                           Callback completionHandler);
    }

    public static AUAudioUnit initWithComponentDescription(AudioComponentDescription desc) {
        ObjCObjectByReference outError = new ObjCObjectByReference();
        AUAudioUnit audioUnit = CLASS.alloc();
Debug.println(audioUnit);
        audioUnit = audioUnit.initWithComponentDescription_error(desc.byValue(), outError);
Debug.println(audioUnit);
        NSError error = outError.getValueAs(NSError.class);
        if (error != null) {
            throw new IllegalStateException(error.description());
        }
        return audioUnit;
    }

    public abstract AUAudioUnit initWithComponentDescription_error(AudioComponentDescription desc, ObjCObjectByReference error);

    /**
     * @param options AudioComponentInstantiationOptions
     */
    public abstract AUAudioUnit initWithComponentDescription_options_error(AudioComponentDescription.ByValue desc, int options, ObjCObjectByReference error);

    public interface CompletionHandlerCallback extends Callback {
        // completionHandler NSViewController
        @RunOnMainThread
        void completionHandler(Pointer completionHandler);
    }

    /**
     * @param completionHandler Function<AUViewControllerBase, Void>
     */
    @RunOnMainThread
    public abstract void requestViewControllerWithCompletionHandler(CompletionHandlerCallback completionHandler);
}