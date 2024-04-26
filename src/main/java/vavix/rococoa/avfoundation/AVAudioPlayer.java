/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import java.io.IOException;
import java.net.URI;

import org.rococoa.ID;
import org.rococoa.ObjCClass;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSData;
import org.rococoa.cocoa.foundation.NSError;
import org.rococoa.cocoa.foundation.NSObject;
import org.rococoa.cocoa.foundation.NSURL;

import com.sun.jna.Callback;


/**
 * An object that plays audio data from a file or buffer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/??/?? umjammer initial version <br>
 */
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

    /** The audio player’s volume relative to other audio output. */
    public abstract float volume();

    /** This property supports values ranging from 0.0 for silence to 1.0 for full volume. */
    public abstract void setVolume(float volume);

    /** Changes the audio player’s volume over a duration of time. */
    public abstract void setVolume_fadeDuration(float volume, double duration);

    /** The audio player’s stereo pan position. */
    public abstract float pan();

    /**
     * Set this property value to position the audio in the stereo field.
     * Use a value of -1.0 to indicate full left, 1.0 for full right, and 0.0 for center.
     */
    public abstract void setPan(float pan);

    public abstract boolean enableRate();

    public abstract void setEnableRate(boolean enableRate);

    /** The audio player’s playback rate. */
    public abstract float rate();

    /**
     * To set an audio player’s playback rate, you must first enable the rate adjustment by setting
     * its enableRate property to YES.
     *
     * The default value of this property is 1.0, which indicates that audio playback occurs at standard speed.
     * This property supports values in the range of 0.5 for half-speed playback to 2.0 for double-speed playback.
     */
    public abstract void setRate(float rate);

    public abstract long numberOfLoops();

    public abstract void setNumberOfLoops(long numberOfLoops);

    public abstract AVAudioFormat format();

    public abstract double currentTime();

    public abstract void setCurrentTime(double currentTime);

    public abstract double duration();

    /** A protocol that defines the methods to respond to audio playback events and decoding errors. */
    public interface AVAudioPlayerDelegate extends Callback {
        /** Tells the delegate when the audio finishes playing. */
        void audioPlayerDidFinishPlaying_successfully(ID /* AVAudioPlayer */ player, boolean flag);
        /** Tells the delegate when an audio player encounters a decoding error during playback. */
        void audioPlayerDecodeErrorDidOccur_error(ID /* AVAudioPlayer */ player, ID /* NSError */ error);

        /** Tells the delegate when the system interrupts the audio player’s playback. */
        @Deprecated
        void audioPlayerBeginInterruption(ID /* AVAudioPlayer */ player);
        /** Tells the delegate when the audio session interruption ends. */
        @Deprecated
        void audioPlayerEndInterruption(ID /* AVAudioPlayer */ player);
        /** Tells the delegate when the audio session interruption ends with options. */
        @Deprecated
        void audioPlayerEndInterruption_withOptions(ID /* AVAudioPlayer */ player, long flags);
        /** Tells the delegate when the audio session interruption ends with flags. */
        @Deprecated
        void audioPlayerEndInterruption_withFlags(ID /* AVAudioPlayer */ player, long flags);
    }

    /** The delegate object for the audio player. */
    public abstract void setDelegate(ID /* AVAudioPlayerDelegate */ delegate);

    public void setDelegate(AVAudioPlayerDelegate delegate) {
        setDelegate(Rococoa.proxy(delegate).id());
    }
}