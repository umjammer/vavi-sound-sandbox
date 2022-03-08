
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSDictionary;
import org.rococoa.cocoa.foundation.NSObject;

import com.sun.jna.Pointer;

public abstract class AVAudioUnitComponent extends NSObject {

    @SuppressWarnings({ "unused", "hiding" })
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioUnitComponent", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioUnitComponent alloc();
    }

    /**
     * @return AudioComponent
     */
    public abstract Pointer audioComponent();

    /**
     * @return <NSString,ID>
     */
    public abstract NSDictionary configurationDictionary();

    public abstract AudioComponentDescription.ByValue audioComponentDescription();

    public abstract boolean hasCustomView();

    public abstract boolean hasMIDIInput();

    public abstract boolean hasMIDIOutput();

    public abstract boolean passesAUVal();

    public abstract boolean isSandboxSafe();

    public abstract boolean supportsNumberInputChannels_outputChannels(int numInputChannels, int numOutputChannels);

    public abstract String manufacturerName();

    public abstract String name();

    public abstract String typeName();

    public abstract int version();

    public abstract String versionString();
}