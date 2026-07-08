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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import vavi.io.LittleEndianSeekableDataInputStream;
import vavi.io.LittleEndianSeekableDataOutputStream;

import static java.lang.System.getLogger;
import static vavi.sound.sf.SFont.Generator.Gen_Instrument;
import static vavi.sound.sf.SFont.Generator.Gen_KeyRange;
import static vavi.sound.sf.SFont.Generator.Gen_VelRange;


/**
 * SFont. SoundFont 2 with the MuseScore/Polyphone compressed sample
 * extensions: SF3 (Ogg Vorbis) and SF4 (FLAC).
 *
 * @author Werner Schweer and others (MuseScore)
 * @author Davy Triponney (Polyphone)
 * @author Cognitone (Juce port, converter)
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2025/04/25 umjammer port to java <br>
 */
public class SFont {

    private static final Logger logger = getLogger(SFont.class.getName());

    private static String FOURCC(byte a, byte b, byte c, byte d) {
        return "%c%c%c%c".formatted((char) a, (char) b, (char) c, (char) d);
    }

    /** resolved by name against the installed audio spi (tritonus pvorbis) */
    public static final AudioFileFormat.Type OGG = new AudioFileFormat.Type("Vorbis", "ogg");

    /** resolved by name against the installed audio spi (tritonus pvorbis) */
    public static final AudioFormat.Encoding VORBIS = new AudioFormat.Encoding("VORBIS");

    /** resolved by name against the installed audio spi (jflac and family) */
    public static final AudioFileFormat.Type FLAC = new AudioFileFormat.Type("FLAC", "flac");

    /** resolved by name against the installed audio spi (jflac and family) */
    public static final AudioFormat.Encoding FLAC_ENC = new AudioFormat.Encoding("FLAC");

    //
    // sfVersionTag
    //
    public enum FileType {
        SF2Format,
        SF3Format,
        SF4Format
    }

    static class sfVersionTag {

        int major;
        int minor;
    }

    public enum Generator {
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

    public enum Transform {
        Linear(0),
        AbsoluteValue(2);
        public final int v;

        Transform(int v) {
            this.v = v;
        }
    }

    /**
     * Bit-masked SampleType, extended with flags
     * for compression (See SF2 spec for details)
     */
    public enum SampleType {
        Mono(1),
        Right(2),
        Left(4),
        Linked(8),
        // Compression flags
        TypeVorbis(16), // compatible with FluidSynth/MuseScore
        TypeFlac(32),
        // ROM sample flag
        Rom(0x8000);
        public final int v;

        SampleType(int v) {
            this.v = v;
        }
    }

    public enum SampleCompression {
        Raw,
        Vorbis,
        Flac
    }

    //
    // ModulatorList
    //
    public static class ModulatorList {

        public int src;
        public Generator dst;
        public int amount;
        public int amtSrc;
        public Transform transform;
    }

    //
    // GeneratorList
    //
    public static class /* union */ GeneratorAmount {

        public short word;
        public Byte bytes = new Byte();

        public static class Byte {

            public byte lo, hi;
        }
    }

    public static class GeneratorList {

        public Generator gen;
        public GeneratorAmount amount = new GeneratorAmount();
    }

    //
    // Zone
    //
    public static class Zone {

        public int instrumentIndex;
        public List<GeneratorList> generators = new ArrayList<>();
        public List<ModulatorList> modulators = new ArrayList<>();
    }

    //
    // Preset
    //
    public static class Preset {

        public String name;
        public int preset;
        public int bank;
        public int presetBagNdx; // used only for read
        public int library;
        public int genre;
        public int morphology;

        public List<Zone> zones = new ArrayList<>();
    }

    //
    // Instrument
    //
    public static class Instrument {

        public int index; // used only for reading
        public String name;
        public List<Zone> zones = new ArrayList<>();
    }

    //
    // Sample
    //

    /** Optional meta data for verification of samples after decompression */
    public static class SampleMeta {

        public String name = "";
        public int samples;   // original number of samples
        public int loopstart; // Relative
        public int loopend;
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
    public static class Sample {

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
            if ((sampletype & SampleType.TypeVorbis.v) != 0) return SampleCompression.Vorbis;
            if ((sampletype & SampleType.TypeFlac.v) != 0) return SampleCompression.Flac;
            return SampleCompression.Raw;
        }

        public void setCompressionType(SampleCompression c) {
            sampletype &= ~(SampleType.TypeVorbis.v | SampleType.TypeFlac.v);
            switch (c) {
                case Vorbis -> sampletype |= SampleType.TypeVorbis.v;
                case Flac -> sampletype |= SampleType.TypeFlac.v;
                case Raw -> { }
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

        /** Verify if sample was properly restored after decompression. */
        public boolean checkMeta() {
            if (meta == null)
                return true;

            return meta.samples == numSamples()
                    && (meta.loopend - meta.loopstart) == (loopend - loopstart);
        }

        public String name = "";
        public int start;
        public int end;
        public int loopstart;
        public int loopend;
        public int samplerate;
        public int origpitch;
        public int pitchadj;
        public int sampleLink;
        public int sampletype = SampleType.Mono.v;
        // Raw byte data, used for compression i/o
        public int byteDataSize;
        public byte[] byteData;
        // Native SF2 sample data, after decompression
        public int sampleDataSize;
        public short[] sampleData;

        public SampleMeta meta;
    }

    //
    // SoundFont
    //
    public static class SoundFont {

        public SoundFont(File filename) {
            _path = filename;
            _name = filename.getName();
        }

        /** reads from an already open channel, e.g. in-memory data */
        public SoundFont(SeekableByteChannel channel, String name) {
            _channel = channel;
            _name = name;
        }

        public void read() throws IOException {
            SeekableByteChannel channel = _channel != null ? _channel : Files.newByteChannel(_path.toPath());
            _fileSizeIn = channel.size();
            _infile = new LittleEndianSeekableDataInputStream(channel);

            int len = readFourcc("RIFF");
            readSignature("sfbk");
            len -= 4;
            while (len > 0) {
                int len2 = readFourcc("LIST");
                len -= (len2 + 8);
                byte[] fourcc = new byte[4];
                fourcc[0] = 0;
                readSignature(fourcc);
                len2 -= 4;
                while (len2 > 0) {
                    fourcc[0] = 0;
                    int len3 = readFourcc(fourcc);
                    len2 -= (len3 + 8);
                    readSection(fourcc, len3);
                }
            }
            // load sample data
            for (Sample sample : _samples) {
                try {
                    readSampleData(sample);
                } catch (UnsupportedAudioFileException e) {
                    throw new IOException("no audio decoder for " + _fileFormatIn, e);
                } catch (IOException | RuntimeException e) {
                    logger.log(Level.WARNING, "cannot decode sample: " + sample.name + ": " + e);
                    sample.sampleData = new short[0];
                    sample.sampleDataSize = 0;
                    sample.start = 0;
                    sample.end = 0;
                }
            }
        }

        public void write(File filename, FileType format, int quality) throws IOException {

            _outfile = new LittleEndianSeekableDataOutputStream(Files.newByteChannel(filename.toPath(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
            _outfile.position(0);
            _fileFormatOut = format;

            // Add a warning that samples were decompressed from a lossy format
            if (_fileFormatIn == FileType.SF3Format && _fileFormatOut != _fileFormatIn) {
                logger.log(Level.INFO, "CAUTION: Samples in this file were decompressed from a lossy format (Ogg Vorbis). If you want to edit this file, you should get the original uncompressed SF2 file.");
            }

            int riffLenPos;
            int listLenPos;

            _outfile.writeBytes("RIFF");
            riffLenPos = (int) _outfile.position();
            writeDword(0);
            _outfile.writeBytes("sfbk");

            _outfile.writeBytes("LIST");
            listLenPos = (int) _outfile.position();
            writeDword(0);
            _outfile.writeBytes("INFO");

            writeIfil();
            if (_name != null && !_name.isEmpty()) writeStringSection("INAM", _name);
            if (_engine != null && !_engine.isEmpty()) writeStringSection("isng", _engine);
            if (_product != null && !_product.isEmpty()) writeStringSection("IPRD", _product);
            if (_creator != null && !_creator.isEmpty()) writeStringSection("IENG", _creator);
            if (_tools != null && !_tools.isEmpty()) writeStringSection("ISFT", _tools);
            if (_date != null && !_date.isEmpty()) writeStringSection("ICRD", _date);
            if (_comment != null && !_comment.isEmpty()) writeStringSection("ICMT", _comment);
            if (_copyright != null && !_copyright.isEmpty()) writeStringSection("ICOP", _copyright);

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
            _outfile.flush();
            _outfile.origin().close();

            _fileSizeOut = endPos;

            if (_fileSizeIn > 0) {
                int percent = Math.toIntExact(Math.round(100 * (double) _fileSizeOut / (double) _fileSizeIn));
                logger.log(Level.DEBUG, "File size change: " + percent + "%");
            }
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

        private int readFourcc(String signature) throws IOException {
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

        private void readSection(byte[] fourcc, int len) throws IOException {
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

                case "shdX": // original sample lengths & loops for verification (compressed formats only)
                    readShdX(len);
                    break;

                case "irom":    // sample rom
                case "iver":    // sample rom version
                default:
                    logger.log(Level.DEBUG, "skipping fourcc " + new String(fourcc));
                    skip(len);
                    break;
            }
        }

        private void readVersion() throws IOException {
            byte[] data = new byte[4];
            if (_infile.readNBytes(data, 0, 4) != 4)
                throw new IOException("unexpected end of file");
            _version.major = (data[0] & 0xff) + ((data[1] & 0xff) << 8);
            _version.minor = (data[2] & 0xff) + ((data[3] & 0xff) << 8);

            _fileFormatIn = FileType.SF2Format;
            if (_version.major == 3) _fileFormatIn = FileType.SF3Format;
            if (_version.major == 4) _fileFormatIn = FileType.SF4Format;
        }

        private String readString(int n) throws IOException {
            if (n == 0)
                return "";

            byte[] data = new byte[n];
            if (_infile.readNBytes(data, 0, n) != n)
                throw new IOException("unexpected end of file");

            int end = 0;
            while (end < n && data[end] != 0)
                end++;
            return new String(data, 0, end, StandardCharsets.UTF_8);
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
                    m.transform = readWord() == Transform.AbsoluteValue.v ? Transform.AbsoluteValue : Transform.Linear;
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
                    throw new IOException("generator list too small");

                for (int g = 0; g < zone.generators.size(); g++) {
                    GeneratorList gen = zone.generators.get(g);

                    gen.gen = Generator.values()[readWord()];
                    if (gen.gen == Gen_KeyRange || gen.gen == Gen_VelRange) {
                        gen.amount.bytes.lo = (byte) readByte();
                        gen.amount.bytes.hi = (byte) readByte();
                    } else {
                        gen.amount.word = (short) readWord();
                    }
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
                s.pitchadj = (byte) readChar();
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
            logger.log(Level.DEBUG, "Reading verification data for " + _samples.size() + " samples");

            for (int i = 0; i < n - 1 && i < _samples.size(); ++i) {
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

        private void readSampleData(Sample s) throws IOException, UnsupportedAudioFileException {
            switch (_fileFormatIn) {
                case SF2Format -> readSampleDataRaw(s);
                case SF3Format -> readSampleDataCompressed(s);
                case SF4Format -> readSampleDataCompressed(s);
            }
        }

        private void readSampleDataRaw(Sample s) throws IOException {
            // Offsets in SF2 are based on samples (short)
            _infile.position(_samplePos + (long) s.start * Short.BYTES);

            int numSamples = (s.end - s.start);
            s.sampleDataSize = numSamples;
            byte[] b = new byte[numSamples * Short.BYTES];
            s.sampleData = new short[numSamples];
            _infile.readFully(b, 0, numSamples * Short.BYTES);
            // sample data in SF2 is little endian
            ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(s.sampleData);

            // normalize offsets & make loop relative
            s.loopstart -= s.start;
            s.loopend -= s.start;
            s.start = 0;
            s.end = numSamples;

            s.createMeta();
        }

        /** SF3 (Ogg Vorbis) and SF4 (FLAC): offsets are byte positions into the smpl chunk */
        private void readSampleDataCompressed(Sample s) throws IOException, UnsupportedAudioFileException {
            int numBytes = (s.end - s.start);
            s.byteDataSize = numBytes;
            s.byteData = new byte[numBytes];
            _infile.position(_samplePos + s.start);
            _infile.readFully(s.byteData, 0, numBytes);

            AudioInputStream encoded = AudioSystem.getAudioInputStream(
                    new ByteArrayInputStream(s.byteData, 0, s.byteDataSize));
            // decoders only accept enumerated sample rates, but the rate is
            // irrelevant for decoding, so hide it from the provider lookup
            AudioFormat ef = encoded.getFormat();
            AudioFormat lax = new AudioFormat(ef.getEncoding(), AudioSystem.NOT_SPECIFIED,
                    ef.getSampleSizeInBits(), ef.getChannels(), ef.getFrameSize(),
                    AudioSystem.NOT_SPECIFIED, ef.isBigEndian(), ef.properties());
            encoded = new AudioInputStream(encoded, lax, AudioSystem.NOT_SPECIFIED);
            byte[] buffer;
            ByteOrder order;
            // convert by encoding first, decoders reject fully specified target formats
            try (AudioInputStream decoded = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, encoded)) {
                AudioFormat format = decoded.getFormat();
                // decoders may leave fields unspecified, then 16 bit is implied
                if (format.getSampleSizeInBits() != AudioSystem.NOT_SPECIFIED && format.getSampleSizeInBits() != 16)
                    throw new IOException("unexpected decoded format: " + format);
                order = format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
                buffer = decoded.readAllBytes();
            }
            int numSamples = buffer.length / Short.BYTES;

            s.sampleDataSize = numSamples;
            s.sampleData = new short[numSamples];
            ByteBuffer.wrap(buffer).order(order).asShortBuffer().get(s.sampleData);

            // normalize offsets, loop offsets in the file are already relative
            s.start = 0;
            s.end = numSamples;

            if (!s.checkMeta())
                logger.log(Level.WARNING, "sample verification failed: " + s.name);
            s.dropByteData();
        }

        private void writeDword(int val) throws IOException {
            _outfile.writeInt(val);
        }

        private void writeWord(short val) throws IOException {
            _outfile.writeShort(val);
        }

        private void writeByte(byte val) throws IOException {
            _outfile.writeByte(val);
        }

        private void writeChar(char val) throws IOException {
            _outfile.writeByte((byte) val);
        }

        private void writeShort(short val) throws IOException {
            _outfile.writeShort(val);
        }

        private void write(byte[] p, int n) throws IOException {
            _outfile.write(p, 0, n);
        }

        /** writes exactly {@code size} bytes, NUL padded */
        private void writeString(String string, int size) throws IOException {
            byte[] b = string == null ? new byte[0] : string.getBytes(StandardCharsets.UTF_8);
            byte[] name = new byte[size];
            System.arraycopy(b, 0, name, 0, Math.min(b.length, size));
            write(name, size);
        }

        private void writeStringSection(String fourcc, String string) throws IOException {
            byte[] s = string.getBytes(StandardCharsets.UTF_8);
            write(fourcc.getBytes(), 4);
            int nn = s.length + 1; // with NUL terminator
            int n = ((nn + 1) / 2) * 2; // word aligned
            writeDword(n);
            byte[] padded = new byte[n];
            System.arraycopy(s, 0, padded, 0, s.length);
            write(padded, n);
        }

        private void writePreset(int zoneIdx, Preset preset) throws IOException {
            writeString(preset.name, 20);
            writeWord((short) preset.preset);
            writeWord((short) preset.bank);
            writeWord((short) zoneIdx);
            writeDword(preset.library);
            writeDword(preset.genre);
            writeDword(preset.morphology);
        }

        private void writeModulator(ModulatorList m) throws IOException {
            writeWord((short) m.src);
            writeWord((short) (m.dst == null ? 0 : m.dst.ordinal()));
            writeShort((short) m.amount);
            writeWord((short) m.amtSrc);
            writeWord((short) (m.transform == null ? 0 : m.transform.v));
        }

        private void writeGenerator(GeneratorList g) throws IOException {
            writeWord((short) (g.gen == null ? 0 : g.gen.ordinal()));
            if (g.gen == Gen_KeyRange || g.gen == Gen_VelRange) {
                writeByte(g.amount.bytes.lo);
                writeByte(g.amount.bytes.hi);
            } else {
                writeWord(g.amount.word);
            }
        }

        private void writeInstrument(int zoneIdx, Instrument instrument) throws IOException {
            writeString(instrument.name, 20);
            writeWord((short) zoneIdx);
        }

        private void writeIfil() throws IOException {
            writeString("ifil", 4);
            writeDword(4);
            byte[] data = new byte[4];
            switch (_fileFormatOut) {
                case SF2Format -> _version.major = 2;
                case SF3Format -> _version.major = 3;
                case SF4Format -> _version.major = 4;
            }
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
                case SF2Format: {
                    for (Sample s : _samples) {
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
                case SF3Format: {
                    for (Sample s : _samples) {
                        int written = writeSampleDataCompressed(s, VORBIS, OGG, quality);

                        s.setCompressionType(SampleCompression.Vorbis);
                        // Offsets in SF3 based on byte offset in file.
                        // Hack start/end of sample metadata to accommodate this:
                        s.start = (int) offsetFromChunk;
                        offsetFromChunk += written;
                        s.end = (int) offsetFromChunk;
                        // Important: keep relative loop offsets in file, so it can be restored after loading.
                    }
                    break;
                }
                case SF4Format: {
                    for (Sample s : _samples) {
                        int written = writeSampleDataCompressed(s, FLAC_ENC, FLAC, quality);

                        s.setCompressionType(SampleCompression.Flac);
                        // Offsets in SF4 based on byte offset in file.
                        s.start = (int) offsetFromChunk;
                        offsetFromChunk += written;
                        s.end = (int) offsetFromChunk;
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

            for (Preset p : _presets) {
                writePreset(zoneIdx, p);
                zoneIdx += p.zones.size();
            }
            Preset p = new Preset();
            p.name = "EOP";
            writePreset(zoneIdx, p);
        }

        private void writeBag(String fourcc, List<Zone> zones) throws IOException {
            writeString(fourcc, 4);
            int n = zones.size();
            writeDword((n + 1) * 4);
            int gIndex = 0;
            int pIndex = 0;

            for (Zone z : zones) {
                writeWord((short) gIndex);
                writeWord((short) pIndex);
                gIndex += z.generators.size();
                pIndex += z.modulators.size();
            }
            writeWord((short) gIndex);
            writeWord((short) pIndex);
        }

        private void writeMod(String fourcc, List<Zone> zones) throws IOException {
            writeString(fourcc, 4);
            int n = 0;

            for (Zone zone : zones) {
                n += zone.modulators.size();
            }
            writeDword((n + 1) * 10);

            for (Zone zone : zones) {
                for (ModulatorList m : zone.modulators) {
                    writeModulator(m);
                }
            }
            // Empty terminator
            ModulatorList mod = new ModulatorList();
            writeModulator(mod);
        }

        private void writeGen(String fourcc, List<Zone> zones) throws IOException {
            writeString(fourcc, 4);
            int n = 0;

            for (Zone zone : zones) {
                n += zone.generators.size();
            }
            writeDword((n + 1) * 4);

            for (Zone zone : zones) {
                for (GeneratorList g : zone.generators) {
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

            for (Instrument p : _instruments) {
                writeInstrument(zoneIdx, p);
                zoneIdx += p.zones.size();
            }
            Instrument p = new Instrument();
            p.name = "EOI";
            writeInstrument(zoneIdx, p);
        }

        void writeShdr() throws IOException {
            writeString("shdr", 4);
            writeDword(46 * (_samples.size() + 1));

            for (Sample sample : _samples) writeShdrEach(sample);

            // Empty last sample as terminator
            Sample s = new Sample();
            s.name = "EOS";
            s.sampletype = 0;
            writeShdrEach(s);
        }

        private void writeShdrEach(Sample s) throws IOException {
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

        private void writeShdXEach(SampleMeta m) throws IOException {
            assert m != null;

            long start = _outfile.position();

            writeString(m.name, 20);
            writeDword(m.samples);
            writeDword(m.loopstart);
            writeDword(m.loopend);
            // Check SampleMetaSize is correct
            assert _outfile.position() - start == SampleMetaSize;
        }

        private int writeSampleDataPlain(Sample s) throws IOException {
            if (s.numSamples() <= 0)
                return 0;

            int numBytes = s.numSamples() * Short.BYTES;
            byte[] b = new byte[numBytes];
            ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(s.sampleData, 0, s.numSamples());
            write(b, numBytes);
            return numBytes;
        }

        private int writeSampleDataCompressed(Sample s, AudioFormat.Encoding encoding, AudioFileFormat.Type fileType, int quality) throws IOException {
            if (s.numSamples() <= 0)
                return 0;
            int numSamples = s.numSamples();
            int rawBytes = numSamples * Short.BYTES;

            byte[] b = new byte[rawBytes];
            ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(s.sampleData, 0, numSamples);

            // TODO quality is currently not passed to the encoders
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            AudioFormat format = new AudioFormat(s.samplerate, 16, 1, true, false);
            AudioInputStream pcm = new AudioInputStream(new ByteArrayInputStream(b), format, numSamples);
            AudioInputStream encoded = AudioSystem.getAudioInputStream(encoding, pcm);
            AudioSystem.write(encoded, fileType, output);

            int numBytes = output.size();
            write(output.toByteArray(), numBytes);

            int percent = Math.round(100.f * (float) numBytes / (float) rawBytes);
            logger.log(Level.DEBUG, "Compressed " + fileType + ": " + s.name + " (" + percent + "%)");

            return numBytes;
        }

        // accessors

        public List<Preset> getPresets() {
            return _presets;
        }

        public List<Instrument> getInstruments() {
            return _instruments;
        }

        public List<Sample> getSamples() {
            return _samples;
        }

        public String getName() {
            return _name;
        }

        public String getEngine() {
            return _engine;
        }

        public String getCreator() {
            return _creator;
        }

        public String getProduct() {
            return _product;
        }

        public String getDate() {
            return _date;
        }

        public String getComment() {
            return _comment;
        }

        public String getCopyright() {
            return _copyright;
        }

        public String getTools() {
            return _tools;
        }

        public FileType getFileFormatIn() {
            return _fileFormatIn;
        }

        protected final List<Preset> _presets = new ArrayList<>();
        protected final List<Instrument> _instruments = new ArrayList<>();
        protected final List<Sample> _samples = new ArrayList<>();

        private File _path;
        private SeekableByteChannel _channel;
        private final sfVersionTag _version = new sfVersionTag();

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

        private FileType _fileFormatIn = FileType.SF2Format;
        private FileType _fileFormatOut = FileType.SF2Format;
        private long _fileSizeIn, _fileSizeOut;

        private final List<Zone> _pZones = new ArrayList<>(); // owned by _presets after loading
        private final List<Zone> _iZones = new ArrayList<>(); // owned by _instruments after loading
    }
}
