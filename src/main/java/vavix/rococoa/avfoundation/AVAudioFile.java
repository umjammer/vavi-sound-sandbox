/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import java.io.IOException;
import java.net.URI;
import com.sun.jna.Pointer;

import org.rococoa.ObjCClass;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.cocoa.foundation.NSDictionary;
import org.rococoa.cocoa.foundation.NSError;
import org.rococoa.cocoa.foundation.NSObject;
import org.rococoa.cocoa.foundation.NSURL;


/**
 * AVAudioFile.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2020/??/?? nsano initial version <br>
 */
public abstract class AVAudioFile extends NSObject {

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioFile", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioFile alloc();
    }

    /** for reading */
    public static AVAudioFile init(URI uri) throws IOException {
        NSURL fileURL = NSURL.URLWithString(uri.toString());
        AVAudioFile file = CLASS.alloc();
        ObjCObjectByReference error = new ObjCObjectByReference();
        file.initForReading_error(fileURL, error);
        if (error.getValueAs(NSError.class) != null) {
            throw new IOException(error.getValueAs(NSError.class).description());
        }
        return file;
    }

    /** for writing */
    public static AVAudioFile init(URI uri, NSDictionary settings, int commonFormat, boolean interleaved) throws IOException {
        NSURL fileURL = NSURL.URLWithString(uri.toString());
        AVAudioFile file = CLASS.alloc();
        ObjCObjectByReference error = new ObjCObjectByReference();
        file.initForWriting_settings_commonFormat_interleaved_error(fileURL, settings, commonFormat, interleaved, error);
        if (error.getValueAs(NSError.class) != null) {
            throw new IOException(error.getValueAs(NSError.class).description());
        }
        return file;
    }

    public abstract AVAudioFile initForReading_error(NSURL fileURL, ObjCObjectByReference outError);

    public abstract AVAudioFile initForWriting_settings_commonFormat_interleaved_error(NSURL fileURL, NSDictionary settings, int commonFormat, boolean interleaved, ObjCObjectByReference error);

    public abstract NSURL url();

    public abstract AVAudioFormat fileFormat();

    /**
     * Reads an entire audio buffer.
     * @return A value of true on a successful read.
     */
    public abstract boolean readIntoBuffer_error(AVAudioPCMBuffer buffer, ObjCObjectByReference outError);

    public abstract boolean readIntoBuffer_frameCount_error(AVAudioPCMBuffer buffer, int frames, ObjCObjectByReference outError);

    public abstract boolean writeFromBuffer_error(AVAudioPCMBuffer buffer, ObjCObjectByReference error);

    public abstract void close();

    public abstract AVAudioFormat processingFormat();
}
