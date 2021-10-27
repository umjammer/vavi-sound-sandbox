
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.cocoa.foundation.NSError;
import org.rococoa.cocoa.foundation.NSObject;


public abstract class AVAudioEngine extends NSObject {

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioEngine", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioEngine alloc();
    }

    public abstract AVAudioEngine init();

    public static AVAudioEngine newInstance() {
        AVAudioEngine engine = CLASS.alloc();
        return engine.init();
    }

    public abstract void attachNode(AVAudioNode node);

    public abstract void connect_to_format(AVAudioNode node1, AVAudioNode node2, AVAudioFormat format);

    public abstract void disconnectNodeInput(AVAudioNode node);

    public abstract void disconnectNodeOutput(AVAudioNode node);

    public abstract AVAudioInputNode inputNode();

    public abstract AVAudioOutputNode outputNode();

    public abstract AVAudioMixerNode mainMixerNode();

    public abstract boolean startAndReturnError(ObjCObjectByReference error);

    public boolean start() {
        ObjCObjectByReference outError = new ObjCObjectByReference();
        boolean r = startAndReturnError(outError);
        NSError error = outError.getValueAs(NSError.class);
        return r;
    }

    public abstract void prepare();

    public abstract void pause();

    public abstract void stop();

    public abstract void reset();

    public abstract boolean running();
}