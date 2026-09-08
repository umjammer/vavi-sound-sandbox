/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import org.rococoa.Foundation;
import org.rococoa.ID;


/**
 * AudioUnitParameterInfo.
 * <p>
 * what {@code kAudioUnitProperty_ParameterInfo} tells us about one parameter: how it is
 * called, what it is measured in, and the range it accepts.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-08 nsano initial version <br>
 */
public class AudioUnitParameterInfo extends Structure {

    /** AudioUnitParameterUnit */
    public enum Unit {
        generic,
        indexed,
        bool,
        percent,
        seconds,
        sampleFrames,
        phase,
        rate,
        hertz,
        cents,
        relativeSemiTones,
        midiNoteNumber,
        midiController,
        decibels,
        linearGain,
        degrees,
        equalPowerCrossfade,
        mixerFaderCurve1,
        pan,
        meters,
        absoluteCents,
        octaves,
        BPM,
        beats,
        milliseconds,
        ratio,
        customUnit;

        static Unit valueOf(int value) {
            return value >= 0 && value < values().length ? values()[value] : generic;
        }
    }

    // AudioUnitParameterOptions
    public static final int kAudioUnitParameterFlag_CFNameRelease = 1 << 4;
    public static final int kAudioUnitParameterFlag_IsWritable = 1 << 30;
    public static final int kAudioUnitParameterFlag_IsReadable = 1 << 29;

    /** {@code char name[52]}, most audio units still fill it in */
    public byte[] name = new byte[52];
    /** CFStringRef */
    public Pointer unitName;
    public int clumpID;
    /** CFStringRef, the modern name, filled in when {@link #name} is not */
    public Pointer cfNameString;
    /** AudioUnitParameterUnit */
    public int unit;
    public float minValue;
    public float maxValue;
    public float defaultValue;
    /** AudioUnitParameterOptions */
    public int flags;

    public AudioUnitParameterInfo() {
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("name",
                             "unitName",
                             "clumpID",
                             "cfNameString",
                             "unit",
                             "minValue",
                             "maxValue",
                             "defaultValue",
                             "flags");
    }

    /** the parameter id this info was read for, set by {@link AVAudioUnit#getParameters()} */
    private int id;

    void setId(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    /** the display name, {@link #name} when there is one, else {@link #cfNameString} */
    public String name() {
        int length = 0;
        while (length < name.length && name[length] != 0) {
            length++;
        }
        if (length > 0) {
            return new String(name, 0, length, StandardCharsets.UTF_8);
        }
        if (cfNameString != null) {
            ID cfName = ID.fromLong(Pointer.nativeValue(cfNameString));
            String value = Foundation.toString(cfName);
            if ((flags & kAudioUnitParameterFlag_CFNameRelease) != 0) {
                Foundation.cfRelease(cfName);
            }
            return value;
        }
        return String.valueOf(id);
    }

    public Unit unit() {
        return Unit.valueOf(unit);
    }

    @Override
    public String toString() {
        return "%d: %s [%s..%s] default %s %s".formatted(id, name(), minValue, maxValue, defaultValue, unit());
    }
}
