
package vavix.rococoa.avfoundation;

import java.io.IOException;
import java.net.URI;

import org.rococoa.ObjCClass;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.cocoa.foundation.NSError;
import org.rococoa.cocoa.foundation.NSObject;
import org.rococoa.cocoa.foundation.NSURL;


public abstract class AVAudioFile extends NSObject {

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioFile", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioFile alloc();
    }

    public static AVAudioFile init(URI uri) throws IOException {
        NSURL fileURL = NSURL.CLASS.URLWithString(uri.toString());
        AVAudioFile file = CLASS.alloc();
        ObjCObjectByReference outError = new ObjCObjectByReference();
        file.initForReading_error(fileURL, outError);
        NSError error = outError.getValueAs(NSError.class);
        if (error != null) {
            throw new IOException(error.description());
        }
        return file;
    }

    public abstract AVAudioFile initForReading_error(NSURL fileURL, ObjCObjectByReference outError);

    public abstract AVAudioFormat fileFormat();

    public abstract boolean readIntoBuffer_error(AVAudioPCMBuffer buffer, NSError outError);

    public abstract boolean readIntoBuffer_frameCount_error(AVAudioPCMBuffer buffer, int frames, NSError outError);

    public abstract boolean writeFromBuffer(AVAudioPCMBuffer buffer, NSError outError);
}