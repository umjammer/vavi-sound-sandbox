/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import org.rococoa.ID;
import org.rococoa.ObjCBlock;
import org.rococoa.ObjCBlocks.BlockLiteral;
import org.rococoa.ObjCClass;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.cocoa.foundation.NSError;
import org.rococoa.cocoa.foundation.NSInteger;
import org.rococoa.cocoa.foundation.NSObject;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;


/**
 * AVAudioConverter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2020/??/?? nsano initial version <br>
 */
public abstract class AVAudioConverter extends NSObject {

    public enum AVAudioConverterOutputStatus {
        haveData,
        inputRanDry,
        endOfStream,
        error
    }

    public enum AVAudioConverterInputStatus {
        haveData,
        noDataNow,
        endOfStream
    }

    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioConverter", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioConverter alloc();
    }

    public static AVAudioConverter init(AVAudioFormat fromFormat, AVAudioFormat toFormat) {
        AVAudioConverter converter = CLASS.alloc();
        converter = converter.initFromFormat_toFormat(fromFormat, toFormat);
        if (converter == null) {
            throw new IllegalArgumentException("in or out format might be wrong");
        }
        return converter;
    }

    public abstract AVAudioConverter initFromFormat_toFormat(AVAudioFormat fromFormat, AVAudioFormat toFormat);

    public abstract int convertToBuffer_error_withInputFromBlock(AVAudioBuffer outBuffer, ObjCObjectByReference error, BlockLiteral /* ^AVAudioConverterInputBlock */ inputBlock);

    public AVAudioConverterOutputStatus convert(AVAudioBuffer outBuffer, BlockLiteral inputBlock) {
        ObjCObjectByReference outError = new ObjCObjectByReference();
        int r = convertToBuffer_error_withInputFromBlock(outBuffer, outError, inputBlock);
        NSError error = outError.getValueAs(NSError.class);
        if (error != null) {
            throw new IllegalStateException(error.description());
        }
        return AVAudioConverterOutputStatus.values()[r];
    }

    public boolean convert(AVAudioPCMBuffer outputBuffer, AVAudioPCMBuffer inputBuffer) {
        ObjCObjectByReference outError = new ObjCObjectByReference();
        boolean r = convertToBuffer_fromBuffer_error(outputBuffer, inputBuffer, outError);
        NSError error = outError.getValueAs(NSError.class);
        if (error != null) {
            throw new IllegalStateException(error.description());
        }
        return r;
    }

    public abstract boolean convertToBuffer_fromBuffer_error(AVAudioPCMBuffer outputBuffer, AVAudioPCMBuffer inputBuffer, ObjCObjectByReference outError);

    public abstract int maximumOutputPacketSize();

    interface InputBlock extends ObjCBlock {
        ID apply(BlockLiteral block, int inNumPackets, LongByReference outStatus);
    }

    public abstract void reset();

    public abstract AVAudioFormat inputFormat();

    public abstract AVAudioFormat outputFormat();
}