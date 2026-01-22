/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import com.sun.jna.Pointer;

import org.rococoa.Foundation;
import org.rococoa.ObjCBlocks.BlockLiteral;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.appkit.NSApplication;
import org.rococoa.cocoa.appkit.NSViewController;
import org.rococoa.cocoa.appkit.NSWindow;
import org.rococoa.cocoa.foundation.NSError;
import vavi.util.ByteUtil;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.rococoa.avfoundation.AVAudioConverter.AVAudioConverterInputStatus;
import vavix.rococoa.avfoundation.AVAudioConverter.AVAudioConverterOutputStatus;
import vavix.rococoa.avfoundation.AVAudioFormat.AVAudioCommonFormat;
import vavix.rococoa.avfoundation.AudioStreamBasicDescription.AudioFormatFlag;
import vavix.rococoa.avfoundation.AudioStreamBasicDescription.AudioFormatID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.rococoa.ObjCBlocks.block;


/**
 * AVAudioConverterTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/09/30 umjammer initial version <br>
 */
@EnabledOnOs(OS.MAC)
@PropsEntity(url = "file:local.properties")
class AVFoundationTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static {
        AVFoundation.instance.toString(); // to make sure library is loaded
    }

    @Property(name = "sf2")
    String sf2name = System.getProperty("user.home") + "/Library/Audio/Sounds/Banks/Orchestra/default.sf2";

    @Property(name = "au.type")
    String auType;

    @Property(name = "au.vendor")
    String auVendor;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
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
     */
    @Test
    void test() throws Exception {
        URI uri = Path.of("src/test/resources", "test.m4a").toUri();
        AVAudioFile file = AVAudioFile.init(uri);
Debug.println(file);
        // AVAudioFile automatically decodes to a processing format (PCM).
        // To test AVAudioConverter, we convert from this processing format to our target format.
        AVAudioFormat inputFormat = file.processingFormat();
Debug.println(inputFormat);

        AVAudioFormat outputFormat = AVAudioFormat.init(AVAudioCommonFormat.pcmFormatInt16.ordinal(), inputFormat.sampleRate(), 2, true);
Debug.println(outputFormat);
        AVAudioConverter converter = AVAudioConverter.init(inputFormat, outputFormat);
Debug.println(converter);

        AVAudioPCMBuffer inBuffer = AVAudioPCMBuffer.init(inputFormat, 1024);
Debug.println(inBuffer);
        AVAudioPCMBuffer outBuffer = AVAudioPCMBuffer.init(outputFormat, 1024);
Debug.println(outBuffer);

        BlockLiteral inputBlock = block((AVAudioConverter.InputBlock) (block, inNumPackets, outStatus) -> {
//Debug.println("enter block: " + inNumPackets + ", " + outStatus);
            ObjCObjectByReference error = new ObjCObjectByReference();
            boolean r = file.readIntoBuffer_error(inBuffer, error);
//Debug.println("readIntoBuffer: " + r + ", " + error.getValueAs(NSError.class));
            if (r && error.getValueAs(NSError.class) == null) {
                if (inBuffer.frameLength() == 0) {
                    outStatus.setValue(AVAudioConverterInputStatus.endOfStream.ordinal());
                    return null;
                }
                outStatus.setValue(AVAudioConverterInputStatus.haveData.ordinal());
                return inBuffer.id();
            } else {
                outStatus.setValue(AVAudioConverterInputStatus.endOfStream.ordinal());
                if (error.getValueAs(NSError.class) != null) {
Debug.println("error: " + error.getValueAs(NSError.class).description());
                }
                return null;
            }
        });

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        AVAudioConverterOutputStatus status;
        int count = 0;
        do {
            status = converter.convert(outBuffer, inputBlock);
            if (status == AVAudioConverterOutputStatus.haveData) {
                ByteBuffer bb = ByteBuffer.allocate(outBuffer.frameLength() * outputFormat.channelCount() * Short.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                ShortBuffer sb = bb.asShortBuffer();
                sb.put(outBuffer.int16ChannelData().getPointer(0).getShortArray(0, outBuffer.frameLength() * outputFormat.channelCount()));
                os.write(bb.array());
                count += outBuffer.frameLength();
System.out.println("count: " + count);
            }
        } while (status == AVAudioConverterOutputStatus.haveData);
Debug.println("Total frames converted: " + count);

        os.flush();
        os.close();

        Path out = Path.of("tmp", "avf_test.wav");
        AudioFormat format = new AudioFormat((float) inputFormat.sampleRate(), 16, 2, true, false);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(os.toByteArray()), format, os.size() / format.getFrameSize()), Type.WAVE, out.toFile());

        Foundation.getRococoaLibrary().releaseObjCBlock(inputBlock.getPointer());
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
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test4() throws Exception {
        AVAudioEngine engine = AVAudioEngine.newInstance();
Debug.println(engine);

        AVAudioUnitSampler midiSynth = AVAudioUnitSampler.newInstance();
Debug.println(midiSynth);

        Path sf2 = Paths.get(sf2name);
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
//        description.componentManufacturer = ByteUtil.readBeInt(auVendor.getBytes(), 0);
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
    void test6() throws Exception {
        AudioComponentDescription description = new AudioComponentDescription();
        description.componentType = AudioComponentDescription.kAudioUnitType_MusicDevice;
//        description.componentSubType = AudioComponentDescription.kAudioUnitSubType_DLSSynth;
//        description.componentManufacturer = ByteUtil.readBeInt(auVendor.getBytes(), 0);
        int r = AudioToolbox.instance.AudioComponentCount(description);
Debug.println("AudioComponentCount: " + r);

        for (AVAudioUnitComponent c : AVAudioUnitComponentManager.shared().components(description)) {
Debug.println("AVAudioUnitComponent: " + c.audioComponentDescription() + ", " + c.name() + ", " + c.manufacturerName() + ", " + c.versionString() + ", " + c.hasMIDIInput() + ", " + c.hasCustomView() + ", " + c.isSandboxSafe());
        }
    }

    /**
     * AudioUnit instantiation
     */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test7() throws Exception {
        AVAudioUnitComponentManager manager = AVAudioUnitComponentManager.shared();

        AudioComponentDescription description = new AudioComponentDescription();
        description.componentType = AudioComponentDescription.kAudioUnitType_MusicDevice;
        description.componentSubType = ByteUtil.readBeInt("mc5p".getBytes(), 0);
        description.componentManufacturer = ByteUtil.readBeInt("Ftcr".getBytes(), 0);
        description.componentFlags = 0;
        description.componentFlagsMask = 0;

        List<AVAudioUnitComponent> list = manager.components(description);
        if (list.isEmpty()) throw new IllegalArgumentException("no such desc: " + auType + ":" + auVendor);

        AudioComponentDescription desc = list.getFirst().audioComponentDescription();
Debug.println("Attempting to instantiate: " + desc);

        AUAudioUnit audioUnit = AUAudioUnit.instantiate(desc, 0);
Debug.println("AudioUnit: " + audioUnit.description());

        NSApplication app = NSApplication.sharedApplication();
        app.setActivationPolicy(0); // NSApplicationActivationPolicyRegular
        app.finishLaunching();
        app.activate();

        BlockLiteral completionHandle = block((AUAudioUnit.AUViewControllerBase) (block, viewControllerId) -> {
Debug.println("block enter: " + viewControllerId);
            NSViewController vc = Rococoa.wrap(viewControllerId, NSViewController.class);
Debug.println(vc);

            Foundation.runOnMainThread(() -> {
                NSWindow window = NSWindow.windowWithContentViewController(vc);
                window.center();
                window.makeKeyAndOrderFront(null);
            });
        });

        audioUnit.requestViewControllerWithCompletionHandler(completionHandle);

        Thread.sleep(20000);

        Foundation.getRococoaLibrary().releaseObjCBlock(completionHandle.getPointer());
    }

    @Test
    void test8() throws Exception {
        for (float f = .0f; f <= 1.1f; f += .1f) {
            float dB = (float) (Math.log(f) / Math.log(10.0) * 20.0);
            System.out.printf("gain: %03.2f, dB: %03.0f%n", f, dB);
        }
    }
}
