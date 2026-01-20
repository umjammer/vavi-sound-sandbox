//
//  sf2convert
//  SoundFont Conversion/Compression Utility
//
//  Copyright (C)
//  2010 Werner Schweer and others (MuseScore)
//  2015 Davy Triponney (Polyphone)
//  2017 Cognitone (Juce port, converter)
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License version 2.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//

package vavi.sound.sf;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import vavi.io.LittleEndianSeekableDataInputStream;
import vavi.io.LittleEndianSeekableDataOutputStream;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;
import static org.kc7bfi.jflac.sound.spi.FlacFileFormatType.FLAC;
import static org.tritonus.sampled.file.pvorbis.VorbisAudioFileWriter.OGG;
import static vavi.sound.sf.SFont.Generator.Gen_Instrument;
import static vavi.sound.sf.SFont.Generator.Gen_KeyRange;
import static vavi.sound.sf.SFont.Generator.Gen_VelRange;


/**
 * SFont.
 *
 * @author Werner Schweer and others (MuseScore)
 * @author Davy Triponney (Polyphone)
 * @author Cognitone (Juce port, converter)
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2025/04/25 umjammer port to java <br>
 */
class SFont {

    private static final Logger logger = getLogger(SFont.class.getName());

    private static String FOURCC(byte a, byte b, byte c, byte d) {
        return "%c%c%c%c".formatted((char) a, (char) b, (char) c, (char) d);
    }

    // Disable this, if you don't want to use the Juce Vorbis code
    static final int USE_JUCE_VORBIS = 1;

    // Enable this, if compression format is set individually per sample (not yet possible)
    static final int USE_MULTIPLE_COMPRESSION_FORMATS = 0;

    //
    // sfVersionTag
    //
    enum FileType {
        SF2Format,
        SF3Format,
        SF4Format
    }

    static class sfVersionTag {

        int major;
        int minor;
    }

    enum Generator {
        Gen_StartAddrOfs, Gen_EndAddrOfs, Gen_StartLoopAddrOfs,
        Gen_EndLoopAddrOfs, Gen_StartAddrCoarseOfs, Gen_ModLFO2Pitch,
        Gen_VibLFO2Pitch, Gen_ModEnv2Pitch, Gen_FilterFc, Gen_FilterQ,
        Gen_ModLFO2FilterFc, Gen_ModEnv2FilterFc, Gen_EndAddrCoarseOfs,
        Gen_ModLFO2Vol, Gen_Unused1, Gen_ChorusSend, Gen_ReverbSend, Gen_Pan,
        Gen_Unused2, Gen_Unused3, Gen_Unused4,
        Gen_ModLFODelay, Gen_ModLFOFreq, Gen_VibLFODelay, Gen_VibLFOFreq,
        Gen_ModEnvDelay, Gen_ModEnvAttack, Gen_ModEnvHold, Gen_ModEnvDecay,
        Gen_ModEnvSustain, Gen_ModEnvRelease, Gen_Key2ModEnvHold,
        Gen_Key2ModEnvDecay, Gen_VolEnvDelay, Gen_VolEnvAttack,
        Gen_VolEnvHold, Gen_VolEnvDecay, Gen_VolEnvSustain, Gen_VolEnvRelease,
        Gen_Key2VolEnvHold, Gen_Key2VolEnvDecay, Gen_Instrument,
        Gen_Reserved1, Gen_KeyRange, Gen_VelRange,
        Gen_StartLoopAddrCoarseOfs, Gen_Keynum, Gen_Velocity,
        Gen_Attenuation, Gen_Reserved2, Gen_EndLoopAddrCoarseOfs,
        Gen_CoarseTune, Gen_FineTune, Gen_SampleId, Gen_SampleModes,
        Gen_Reserved3, Gen_ScaleTune, Gen_ExclusiveClass, Gen_OverrideRootKey,
        Gen_Dummy
    }

    enum Transform {
        Linear(0),
        AbsoluteValue(2);
        final int v;

        Transform(int v) {
            this.v = v;
        }
    }

    /**
     * Bit-masked SampleType, extended with flags
     * for compression (See SF2 spec for details)
     */
    enum SampleType {
        Mono(1),
        Right(2),
        Left(4),
        Linked(8),        // Compression flags
        TypeVorbis(16),  // compatible with FluidSynth/MuseScore
        TypeFlac(32),        // ROM sample flag
        Rom(0x8000);
        final int v;

        SampleType(int v) {
            this.v = v;
        }
    }

    enum SampleCompression {
        Raw,
        Vorbis,
        Flac
    }

    //
    // ModulatorList
    //
    static class ModulatorList {

        public ModulatorList() {
        }

        int src;
        Generator dst;
        int amount;
        int amtSrc;
        Transform transform;
    }

    //
    // GeneratorList
    //
    static class /* union */ GeneratorAmount {

        short word;
        Byte bytes = new Byte();

        static class Byte {

            byte lo, hi;
        }
    }

    static class GeneratorList {

        public GeneratorList() {
        }

        Generator gen;
        GeneratorAmount amount = new GeneratorAmount();
    }

    //
    // Zone
    //
    static class Zone {

        public
        int instrumentIndex;
        List<GeneratorList> generators = new ArrayList<>();
        List<ModulatorList> modulators = new ArrayList<>();
    }

    //
    // Preset
    //
    static class Preset {

        public Preset() {
            name = null;
            preset = 0;
            bank = 0;
            presetBagNdx = 0;
            library = 0;
            genre = 0;
            morphology = 0;
        }

        String name;
        int preset;
        int bank;
        int presetBagNdx; // used only for read
        int library;
        int genre;
        int morphology;

        List<Zone> zones = new ArrayList<>();
    }

    //
    // Instrument
    //
    static class Instrument {

        public Instrument() {
            index = 0;
            name = null;
        }

        int index;        // used only for reading
        String name;
        List<Zone> zones = new ArrayList<>();
    }

    //
    // Sample
    //

    /** Optional meta data for verification of samples after decompression */

    static class SampleMeta {

        public SampleMeta() {
            name = "";
            samples = 0;
            loopstart = 0;
            loopend = 0;
        }

        String name;
        int samples;   // original number of samples
        int loopstart; // Relative
        int loopend;
    }

    // Size in bytes for file positioning - critical
    static final int SampleMetaSize = 32;

    /**
     * Offsets start/end are absolute from start of chunk, measured in
     * samples or bytes (depending on compression format). Loop points
     * are absolute in the file (SF2 only), but turn into relative offsets
     * from start after loaded into RAM. This is to support Vorbis and Flac
     * compression, which unpredictably changes offsets in the file.
     */

    static class Sample {

        public Sample() {
            name = "";
            start = 0;
            end = 0;
            loopstart = 0;
            loopend = 0;
            samplerate = 0;
            origpitch = 0;
            pitchadj = 0;
            sampleLink = 0;
            sampletype = SampleType.Mono.v;
            byteDataSize = 0;
            byteData = null;
            sampleDataSize = 0;
            sampleData = null;
            meta = null;
            // All members are required to be all-zero, for a clean Sample instance is used as terminator in shdr chunk!
        }

        /**
         * Getting the number of samples is a bit shaky, because this is
         * derived from start/end offsets, which are subject to change when
         * written to a file that uses compression. Luckily, once the sample
         * data was loaded (and/or decompressed), we know for sure.
         */
        public final int numSamples() {
            if (sampleData != null)
                return sampleDataSize;
            else
                return end - start;
        }

        public SampleCompression getCompressionType() {
            if (sampletype == SampleType.TypeVorbis.v) return SampleCompression.Vorbis;
            if (sampletype == SampleType.TypeFlac.v) return SampleCompression.Flac;
            return SampleCompression.Raw;
        }

        public void setCompressionType(SampleCompression c) {
            switch (c) {
                case SampleCompression.Vorbis:
                case SampleCompression.Flac:
                    sampletype |= SampleType.values()[c.ordinal()].v;
                    break;
                case SampleCompression.Raw:
                    sampletype &= ~((int) (SampleType.TypeVorbis.v + SampleType.TypeFlac.v));
                    break;
            }
        }

        public void dropSampleData() {
            sampleData = null;
            sampleDataSize = 0;
        }

        public void dropByteData() {
            byteData = null;
            byteDataSize = 0;
        }

        /**
         * Since sample & loop offsets are 'repurposed' in compressed files,
         * this optional meta data preserves them, so we can verify if
         * decompression worked properly after load.
         */
        public SampleMeta createMeta() {
            meta = new SampleMeta();
            meta.name = name;
            meta.samples = numSamples();
            meta.loopstart = loopstart;
            meta.loopend = loopend;
            return meta;
        }

        /**
         * Verify if sample was properly restored after decompression.
         */
        public boolean checkMeta() {
            if (meta == null)
                return true;

            return meta.samples == numSamples()
                    && (meta.loopend - meta.loopstart) == (loopend - loopstart);
        }

        String name;
        int start;
        int end;
        int loopstart;
        int loopend;
        int samplerate;
        int origpitch;
        int pitchadj;
        int sampleLink;
        int sampletype;
        // Raw byte data, used for compression i/o
        int byteDataSize;
        byte[] byteData;
        // Native SF2 sample data, after decompression
        int sampleDataSize;
        short[] sampleData;

        SampleMeta meta;
    }

    //
    // SoundFont
    //
    static class SoundFont {

//#if ! USE_JUCE_VORBIS

        /** This is a hack to simplify static Ogg callbacks for decoding */
        public static class CallbackData {

            Sample decodeSample;
            int decodePosition;
        }
//#endif

        public SoundFont(final File filename) {
            _path = filename;
            _engine = null;
            _name = null;
            _date = null;
            _comment = null;
            _tools = null;
            _creator = null;
            _product = null;
            _copyright = null;
            _infile = null;
            _outfile = null;
            _fileFormatIn = FileType.SF2Format;
            _fileFormatOut = FileType.SF2Format;
            _fileSizeIn = 0;
            _fileSizeOut = 0;
//            _manager = null;

//            _manager.registerBasicFormats();
//            _audioFormatVorbis = (OggVorbisAudioFormat) (_manager.findFormatForFileExtension("ogg"));
//            _audioFormatFlac = (FlacAudioFormat) (_manager.findFormatForFileExtension("flac"));

//            assert _audioFormatVorbis != null;
//            assert _audioFormatFlac != null;

//            _qualityOptionsVorbis = _audioFormatVorbis.getQualityOptions();
//            _qualityOptionsFlac = _audioFormatFlac.getQualityOptions();

            // DEBUG: Use this snippet to learn about quality options
//            logger.log(Level.DEBUG, "Vorbis");
//            for (int i = 0; i < _qualityOptionsVorbis.size(); i++)
//                logger.log(Level.DEBUG, i + ": " + _qualityOptionsVorbis.get(i));
//            logger.log(Level.DEBUG, "FLAC");
//            for (int i = 0; i < _qualityOptionsFlac.size(); i++)
//                logger.log(Level.DEBUG, i + ": " + _qualityOptionsFlac.get(i));
        }

        public boolean read() throws IOException {
            _fileSizeIn = _path.length();
            _infile = new LittleEndianSeekableDataInputStream(Files.newByteChannel(_path.toPath()));

            try {
                int len = readFourcc("RIFF");
                readSignature("sfbk");
                len -= 4;
                while (len != 0) {
                    int len2 = readFourcc("LIST");
                    len -= (len2 + 8);
                    byte[] fourcc = new byte[4];
                    fourcc[0] = 0;
                    readSignature(fourcc);
                    len2 -= 4;
                    while (len2 != 0) {
                        fourcc[0] = 0;
                        int len3 = readFourcc(fourcc);
                        len2 -= (len3 + 8);
                        readSection(fourcc, len3);
                    }
                }
                // load sample data
                for (Sample sample : _samples)
                    readSampleData(sample);
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
                return false;
            }
            return true;
        }

        public boolean write(final File filename, FileType format, int quality) throws IOException {

            _outfile = new LittleEndianSeekableDataOutputStream(Files.newByteChannel(filename.toPath(), StandardOpenOption.TRUNCATE_EXISTING));
            _outfile.position(0);
            _fileFormatOut = format;

            // Add a warning that samples were decompressed from a lossy format
            if (_fileFormatIn == FileType.SF3Format && _fileFormatOut != _fileFormatIn) {
                logger.log(Level.INFO, "\n\n" + "CAUTION: Samples in this file were decompressed from a lossy format (Ogg Vorbis). If you want to edit this file, you should get the original uncompressed SF2 file.");
            }

            int riffLenPos;
            int listLenPos;
            try {
                _outfile.writeBytes("RIFF");
                riffLenPos = (int) _outfile.position();
                writeDword(0);
                _outfile.writeBytes("sfbk");

                _outfile.writeBytes("LIST");
                listLenPos = (int) _outfile.position();
                writeDword(0);
                _outfile.writeBytes("INFO");

                writeIfil();
                if (!_name.isEmpty()) writeStringSection("INAM", _name);
                if (!_engine.isEmpty()) writeStringSection("isng", _engine);
                if (!_product.isEmpty()) writeStringSection("IPRD", _product);
                if (!_creator.isEmpty()) writeStringSection("IENG", _creator);
                if (!_tools.isEmpty()) writeStringSection("ISFT", _tools);
                if (!_date.isEmpty()) writeStringSection("ICRD", _date);
                if (!_comment.isEmpty()) writeStringSection("ICMT", _comment);
                if (!_copyright.isEmpty()) writeStringSection("ICOP", _copyright);

                int pos = (int) _outfile.position();
                _outfile.position(listLenPos);
                writeDword(pos - listLenPos - 4);
                _outfile.position(pos);

                _outfile.writeBytes("LIST");
                listLenPos = (int) _outfile.position();
                writeDword(0);

                _outfile.writeBytes("sdta");
                writeSmpl(quality);
                pos = (int) _outfile.position();
                _outfile.position(listLenPos);
                writeDword(pos - listLenPos - 4);
                _outfile.position(pos);

                _outfile.writeBytes("LIST");
                listLenPos = (int) _outfile.position();
                writeDword(0);
                _outfile.writeBytes("pdta");

                writePhdr();
                writeBag("pbag", _pZones);
                writeMod("pmod", _pZones);
                writeGen("pgen", _pZones);
                writeInst();
                writeBag("ibag", _iZones);
                writeMod("imod", _iZones);
                writeGen("igen", _iZones);
                writeShdr();

                if (_fileFormatOut != FileType.SF2Format)
                    writeShdX();

                pos = (int) _outfile.position();
                _outfile.position(listLenPos);
                writeDword(pos - listLenPos - 4);
                _outfile.position(pos);

                int endPos = (int) _outfile.position();
                _outfile.position(riffLenPos);
                writeDword(endPos - riffLenPos - 4);

                _fileSizeOut = endPos;
            } catch (Exception s) {
                logger.log(Level.DEBUG, "write SF2 file failed: " + s);
                return false;
            }

            int percent = Math.toIntExact(Math.round(100 * (double) _fileSizeOut / (double) _fileSizeIn));
            logger.log(Level.DEBUG, "File size change: " + percent + "%");

            return true;
        }

        public void dumpPresets() {
            int idx = 0;

            for (Preset p : _presets) {
                logger.log(Level.DEBUG, "%03d %04x-%02x %s".formatted(idx, p.bank, p.preset, p.name));
                ++idx;
            }
        }

        private int readDword() throws IOException {
            return _infile.readInt();
        }

        private int readWord() throws IOException {
            return _infile.readUnsignedShort();
        }


        private int readShort() throws IOException {
            return _infile.readShort();
        }

        private int readByte() throws IOException {
            return _infile.readUnsignedByte();
        }

        private int readChar() throws IOException {
            return _infile.readUnsignedByte();
        }

        private int readFourcc(final String signature) throws IOException {
            readSignature(signature);
            return readDword();
        }

        private int readFourcc(byte[] signature) throws IOException {
            readSignature(signature);
            return readDword();
        }

        private void readSignature(String signature) throws IOException {
            byte[] fourcc = new byte[4];
            readSignature(fourcc);
            if (!Arrays.equals(fourcc, 0, 4, signature.getBytes(), 0, 4))
                throw new IOException("fourcc " + signature + " expected");
        }

        private void readSignature(byte[] signature) throws IOException {
            _infile.readFully(signature, 0, 4);
        }

        private void skip(int n) throws IOException {
            _infile.skipBytes(n);
        }

        private void readSection(final byte[] fourcc, int len) throws IOException {
logger.log(Level.DEBUG, "read: " + new String(fourcc) + ", " + len);
            switch (FOURCC(fourcc[0], fourcc[1], fourcc[2], fourcc[3])) {
                case "ifil":    // version
                    readVersion();
                    break;
                case "INAM":       // sound font name
                    _name = readString(len);
                    break;
                case "isng":       // target render engine
                    _engine = readString(len);
                    break;
                case "IPRD":       // product for which the bank was intended
                    _product = readString(len);
                    break;
                case "IENG": // sound designers and engineers for the bank
                    _creator = readString(len);
                    break;
                case "ISFT": // SoundFont tools used to create and alter the bank
                    _tools = readString(len);
                    break;
                case "ICRD": // date of creation of the bank
                    _date = readString(len);
                    break;
                case "ICMT": // comments on the bank
                    _comment = readString(len);
                    break;
                case "ICOP": // copyright message
                    _copyright = readString(len);
                    break;
                case "smpl": // the digital audio samples
                    _samplePos = _infile.position();
                    _sampleLen = len;
                    skip(len);
                    break;
                case "phdr": // preset headers
                    readPhdr(len);
                    break;
                case "pbag": // preset index list
                    readBag(len, _pZones);
                    break;
                case "pmod": // preset modulator list
                    readMod(len, _pZones);
                    break;
                case "pgen": // preset generator list
                    readGen(len, _pZones);
                    break;
                case "inst": // instrument names and indices
                    readInst(len);
                    break;
                case "ibag": // instrument index list
                    readBag(len, _iZones);
                    break;
                case "imod": // instrument modulator list
                    readMod(len, _iZones);
                    break;
                case "igen": // instrument generator list
                    readGen(len, _iZones);
                    break;
                case "shdr": // sample headers
                    readShdr(len);
                    break;

                case "shdX": // original sample lenghts & loops for verification (compressed formats only)
                    readShdX(len);
                    break;

                case "irom":    // sample rom
                case "iver":    // sample rom version
                default:
                    skip(len);
                    throw new IOException("unknown fourcc " + new String(fourcc));
            }
        }

        private void readVersion() throws IOException {
            byte[] data = new byte[4];
            if (_infile.readNBytes(data, 0, 4) != 4)
                throw new IOException("unexpected end of file");
            _version.major = data[0] + (data[1] << 8);
            _version.minor = data[2] + (data[3] << 8);

            _fileFormatIn = FileType.SF2Format;
            if (_version.major == 3) _fileFormatIn = FileType.SF3Format;
            if (_version.major == 4) _fileFormatIn = FileType.SF4Format;
        }

        private String readString(int n) throws IOException {
            if (n == 0)
                return "";

            // Visual C++ doesn't allow a variable array size here
            if (n > 2014) n = 1024;
            byte[] data = new byte[n];

            if (_infile.readNBytes(data, 0, n) != n)
                throw new IOException("unexpected end of file");

            return new String(data).replace("\u0000", "");
        }

        private void readPhdr(int len) throws IOException {
            if (len < (38 * 2))
                throw new IOException("phdr too short");
            if ((len % 38) != 0)
                throw new IOException("phdr not a multiple of 38");
            int n = len / 38;
            if (n <= 1) {
                skip(len);
                return;
            }
            int index1 = 0, index2;
            for (int i = 0; i < n; ++i) {
                Preset preset = new Preset();
                preset.name = readString(20);
                preset.preset = readWord();
                preset.bank = readWord();
                index2 = readWord();
                preset.library = readDword();
                preset.genre = readDword();
                preset.morphology = readDword();
                if (index2 < index1)
                    throw new IOException("preset header indices not monotonic");
                if (i > 0) {
                    int n2 = index2 - index1;
                    while (n2-- != 0) {
                        Zone z = new Zone();
                        _presets.getLast().zones.add(z);
                        _pZones.add(z);
                    }
                }
                index1 = index2;
                _presets.add(preset);
            }
            _presets.removeLast();
        }

        private void readBag(int len, List<Zone> zones) throws IOException {
            if ((len % 4) != 0)
                throw new IOException("bag size not a multiple of 4");
            int gIndex2, mIndex2;
            int gIndex1 = readWord();
            int mIndex1 = readWord();
            len -= 4;

            for (Zone zone : zones) {
                gIndex2 = readWord();
                mIndex2 = readWord();
                len -= 4;
                if (len < 0)
                    throw new IOException("bag size too small");
                if (gIndex2 < gIndex1)
                    throw new IOException("generator indices not monotonic");
                if (mIndex2 < mIndex1)
                    throw new IOException("modulator indices not monotonic");
                int n = mIndex2 - mIndex1;
                while (n-- != 0)
                    zone.modulators.add(new ModulatorList());
                n = gIndex2 - gIndex1;
                while (n-- != 0)
                    zone.generators.add(new GeneratorList());
                gIndex1 = gIndex2;
                mIndex1 = mIndex2;
            }
        }

        private void readMod(int size, List<Zone> zones) throws IOException {
            for (Zone zone : zones) {
                for (int k = 0; k < zone.modulators.size(); k++) {
                    ModulatorList m = zone.modulators.get(k);

                    size -= 10;
                    if (size < 0)
                        throw new IOException("pmod size mismatch");
                    m.src = readWord();
                    m.dst = Generator.values()[readWord()];
                    m.amount = readShort();
                    m.amtSrc = readWord();
                    m.transform = Transform.values()[readWord()];
                }
            }
            if (size != 10)
                throw new IOException("modulator list size mismatch");
            skip(10);
        }

        private void readGen(int size, List<Zone> zones) throws IOException {
            if ((size % 4) != 0)
                throw new IOException("bad generator list size");

            for (Zone zone : zones) {
                size -= (zone.generators.size() * 4);
                if (size < 0)
                    break;

                for (int g = 0; g < zone.generators.size(); g++) {
                    GeneratorList gen = zone.generators.get(g);

                    gen.gen = Generator.values()[readWord()];
                    if (gen.gen == Gen_KeyRange || gen.gen == Gen_VelRange) {
                        gen.amount.bytes.lo = (byte) readByte();
                        gen.amount.bytes.hi = (byte) readByte();
                    } else if (gen.gen == Gen_Instrument)
                        gen.amount.word = (short) readWord();
                    else
                        gen.amount.word = (short) readWord();
                }
            }
            if (size != 4)
                throw new IOException("generator list size mismatch != 4: " + size);
            skip(size);
        }

        private void readInst(int size) throws IOException {
            int n1 = size / 22;
            int index1 = 0, index2;
            for (int i = 0; i < n1; ++i) {
                Instrument instrument = new Instrument();
                instrument.name = readString(20);
                index2 = readWord();
                if (index2 < index1)
                    throw new IOException("instrument header indices not monotonic");
                if (i > 0) {
                    int n = index2 - index1;
                    while (n-- != 0) {
                        Zone z = new Zone();
                        _instruments.getLast().zones.add(z);
                        _iZones.add(z);
                    }
                }
                index1 = index2;
                _instruments.add(instrument);
            }
            _instruments.removeLast();
        }

        private void readShdr(int size) throws IOException {
            int n = size / 46;
            for (int i = 0; i < n - 1; ++i) {
                Sample s = new Sample();

                s.name = readString(20);
                s.start = readDword();
                s.end = readDword();
                s.loopstart = readDword();
                s.loopend = readDword();
                s.samplerate = readDword();
                s.origpitch = readByte();
                s.pitchadj = readChar();
                s.sampleLink = readWord();
                s.sampletype = readWord();

                _samples.add(s);
            }
            skip(46);   // trailing record
        }

        /**
         * Non-standard extension: This optional chunk retains information on original
         * sample lengths & loops for later verification of a compressed file.
         */
        private void readShdX(int size) throws IOException {
            int n = size / SampleMetaSize;
            assert _samples.size() == n - 1;
            logger.log(Level.DEBUG, "Reading verification data for " + _samples.size() + " samples");

            for (int i = 0; i < n - 1; ++i) {
                SampleMeta m = _samples.get(i).createMeta();
                m.name = readString(20);
                m.samples = readDword();
                m.loopstart = readDword();
                m.loopend = readDword();
                // it is required that samples & meta headers be written in identical sequence!
                assert _samples.get(i).name.equals(m.name);
            }
            skip(SampleMetaSize);   // trailing record
        }

        private int readSampleData(Sample s) throws IOException, UnsupportedAudioFileException {
//#if USE_MULTIPLE_COMPRESSION_FORMATS
//          switch (s.getCompressionType()) {
//              case Raw:
//                  return readSampleDataRaw(s);
//              case Vorbis:
//                  return readSampleDataVorbis(s);
//              case Flac:
//                  return readSampleDataFlac(s);
//              default:
//                  break;
//          }
//#else
            if (_fileFormatIn == FileType.SF2Format)
                return readSampleDataRaw(s);

            if (_fileFormatIn == FileType.SF3Format)
                return readSampleDataVorbis(s);

            if (_fileFormatIn == FileType.SF4Format)
                return readSampleDataFlac(s);
//#endif
            assert false;
            return 0;
        }

        private int readSampleDataRaw(Sample s) throws IOException {
            // Offsets in SF2 are based on samples (short)
            _infile.position(_samplePos + (long) s.start * Short.BYTES);

            int numSamples = (s.end - s.start);
            s.sampleDataSize = numSamples;
            byte[] b = new byte[numSamples * Short.BYTES];
            s.sampleData = new short[numSamples];
            int read = _infile.readNBytes(b, 0, numSamples * Short.BYTES);
            ShortBuffer sb = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
            sb.get(s.sampleData);

            // normalize offsets & make loop relative
            s.loopstart -= s.start;
            s.loopend -= s.start;
            s.start = 0;
            s.end = numSamples;

            s.createMeta();

            return read;
        }

        private int readSampleDataVorbis(Sample s) throws IOException, UnsupportedAudioFileException {
            // Offsets in SF3 are bytes
            int numBytes = (s.end - s.start);
            s.byteDataSize = numBytes;
            s.byteData = new byte[numBytes];
            _infile.position(_samplePos + s.start);
            _infile.readNBytes(s.byteData, 0, numBytes);

//#if USE_JUCE_VORBIS

            ByteArrayInputStream input = new ByteArrayInputStream(s.byteData, 0, s.byteDataSize);
            AudioInputStream reader = AudioSystem.getAudioInputStream(input);
            if (reader == null)
                throw new IOException("Failed decoding Vorbis data!");
            byte[] buffer = reader.readAllBytes();
            int numSamples = buffer.length / Short.BYTES;

            // copy buffer to sampleData
            s.sampleDataSize = numSamples;
            s.sampleData = new short[numSamples];
            for (int i = 0; i < numSamples; i++)
                s.sampleData[i] = (short) Math.round(ByteUtil.readBeShort(buffer, i * Short.BYTES) * 32768.f);
//#else

//            decodeOggVorbis(s);
//            int numSamples = s.numSamples();

//#endif

            // normalize offsets & make loop relative
            s.start = 0;
            s.end = numSamples;
            // loop in file was already relative ...
            //s.loopstart -= s.start;
            //s.loopend   -= s.start;

            assert s.checkMeta();
            s.dropByteData();
            return numBytes;
        }

        private int readSampleDataFlac(Sample s) throws IOException, UnsupportedAudioFileException {
            // Offsets in SF4 are bytes
            int numBytes = (s.end - s.start);
            s.byteDataSize = numBytes;
            s.byteData = new byte[numBytes];
            _infile.position(_samplePos + s.start);
            _infile.readNBytes(s.byteData, 0, numBytes);

            ByteArrayInputStream input = new ByteArrayInputStream(s.byteData, 0, s.byteDataSize);
            AudioInputStream reader = AudioSystem.getAudioInputStream(input);
            if (reader == null)
                throw new IOException("Failed decoding FLAC data!");

            byte[] buffer = reader.readAllBytes();
            int numSamples = buffer.length / Short.BYTES;

            // copy buffer to sampleData
            s.sampleDataSize = numSamples;
            s.sampleData = new short[numSamples];
            for (int i = 0; i < numSamples; i++)
                s.sampleData[i] = (short) Math.round(ByteUtil.readBeShort(buffer, i * Short.BYTES) * 32768.f);

            // normalize offsets & make loop relative
            s.start = 0;
            s.end = numSamples;
            // loop in file was already relative ...
            //s.loopstart -= s.start;
            //s.loopend   -= s.start;

            s.dropByteData();
            assert s.checkMeta();

            return numBytes;
        }

        private void writeDword(int val) throws IOException {
            byte[] b = new byte[4];
            ByteUtil.writeBeInt(val, b);
            write(b, 4);
        }

        private void writeWord(short val) throws IOException {
            byte[] b = new byte[2];
            ByteUtil.writeBeShort(val, b);
            write(b, 2);
        }

        private void writeByte(byte val) throws IOException {
            write(new byte[] {val}, 1);
        }

        private void writeChar(char val) throws IOException {
            write(new byte[] {(byte) val}, 1);
        }

        private void writeShort(short val) throws IOException {
            write(ByteUtil.getLeBytes(val), 2);
        }

        private void write(final byte[] p, int n) throws IOException {
            write(p, n);
        }

        private void writeString(final String string, int size) throws IOException {
            // Visual C++ doesn't allow variable arrays
            final int limit = string.getBytes().length;
            byte[] name = new byte[limit];
            // Yes, there are better ways to port this ...
            if (limit > 0)
                System.arraycopy(string.getBytes(), 0, name, 0, limit);

            write(name, limit);
        }

        private void writeStringSection(final String fourcc, final String string) throws IOException {
            final byte[] s = string.getBytes();
            write(fourcc.getBytes(), 4);
            int nn = s.length + 1;
            int n = ((nn + 1) / 2) * 2;
            writeDword(n);
            write(s, nn);
            if ((n - nn) != 0) {
                char c = 0;
                writeChar(c);
            }
        }

        private void writePreset(int zoneIdx, final Preset preset) throws IOException {
            writeString(preset.name, 20);
            writeWord((short) preset.preset);
            writeWord((short) preset.bank);
            writeWord((short) zoneIdx);
            writeDword(preset.library);
            writeDword(preset.genre);
            writeDword(preset.morphology);
        }

        private void writeModulator(final ModulatorList m) throws IOException {
            writeWord((short) m.src);
            writeWord((short) m.dst.ordinal());
            writeShort((short) m.amount);
            writeWord((short) m.amtSrc);
            writeWord((short) m.transform.ordinal());
        }

        private void writeGenerator(final GeneratorList g) throws IOException {
            writeWord((short) g.gen.ordinal());
            if (g.gen == Gen_KeyRange || g.gen == Gen_VelRange) {
                writeByte(g.amount.bytes.lo);
                writeByte(g.amount.bytes.hi);
            } else if (g.gen == Gen_Instrument)
                writeWord(g.amount.word);
            else
                writeWord(g.amount.word);
        }

        private void writeInstrument(int zoneIdx, final Instrument instrument) throws IOException {
            writeString(instrument.name, 20);
            writeWord((short) zoneIdx);
        }

        private void writeIfil() throws IOException {
            writeString("ifil", 4);
            writeDword(4);
            byte[] data = new byte[4];
            if (_fileFormatOut == FileType.SF3Format) _version.major = 3;
            if (_fileFormatOut == FileType.SF4Format) _version.major = 4;
            data[0] = (byte) _version.major;
            data[1] = (byte) (_version.major >> 8);
            data[2] = (byte) _version.minor;
            data[3] = (byte) (_version.minor >> 8);
            write(data, 4);
        }

        private void writeSmpl(int quality) throws IOException {
            /* Write sample data chunk and update each Sample's metadata
               to reflect the actual written offsets */

            writeString("smpl", 4);
            int pos = (int) _outfile.position();
            writeDword(0);

            long offsetFromChunk = 0;
            switch (_fileFormatOut) {
                case SF2Format: // SF2
                {
                    for (int i = 0; i < _samples.size(); i++) {
                        Sample s = _samples.get(i);
                        int written = writeSampleDataPlain(s);

                        s.setCompressionType(SampleCompression.Raw);
                        // Offsets in SF2 format based on 'sample count'
                        s.start = (int) (offsetFromChunk / Short.BYTES);
                        offsetFromChunk += written;
                        s.end = (int) (offsetFromChunk / Short.BYTES);
                        // turn relative loop points to absolute, as SF2 format requires
                        s.loopstart += s.start;
                        s.loopend += s.start;
                    }
                    break;
                }
                case SF3Format: // SF3
                {
                    for (int i = 0; i < _samples.size(); i++) {
                        Sample s = _samples.get(i);
                        int written = writeSampleDataVorbis(s, quality);

                        s.setCompressionType(SampleCompression.Vorbis);
                        // Offsets in SF3 based on byte offset in file.
                        // Hack start/end of sample metadata to accommodate this:
                        s.start = (int) offsetFromChunk;
                        offsetFromChunk += written;
                        s.end = (int) offsetFromChunk;
                        // Important: keep relative loop offsets in file, so it can be restored after loading.
                        // Loop is already relative ...
                    }
                    break;
                }
                case SF4Format: // SF4
                {
                    for (int i = 0; i < _samples.size(); i++) {
                        Sample s = _samples.get(i);
                        int written = writeSampleDataFlac(s, quality);

                        s.setCompressionType(SampleCompression.Flac);
                        // Offsets in SF4 based on byte offset in file.
                        // Hack start/end of sample metadata to accommodate this:
                        s.start = (int) offsetFromChunk;
                        offsetFromChunk += written;
                        s.end = (int) offsetFromChunk;
                        // Important: keep relative loop offsets in file, so it can be restored after loading.
                        // Loop is already relative ...
                    }
                    break;
                }
            }
            int npos = (int) _outfile.position();
            _outfile.position(pos);
            writeDword(npos - pos - 4);
            _outfile.position(npos);
        }

        private void writePhdr() throws IOException {
            writeString("phdr", 4);
            int n = _presets.size();
            writeDword((n + 1) * 38);
            int zoneIdx = 0;

            for (final Preset p : _presets) {
                writePreset(zoneIdx, p);
                zoneIdx += p.zones.size();
            }
            Preset p = new Preset();
            writePreset(zoneIdx, p);
        }

        private void writeBag(final String fourcc, List<Zone> zones) throws IOException {
            writeString(fourcc, 4);
            int n = zones.size();
            writeDword((n + 1) * 4);
            int gIndex = 0;
            int pIndex = 0;

            for (final Zone z : zones) {
                writeWord((short) gIndex);
                writeWord((short) pIndex);
                gIndex += z.generators.size();
                pIndex += z.modulators.size();
            }
            writeWord((short) gIndex);
            writeWord((short) pIndex);
        }

        private void writeMod(final String fourcc, final List<Zone> zones) throws IOException {
            writeString(fourcc, 4);
            int n = 0;

            for (final Zone zone : zones) {
                n += zone.modulators.size();
            }
            writeDword((n + 1) * 10);

            for (final Zone zone : zones) {
                for (int k = 0; k < zone.modulators.size(); k++) {
                    final ModulatorList m = zone.modulators.get(k);
                    writeModulator(m);
                }
            }
            // Empty terminator
            ModulatorList mod = new ModulatorList();
            writeModulator(mod);
        }

        private void writeGen(final String fourcc, List<Zone> zones) throws IOException {
            writeString(fourcc, 4);
            int n = 0;

            for (final Zone zone : zones) {
                n += zone.generators.size();
            }
            writeDword((n + 1) * 4);

            for (final Zone zone : zones) {
                for (int k = 0; k < zone.generators.size(); k++) {
                    final GeneratorList g = zone.generators.get(k);
                    writeGenerator(g);
                }
            }
            // Empty terminator
            GeneratorList gen = new GeneratorList();
            writeGenerator(gen);
        }

        private void writeInst() throws IOException {
            writeString("inst", 4);
            int n = _instruments.size();
            writeDword((n + 1) * 22);
            int zoneIdx = 0;

            for (final Instrument p : _instruments) {
                writeInstrument(zoneIdx, p);
                zoneIdx += p.zones.size();
            }
            Instrument p = new Instrument();
            writeInstrument(zoneIdx, p);
        }

        void writeShdr() throws IOException {
            writeString("shdr", 4);
            writeDword(46 * (_samples.size() + 1));

            for (Sample sample : _samples) writeShdrEach(sample);

            // Empty last sample as terminator
            Sample s = new Sample();
            writeShdrEach(s);
        }

        private void writeShdrEach(final Sample s) throws IOException {
            writeString(s.name, 20);
            writeDword(s.start);
            writeDword(s.end);
            writeDword(s.loopstart);
            writeDword(s.loopend);
            writeDword(s.samplerate);
            writeByte((byte) s.origpitch);
            writeChar((char) s.pitchadj);
            writeWord((short) s.sampleLink);
            writeWord((short) s.sampletype);
        }

        private void writeShdX() throws IOException {
            // need 100% meta data available
            for (Sample sample : _samples) {
                if (sample.meta == null)
                    return;
            }

            logger.log(Level.DEBUG, "Attaching verification data for " + _samples.size() + " samples");

            writeString("shdX", 4);
            writeDword(SampleMetaSize * (_samples.size() + 1));

            for (Sample sample : _samples) writeShdXEach(sample.meta);

            // Empty terminator
            SampleMeta m = new SampleMeta();
            writeShdXEach(m);
        }

        private void writeShdXEach(final SampleMeta m) throws IOException {
            assert m != null;

            int start = (int) _outfile.position();

            writeString(m.name, 20);
            writeDword(m.samples);
            writeDword(m.loopstart);
            writeDword(m.loopend);
            // Check SampleMetaSize is correct
            assert _outfile.position() - start == SampleMetaSize;
        }

        private int writeSampleDataPlain(Sample s) throws IOException {
            assert s.numSamples() > 0;

            int numBytes = s.numSamples() * Short.BYTES;
            byte[] b = new byte[numBytes];
            ShortBuffer sb = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
            write(b, numBytes);
            sb.get(s.sampleData);
            return numBytes;
        }

        private int writeSampleDataVorbis(Sample s, int quality) throws IOException {
            assert s.numSamples() > 0;
            final int numSamples = s.numSamples();
            int rawBytes = numSamples * Short.BYTES;
            int option = 4;

//#if USE_JUCE_VORBIS

            byte[] b = new byte[rawBytes];
            for (int i = 0; i < numSamples; i++)
                ByteUtil.writeBeShort((short) (s.sampleData[i] / 32768.f), b, i * Short.BYTES); // scale to unity

            /*
             0: 64 kbps
             1: 80 kbps
             2: 96 kbps
             3: 112 kbps
             4: 128 kbps
             5: 160 kbps
             6: 192 kbps
             7: 224 kbps
             8: 256 kbps
             9: 320 kbps
             10: 500 kbps */
            switch (quality) {
                case 0:
                    option = 5;
                    break; // Low quality
                case 1:
                    option = 8;
                    break; // Medium quality
                case 2:
                    option = 10;
                    break; // High quality
            }
//            assert option < _qualityOptionsVorbis.size();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            AudioFormat format = new AudioFormat(s.samplerate, 16, 1, true, true);
            AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(b), format, b.length);
            AudioSystem.write(ais, OGG, output);
            // writer MUST be deleted to properly flush & close ...

            int numBytes = output.size();
            write(output.toByteArray(), numBytes);

//#else  // USE_JUCE_VORBIS

//            ogg_stream_state os;
//            ogg_page og;
//            ogg_packet op;
//            vorbis_info vi;
//            vorbis_dsp_state vd;
//            vorbis_block vb;
//            vorbis_comment vc;
//
//            vorbis_info_init(vi);
//
//            float qualityF = 1.0f;
//            switch (quality) {
//                case 0:
//                    option = 5;
//                    qualityF = 0.2f;
//                    break; // Low quality
//                case 1:
//                    option = 8;
//                    qualityF = 0.6f;
//                    break; // Medium quality
//                case 2:
//                    option = 10;
//                    qualityF = 1.0f;
//                    break; // High quality
//            }
//
//            int ret = vorbis_encode_init_vbr(vi, 1, s.samplerate, qualityF);
//            if (ret) {
//                logger.log(Level.DEBUG, "vorbis init failed\n");
//                return false;
//            }
//
//            vorbis_comment_init(vc);
//            vorbis_analysis_init(vd, vi);
//            vorbis_block_init(vd, vb);
//            srand(time(null));
//            ogg_stream_init(os, rand());
//
//            ogg_packet header;
//            ogg_packet header_comm;
//            ogg_packet header_code;
//
//            vorbis_analysis_headerout(vd, vc, header, header_comm, header_code);
//            ogg_stream_packetin(os, header);
//            ogg_stream_packetin(os, header_comm);
//            ogg_stream_packetin(os, header_code);
//
//            byte[] obuf = new byte[1048576]; // 1024 * 1024
//            byte[] p = obuf;
//
//            for (; ; ) {
//                int result = ogg_stream_flush(os, og);
//                if (result == 0)
//                    break;
//                memcpy(p, og.header, og.header_len);
//                p += og.header_len;
//                memcpy(p, og.body, og.body_len);
//                p += og.body_len;
//            }
//
//            long i;
//            int page = 0;
//
//            for (; ; ) {
//                int bufflength = jmin(BLOCK_SIZE, numSamples - page * BLOCK_SIZE);
//                float[][] buffer = vorbis_analysis_buffer(vd, bufflength);
//                int j = 0;
//                int max = jmin((page + 1) * BLOCK_SIZE, numSamples);
//                for (i = page * BLOCK_SIZE; i < max; i++) {
//                    buffer[0][j] = s.sampleData[i] / 32768.f;
//                    // buffer[0][j] = ibuffer[i] / 35000.f; // HACK: attenuate samples due to libsndfile bug
//                    j++;
//                }
//
//                vorbis_analysis_wrote(vd, bufflength);
//
//                while (vorbis_analysis_blockout(vd, vb) == 1) {
//                    vorbis_analysis(vb, 0);
//                    vorbis_bitrate_addblock(vb);
//
//                    while (vorbis_bitrate_flushpacket(vd, op)) {
//                        ogg_stream_packetin(os, op);
//
//                        for (; ; ) {
//                            int result = ogg_stream_pageout(os, og);
//                            if (result == 0)
//                                break;
//                            memcpy(p, og.header, og.header_len);
//                            p += og.header_len;
//                            memcpy(p, og.body, og.body_len);
//                            p += og.body_len;
//                        }
//                    }
//                }
//                page++;
//                if ((max == numSamples) || !((numSamples - page * BLOCK_SIZE) > 0))
//                    break;
//            }
//
//            vorbis_analysis_wrote(vd, 0);
//
//            while (vorbis_analysis_blockout(vd, vb) == 1) {
//                vorbis_analysis(vb, 0);
//                vorbis_bitrate_addblock(vb);
//
//                while (vorbis_bitrate_flushpacket(vd, op)) {
//                    ogg_stream_packetin(os, op);
//
//                    for (; ; ) {
//                        int result = ogg_stream_pageout(os, og);
//                        if (result == 0)
//                            break;
//                        memcpy(p, og.header, og.header_len);
//                        p += og.header_len;
//                        memcpy(p, og.body, og.body_len);
//                        p += og.body_len;
//                    }
//                }
//            }
//
//            ogg_stream_clear(os);
//            vorbis_block_clear(vb);
//            vorbis_dsp_clear(vd);
//            vorbis_comment_clear(vc);
//            vorbis_info_clear(vi);
//
//            int numBytes = p - obuf;
//            write(obuf, numBytes);

//#endif // USE_JUCE_VORBIS

            int percent = Math.round(100.f * (float) numBytes / (float) rawBytes);
//            logger.log(Level.DEBUG, "Compressed " + _qualityOptionsVorbis.get(option) + ": " + s.name + " (" + percent + "%)");

            return numBytes;
        }

        private int writeSampleDataFlac(Sample s, int quality) throws IOException {
            assert s.numSamples() > 0;
            final int numSamples = s.numSamples();
            int rawBytes = numSamples * Short.BYTES;

            byte[] b = new byte[rawBytes];
            for (int i = 0; i < numSamples; i++)
                ByteUtil.writeBeShort((short) (s.sampleData[i] / 32768.f), b, i * Short.BYTES); // scale to unity

            int option = 8;
            /*
             0: 0 (Fastest)
             1: 1
             2: 2
             3: 3
             4: 4
             5: 5 (Default)
             6: 6
             7: 7
             8: 8 (Highest quality) */
            switch (quality) {
                case 0:
                    option = 1;
                    break; // Low quality
                case 1:
                    option = 5;
                    break; // Medium quality
                case 2:
                    option = 8;
                    break; // High quality
            }
//            assert option < _qualityOptionsFlac.size();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            AudioFormat format = new AudioFormat(s.samplerate, 16, 1, true, true);
            AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(b), format, b.length);
            AudioSystem.write(ais, FLAC, output);
            // writer MUST be deleted to properly flush & close ...

            int numBytes = output.size();
            write(output.toByteArray(), numBytes);

            int percent = Math.round(100.f * (float) numBytes / (float) rawBytes);
//            logger.log(Level.DEBUG, "Compressed FLAC " + _qualityOptionsFlac.get(option) + ": " + s.name + " (" + percent + "%)");

            return numBytes;
        }

//        boolean writeCSample(Sample s, int idx);

//#if ! USE_JUCE_VORBIS
//        private boolean decodeOggVorbis(Sample s) {
//            assert (s.numSamples() > 0);
//
//            int numBytes = s.numSamples() * Short.BYTES;
//            write((byte[]) s.sampleData, numBytes);
//            return numBytes;
//        }
//#endif

        // You may want to access these from your code, so make it a friend class */

        protected List<Preset> _presets = new ArrayList<>();
        protected List<Instrument> _instruments = new ArrayList<>();
        protected List<Sample> _samples = new ArrayList<>();

        private File _path;
        private SFont.sfVersionTag _version = new sfVersionTag();

        private String _engine;
        private String _name;
        private String _date;
        private String _comment;
        private String _tools;
        private String _creator;
        private String _product;
        private String _copyright;

        private long _samplePos;
        private long _sampleLen;

        private LittleEndianSeekableDataInputStream _infile;
        private LittleEndianSeekableDataOutputStream _outfile;

        private FileType _fileFormatIn, _fileFormatOut;
        private long _fileSizeIn, _fileSizeOut;

//        private AudioFormatManager _manager;
//        private OggVorbisAudioFormat _audioFormatVorbis;
//        private FlacAudioFormat _audioFormatFlac;
//        private List<String> _qualityOptionsVorbis;
//        private List<String> _qualityOptionsFlac;

        private List<Zone> _pZones = new ArrayList<>(); // owned by _presets after loading
        private List<Zone> _iZones = new ArrayList<>(); // owned by _instruments after loading
    }
}
