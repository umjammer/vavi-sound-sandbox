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

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSArray;
import org.rococoa.cocoa.foundation.NSObject;

import static java.lang.System.getLogger;


/**
 * @interface AVAudioUnitComponentManager : NSObject
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/??/?? umjammer initial version <br>
 */
public abstract class AVAudioUnitComponentManager extends NSObject {

    private static final Logger logger = getLogger(AVAudioUnitComponentManager.class.getName());

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioUnitComponentManager", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioUnitComponentManager sharedAudioUnitComponentManager();
    }

    public static AVAudioUnitComponentManager shared() {
        AVAudioUnitComponentManager manager = CLASS.sharedAudioUnitComponentManager();
logger.log(Level.DEBUG, manager);
        return manager;
    }

    public abstract NSArray componentsMatchingDescription(AudioComponentDescription.ByValue desc);

    public List<AVAudioUnitComponent> components(AudioComponentDescription desc) {
        List<AVAudioUnitComponent> result = new ArrayList<>();
        NSArray components = componentsMatchingDescription(desc.byValue());
logger.log(Level.DEBUG, "components: " + components.count());
        for (int i = 0; i < components.count(); i++) {
            result.add(org.rococoa.Rococoa.cast(components.objectAtIndex(i), AVAudioUnitComponent.class));
        }
        return result;
    }
}