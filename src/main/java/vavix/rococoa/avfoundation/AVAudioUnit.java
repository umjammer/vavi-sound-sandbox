/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import java.lang.System.Logger;
import java.util.concurrent.CountDownLatch;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import org.rococoa.ID;
import org.rococoa.ObjCClass;

import static java.lang.System.getLogger;


/**
 * @interface AVAudioUnit : AVAudioNode
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/??/?? umjammer initial version <br>
 */
public abstract class AVAudioUnit extends AVAudioNode {

    private static final Logger logger = getLogger(AVAudioUnit.class.getName());

    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioUnit", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioUnit alloc();
        /**
         * @param options AudioComponentInstantiationOptions
         * @param completionHandler BiFunction<AVAudioUnit, NSError, Void>
         */
        void instantiateWithComponentDescription_options_completionHandler(AudioComponentDescription.ByValue audioComponentDescription,
                                                                           int options,
                                                                           Callback completionHandler);
    }

    // AudioComponentInstantiationOptions
    public static final int kAudioComponentInstantiation_LoadInProcess = 2;
    public static final int kAudioComponentInstantiation_LoadOutOfProcess = 1;

    public static AVAudioUnit instantiate(AudioComponentDescription audioComponentDescription,
                                   int options) {
        class Wrapper { AVAudioUnit object; }
        Wrapper result = new Wrapper();
        CountDownLatch cdl = new CountDownLatch(1);
        Callback callback = new Callback() {
            @SuppressWarnings("unused")
            public void apply(ID audioUnit, ID error) {
//                result.object = Rococoa.wrap(audioUnit, AVAudioUnit.class);
//logger.log(Level.TRACE, Rococoa.wrap(error, NSError.class));
//                cdl.countDown();
            }
        };
        CLASS.instantiateWithComponentDescription_options_completionHandler(audioComponentDescription.byValue(), options, callback);
        try { cdl.await(); } catch (InterruptedException e) { e.printStackTrace(); }
        return result.object;
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