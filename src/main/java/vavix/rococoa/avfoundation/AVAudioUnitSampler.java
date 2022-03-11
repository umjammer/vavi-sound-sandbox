
package vavix.rococoa.avfoundation;

import java.net.URI;

import org.rococoa.ObjCClass;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.cocoa.foundation.NSError;
import org.rococoa.cocoa.foundation.NSURL;


public abstract class AVAudioUnitSampler extends AVAudioUnitMIDIInstrument {

    public static final int kAUSampler_DefaultPercussionBankMSB = 0x78;

    public static final int kAUSampler_DefaultMelodicBankMSB = 0x79;

    public static final int kAUSampler_DefaultBankLSB = 0x00;

    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioUnitSampler", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioUnitSampler alloc();
    }

    public static AVAudioUnitSampler newInstance() {
        AVAudioUnitSampler audioUnit = CLASS.alloc();
        return audioUnit.init();
    }

    public abstract AVAudioUnitSampler init();

    public boolean loadSoundBankInstrument(URI uri, int program, int bankMSB, int bankLSB) {
        NSURL bankURL = NSURL.URLWithString(uri.toString());
        ObjCObjectByReference outError = new ObjCObjectByReference();
        boolean r = loadSoundBankInstrumentAtURL_program_bankMSB_bankLSB_error(bankURL,
                                                                               (byte) program,
                                                                               (byte) bankMSB,
                                                                               (byte) bankLSB,
                                                                               outError);
        NSError error = outError.getValueAs(NSError.class);
        if (error != null) {
            return false;
        }
        return r;
    }

    public abstract boolean loadSoundBankInstrumentAtURL_program_bankMSB_bankLSB_error(NSURL bankURL,
                                                                                       byte program,
                                                                                       byte bankMSB,
                                                                                       byte bankLSB,
                                                                                       ObjCObjectByReference outError);

    public abstract float masterGain();

    /** The default value is 0.0 dB. The range of valid values is -90.0 dB to 12.0 dB. */
    public abstract void masterGain(float gain);
}
