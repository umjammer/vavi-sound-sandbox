/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.rococoa.RunOnMainThread;

import com.sun.jna.CallbackThreadInitializer;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import vavi.util.ByteUtil;
import vavi.util.Debug;

import vavix.rococoa.avfoundation.AVAudioConverter.AVAudioConverterInputStatus;
import vavix.rococoa.avfoundation.AVAudioConverter.AVAudioConverterOutputStatus;
import vavix.rococoa.avfoundation.AVAudioFormat.AVAudioCommonFormat;
import vavix.rococoa.avfoundation.AudioStreamBasicDescription.AudioFormatFlag;
import vavix.rococoa.avfoundation.AudioStreamBasicDescription.AudioFormatID;


/**
 * AVAudioConverterTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/09/30 umjammer initial version <br>
 */
@EnabledOnOs(OS.MAC)
class AVFoundationTest {

    static {
        AVFoundation.instance.toString(); // to make sure library is loaded
    }

    /** {@link AVAudioFormat#init(AudioStreamBasicDescription)} */
    @Test
    @DisabledIfSystemProperty(named = "os.arch", matches = "x86_64")
    void test01() {
        AudioStreamBasicDescription outDesc = new AudioStreamBasicDescription();
        outDesc.mSampleRate = 44100.0;
        outDesc.mFormatID = AudioFormatID.kAudioFormatLinearPCM.id;
        outDesc.mFormatFlags = EnumSet.noneOf(AudioFormatFlag.class).stream().distinct().mapToInt(e -> e.value).sum();
        outDesc.mBytesPerPacket = 0;
        outDesc.mFramesPerPacket = 0;
        outDesc.mBytesPerFrame = 0;
        outDesc.mChannelsPerFrame = 2;
        outDesc.mBitsPerChannel = 0;
        outDesc.mReserved = 0;
Debug.println(outDesc);
        AVAudioFormat outputFormat = AVAudioFormat.init(outDesc);
Debug.println(outputFormat);
    }

    /**
     * convert
     *
     * TODO not work
     */
    @Test
    @Disabled
    void test() throws Exception {
        URI uri = URI.create("file:///Users/nsano/Music/0/out.m4a");
        AVAudioFile file = AVAudioFile.init(uri);
Debug.println(file);
        AVAudioFormat inputFormat = file.fileFormat();
Debug.println(inputFormat);

        AVAudioFormat outputFormat = AVAudioFormat.init(AVAudioCommonFormat.pcmFormatInt16, inputFormat.sampleRate(), 2, true);
Debug.println(outputFormat);
        AVAudioConverter converter = AVAudioConverter.init(inputFormat, outputFormat);
Debug.println(converter);

        AVAudioCompressedBuffer inBuffer = AVAudioCompressedBuffer.init(inputFormat, 1024, converter.maximumOutputPacketSize());
Debug.println(inBuffer);
        AVAudioPCMBuffer outBuffer = AVAudioPCMBuffer.init(outputFormat, 1024);
Debug.println(outBuffer);

        AVAudioConverter.InputBlock inputBlock = (inNumPackets, outStatus) -> {
            outStatus.setValue(AVAudioConverterInputStatus.haveData.ordinal());
            return inBuffer.id();
        };

        AVAudioConverterOutputStatus status = converter.convert(outBuffer, inputBlock);
//        boolean status = converter.convert(outBuffer, inBuffer);
Debug.println(status);
    }

    /**
     * AVAudioUnitMIDIInstrument (kAudioUnitSubType_MIDISynth) w/ AudioToolbox#MusicDeviceMIDIEvent
     */
    @Test
    @DisabledIfSystemProperty(named = "os.arch", matches = "x86_64")
    void test2() throws Exception {
        AVAudioEngine engine = AVAudioEngine.newInstance();
Debug.println(engine);
        AudioComponentDescription description = new AudioComponentDescription();
        description.componentType = AudioComponentDescription.kAudioUnitType_MusicDevice;
        description.componentSubType = AudioComponentDescription.kAudioUnitSubType_MIDISynth;
        description.componentManufacturer = AudioComponentDescription.kAudioUnitManufacturer_Apple;
        description.componentFlags = 0;
        description.componentFlagsMask = 0;

        AVAudioUnitMIDIInstrument midiSynth = AVAudioUnitMIDIInstrument.init(description);
Debug.println(midiSynth);

        engine.attachNode(midiSynth);
        engine.connect_to_format(midiSynth, engine.mainMixerNode(), null);
        engine.prepare();
        boolean r = engine.start();
Debug.println("stated: " + r);

        int channel = 0;
        int noteCommand = 0x90 | channel;
        int pcCommand = 0xC0 | channel;
        int status;

        int patch = 46;
        int pitch = 83; // 36 - 100
        status = AudioToolbox.instance.MusicDeviceMIDIEvent(midiSynth.audioUnit(), pcCommand, patch, 0, 0);
Debug.println(status);
        status = AudioToolbox.instance.MusicDeviceMIDIEvent(midiSynth.audioUnit(), noteCommand, pitch, 64, 0);
Debug.println(status);

        Thread.sleep(2000);

        status = AudioToolbox.instance.MusicDeviceMIDIEvent(midiSynth.audioUnit(), noteCommand, pitch, 0, 0);
Debug.println(status);
    }

    /**
     * AVAudioUnitMIDIInstrument (kAudioUnitSubType_MIDISynth) w/ it's own methods.
     *
     * TODO w/ sound font
     */
    @Test
    @DisabledIfSystemProperty(named = "os.arch", matches = "x86_64")
    void test3() throws Exception {
        AVAudioEngine engine = AVAudioEngine.newInstance();
Debug.println(engine);
        AudioComponentDescription description = new AudioComponentDescription();
        description.componentType = AudioComponentDescription.kAudioUnitType_MusicDevice;
        description.componentSubType = AudioComponentDescription.kAudioUnitSubType_MIDISynth;
        description.componentManufacturer = AudioComponentDescription.kAudioUnitManufacturer_Apple;
        description.componentFlags = 0;
        description.componentFlagsMask = 0;

        AVAudioUnitMIDIInstrument midiSynth = AVAudioUnitMIDIInstrument.init(description);
Debug.println(midiSynth);

//        String sf2name = "SGM-V2.01.sf2";
//        Path sf2 = Paths.get(System.getProperty("user.home"), "/Library/Audio/Sounds/Banks/Orchestra", sf2name);
//        NSURL bankURL = NSURL.CLASS.fileURLWithPath(sf2.toString());
//        int status = AudioToolbox.instance.AudioUnitSetProperty(
//                                  midiSynth.audioUnit(),
//                                  AudioUnitPropertyID.kMusicDeviceProperty_SoundBankURL.id,
//                                  AudioUnitScope.kAudioUnitScope_Global.ordinal(),
//                                  0,
//                                  bankURL.getPointer(),
//                                  bankURL.CLASS.sf2.length());



        engine.attachNode(midiSynth);
        engine.connect_to_format(midiSynth, engine.mainMixerNode(), null);
        engine.prepare();
        boolean r = engine.start();
Debug.println("stated: " + r);

        int channel = 0;

        int patch = 46;
        int pitch = 75; // 36 - 100
        midiSynth.sendProgramChange(patch, channel);
        midiSynth.startNote(pitch, 64, channel);

        Thread.sleep(2000);

        midiSynth.stopNote(pitch, channel);
    }

    /**
     * AVAudioUnitMIDIInstrument (kAudioUnitSubType_DLSSynth)
     */
    @Test
    @DisabledIfSystemProperty(named = "os.arch", matches = "x86_64")
    void test31() throws Exception {
        AVAudioEngine engine = AVAudioEngine.newInstance();
Debug.println(engine);
        AudioComponentDescription description = new AudioComponentDescription();
        description.componentType = AudioComponentDescription.kAudioUnitType_MusicDevice;
        description.componentSubType = AudioComponentDescription.kAudioUnitSubType_DLSSynth;
        description.componentManufacturer = AudioComponentDescription.kAudioUnitManufacturer_Apple;
        description.componentFlags = 0;
        description.componentFlagsMask = 0;

        AVAudioUnitMIDIInstrument midiSynth = AVAudioUnitMIDIInstrument.init(description);
Debug.println(midiSynth);

        engine.attachNode(midiSynth);
        engine.connect_to_format(midiSynth, engine.mainMixerNode(), null);
        engine.prepare();
        boolean r = engine.start();
Debug.println("stated: " + r);

        int channel = 0;

        int patch = 46;
        int pitch = 75; // 36 - 100
        midiSynth.sendProgramChange(patch, channel);
        midiSynth.startNote(pitch, 64, channel);

        Thread.sleep(2000);

        midiSynth.stopNote(pitch, channel);
    }

    /**
     * AVAudioUnitSampler w/ sound font
     */
    @Test
    @DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
    void test4() throws Exception {
        AVAudioEngine engine = AVAudioEngine.newInstance();
Debug.println(engine);

        AVAudioUnitSampler midiSynth = AVAudioUnitSampler.newInstance();
Debug.println(midiSynth);

        String sf2name = "SGM-V2.01.sf2";
        Path sf2 = Paths.get(System.getProperty("user.home"), "/Library/Audio/Sounds/Banks/Orchestra", sf2name);
        midiSynth.loadSoundBankInstrument(sf2.toUri(),
                                0,
                                AVAudioUnitSampler.kAUSampler_DefaultMelodicBankMSB,
                                AVAudioUnitSampler.kAUSampler_DefaultBankLSB);

        engine.attachNode(midiSynth);
        engine.connect_to_format(midiSynth, engine.mainMixerNode(), null);
        engine.prepare();
        boolean r = engine.start();
Debug.println("stated: " + r);

        int channel = 0;

        int patch = 46;
        int pitch = 75; // 36 - 100
        midiSynth.sendProgramChange(patch, channel);
        midiSynth.startNote(pitch, 64, channel);

        Thread.sleep(2000);

        midiSynth.stopNote(pitch, channel);
    }

    /**
     * list AudioComponent
     *
     * @see "/Library/Audio/Plug-Ins/Components/Foo.component/Contents/Info.plist"
     */
    @Test
    void test5() throws Exception {
        AudioComponentDescription description = new AudioComponentDescription();
        description.componentType = AudioComponentDescription.kAudioUnitType_MusicDevice;
//        description.componentSubType = AudioComponentDescription.kAudioUnitSubType_DLSSynth;
        description.componentManufacturer = ByteUtil.readBeInt("Ftcr".getBytes(), 0);
        int r = AudioToolbox.instance.AudioComponentCount(description);
Debug.println("AudioComponentCount: " + r);

        Pointer comp = null;
        while ((comp = AudioToolbox.instance.AudioComponentFindNext(comp, description)) != null) {
            String name = AudioToolbox.AudioComponentName(comp);
Debug.println("AudioComponent: " + name);
//            AVAudioUnit audioUnit = AVAudioUnit.instantiate(description, AVAudioUnit.kAudioComponentInstantiation_LoadInProcess);
        }
    }

    /** list AVAudioUnitComponent */
    @Test
    @DisabledIfSystemProperty(named = "os.arch", matches = "x86_64")
    void test6() throws Exception {
        AudioComponentDescription description = new AudioComponentDescription();
        description.componentType = AudioComponentDescription.kAudioUnitType_MusicDevice;
//        description.componentSubType = AudioComponentDescription.kAudioUnitSubType_DLSSynth;
//        description.componentManufacturer = ByteUtil.readBeInt("Ftcr".getBytes(), 0);
        int r = AudioToolbox.instance.AudioComponentCount(description);
Debug.println("AudioComponentCount: " + r);

        for (AVAudioUnitComponent c : AVAudioUnitComponentManager.sharedInstance().components(description)) {
Debug.println("AVAudioUnitComponent: " + c.audioComponentDescription() + ", " + c.name() + ", " + c.manufacturerName() + ", " + c.versionString() + ", " + c.hasMIDIInput() + ", " + c.hasCustomView() + ", " + c.isSandboxSafe());
        }
    }

    /**
     * AUdioUnit instantiation
     * TODO not work
     *
     * @see "https://stackoverflow.com/questions/32386391/jna-objective-c-rococoa-calendar-callback"
     */
    @Disabled
    @Test
    void test7() throws Exception {
        AudioComponentDescription description = new AudioComponentDescription();
        description.componentType = AudioComponentDescription.kAudioUnitType_MusicDevice;
        description.componentSubType = ByteUtil.readBeInt("mc5p".getBytes(), 0);
        description.componentManufacturer = ByteUtil.readBeInt("Ftcr".getBytes(), 0);
        description.componentFlags = 0;
        description.componentFlagsMask = 0;

        AUAudioUnit audioUnit = AUAudioUnit.initWithComponentDescription(description);
Debug.println("AudioUnit: " + audioUnit.description());
        AUAudioUnit.CompletionHandlerCallback callback = new AUAudioUnit.CompletionHandlerCallback() {
            @Override
            @RunOnMainThread
            public void completionHandler(Pointer outCompletionHandler) {
//                NSViewController viewControler = outCompletionHandler.getValueAs(NSViewController.class);
//Debug.println("viewControler: " + viewControler);
//                viewControler.loadView();
            }
        };
        Native.setCallbackThreadInitializer(callback, new CallbackThreadInitializer(false, false, "Cocoa Dispatch Thread"));
        audioUnit.requestViewControllerWithCompletionHandler(callback);
    }

    @Test
    void test8() throws Exception {
        for (float f = .0f; f <= 1.1f; f += .1f) {
            float dB = (float) (Math.log(f) / Math.log(10.0) * 20.0);
            System.out.printf("gain: %03.2f, dB: %03.0f%n", f, dB);
        }
    }
}
