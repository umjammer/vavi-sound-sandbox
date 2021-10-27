
package vavix.rococoa.avfoundation;

import java.util.ArrayList;
import java.util.List;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSArray;
import org.rococoa.cocoa.foundation.NSObject;

import vavi.util.Debug;


public abstract class AVAudioUnitComponentManager extends NSObject {

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioUnitComponentManager", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioUnitComponentManager sharedAudioUnitComponentManager();
    }

    public static AVAudioUnitComponentManager sharedInstance() {
        AVAudioUnitComponentManager manager = CLASS.sharedAudioUnitComponentManager();
Debug.println(manager);
        return manager;
    }

    public abstract NSArray componentsMatchingDescription(AudioComponentDescription.ByValue desc);

    public List<AVAudioUnitComponent> components(AudioComponentDescription desc) {
        List<AVAudioUnitComponent> result = new ArrayList<>();
        NSArray components = componentsMatchingDescription(desc.byValue());
Debug.println(components.count());
        for (int i = 0; i < components.count(); i++) {
            result.add(org.rococoa.Rococoa.cast(components.objectAtIndex(i), AVAudioUnitComponent.class));
        }
        return result;
    }
}