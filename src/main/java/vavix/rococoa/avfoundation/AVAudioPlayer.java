
package vavix.rococoa.avfoundation;

import java.io.IOException;
import java.net.URI;

import org.rococoa.ID;
import org.rococoa.ObjCClass;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.cocoa.foundation.NSData;
import org.rococoa.cocoa.foundation.NSError;
import org.rococoa.cocoa.foundation.NSObject;
import org.rococoa.cocoa.foundation.NSURL;

import com.sun.jna.Callback;


public abstract class AVAudioPlayer extends NSObject {

    @SuppressWarnings({ "hiding" })
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioPlayer", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioPlayer alloc();
    }

    public static AVAudioPlayer init(URI uri) throws IOException {
        NSURL fileURL = NSURL.URLWithString(uri.toString());
        AVAudioPlayer player = CLASS.alloc();
        ObjCObjectByReference outError = new ObjCObjectByReference();
        player.initWithContentsOfURL_error(fileURL, outError);
        NSError error = outError.getValueAs(NSError.class);
        if (error != null) {
            throw new IOException(error.description());
        }
        return player;
    }

    public abstract AVAudioPlayer initWithContentsOfURL_error(NSURL url, ObjCObjectByReference outError);

    public abstract AVAudioPlayer initWithData_(NSData data, ObjCObjectByReference outError);

    public abstract boolean prepareToPlay();

    public abstract boolean play();

    public abstract boolean playAtTime(double time);

    public abstract void pause();

    public abstract void stop();

    public abstract boolean isPlaying();

    public abstract float volume();

    public abstract void setVolume(float volume);

    public abstract void setVolume_fadeDuration(float volume, double duration);

    public abstract float pan();

    public abstract void setPan(float pan);

    public abstract boolean enableRate();

    public abstract void setEnableRate(boolean enableRate);

    public abstract float rate();

    public abstract void setRate(float rate);

    public abstract long numberOfLoops();

    public abstract void setNumberOfLoops(long numberOfLoops);

    public abstract AVAudioFormat format();

    public abstract double currentTime();

    public abstract void setCurrentTime(double currentTime);

    public abstract double duration();

    // TODO doesn't work
    public interface Delegate extends Callback {
        void audioPlayerDidFinishPlaying_successfully(ID player, boolean flag);
    }

    // TODO doesn't work
    public abstract void setDelegate(Delegate delegate);
}