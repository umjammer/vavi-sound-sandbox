/*
 * https://github.com/cldmnky/exsconvert/blob/main/pkg/exs/decode.go
 */

package vavi.sound.exs;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vavi.io.SeekableDataInputStream;

import static java.lang.System.getLogger;


/**
 * Exs24 decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-04-10 nsano initial version <br>
 * @see "https://claude.ai/chat/2ba404d8-e554-4d27-be8e-5dae5a2601bb"
 * @see "https://github.com/git-moss/ConvertWithMoss"
 */
public class EXS {

    private static final Logger logger = getLogger(EXS.class.getName());

    private static final int HEADER_CHUNK = 0x00;
    private static final int ZONE_CHUNK = 0x01;
    private static final int GROUP_CHUNK = 0x02;
    private static final int SAMPLE_CHUNK = 0x03;
    private static final int OPTIONS_CHUNK = 0x04;

    private String name;
    private boolean bigEndian;
    private boolean isSizeExpanded;
    private int size;
    private final List<Zone> zones = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();
    private final List<Sample> samples = new ArrayList<>();
    private Params params;
    private final List<List<Integer>> sequences = new ArrayList<>();

    public static class Zone {

        private final ExsZone exsZone;
        private final String name;
        private final boolean pitch;
        private final boolean oneShot;
        private final boolean reverse;
        private final boolean velocityRangeOn;
        private final boolean loopOn;
        private final boolean loopEqualPower;

        public Zone(ExsZone exsZone, String name, boolean pitch, boolean oneShot, boolean reverse,
                    boolean velocityRangeOn, boolean loopOn, boolean loopEqualPower) {
            this.exsZone = exsZone;
            this.name = name;
            this.pitch = pitch;
            this.oneShot = oneShot;
            this.reverse = reverse;
            this.velocityRangeOn = velocityRangeOn;
            this.loopOn = loopOn;
            this.loopEqualPower = loopEqualPower;
        }

        // Getters and setters
        public int getKeyLow() {
            return exsZone.keyLow;
        }

        public int getKeyHigh() {
            return exsZone.keyHigh;
        }

        public int getVelLow() {
            return exsZone.velLow;
        }

        public int getVelHigh() {
            return exsZone.velHigh;
        }

        public int getSampleIndex() {
            return exsZone.sampleIndex;
        }

        public int getGroupIndex() {
            return exsZone.groupIndex;
        }

        public String getName() {
            return name;
        }
    }

    public static class Group {

        private final ExsGroup exsGroup;
        private final String name;
        private final boolean decay;

        public Group(ExsGroup exsGroup, String name, boolean decay) {
            this.exsGroup = exsGroup;
            this.name = name;
            this.decay = decay;
        }

        // Getters and setters
        public int getID() {
            return exsGroup.id;
        }

        public int getSelectNumber() {
            return exsGroup.selectNumber;
        }

        public int getSelectGroup() {
            return exsGroup.selectGroup;
        }

        public String getName() {
            return name;
        }
    }

    public static class Sample {

        private final ExsSample exsSample;
        private final String name;
        private final String fileName;
        private final String path;

        public Sample(ExsSample exsSample, String name, String fileName, String path) {
            this.exsSample = exsSample;
            this.name = name;
            this.fileName = fileName;
            this.path = path;
        }

        // Getters and setters
        public String getName() {
            return name;
        }

        public String getFileName() {
            return fileName;
        }

        public String getPath() {
            return path;
        }
    }

    public static class ExsHeader {

        private final byte[] reserved1 = new byte[4];
        private int size;
        private final byte[] reserved2 = new byte[8];
        private final byte[] magic = new byte[4];
    }

    public static class ExsChunkHeader {

        private int signature;
        private int size;
        private final byte[] magic = new byte[4];
    }

    public static class ExsZone {

        private final byte[] reserved1 = new byte[8];
        private int id;
        private final byte[] reserved2 = new byte[8];
        private final byte[] name = new byte[64];
        private byte opts;
        private byte key;
        private byte fineTuning;
        private byte pan;
        private byte volume;
        private byte scale;
        private byte keyLow;
        private byte keyHigh;
        private byte reserved3;
        private byte velLow;
        private byte velHigh;
        private byte reserved4;
        private int sampleStart;
        private int sampleEnd;
        private int loopStart;
        private int loopEnd;
        private int loopCrossfade;
        private byte loopTune;
        private int loopOpts;
        private final byte[] reserved5 = new byte[43];
        private byte coarseTuning;
        private byte reserved6;
        private byte output;
        private final byte[] reserved7 = new byte[5];
        private int groupIndex;
        private int sampleIndex;
        private final byte[] reserved8 = new byte[8];
        private int sampleFade;
        private int offset;
    }

    public static class ExsGroup {

        private final byte[] reserved1 = new byte[8];
        private int id;
        private final byte[] reserved2 = new byte[8];
        private final byte[] name = new byte[64];
        private byte volume;
        private byte pan;
        private byte polyphony;
        private byte decay;
        private byte exclusive;
        private byte velLow;
        private byte velHigh;
        private final byte[] reserved3 = new byte[9];
        private int decayTime;
        private final byte[] reserved4 = new byte[21];
        private byte cutoff;
        private byte reserved5;
        private byte resonance;
        private final byte[] reserved6 = new byte[12];
        private int attack2;
        private int decay2;
        private int sustain2;
        private int release2;
        private byte reserved7;
        private byte trigger;
        private byte output;
        private final byte[] reserved8 = new byte[5];
        private int selectGroup;
        private byte selectType;
        private byte selectNumber;
        private byte selectHigh;
        private byte selectLow;
        private byte keyLow;
        private byte keyHigh;
        private final byte[] reserved9 = new byte[6];
        private int hold2;
        private int attack1;
        private int decay1;
        private int sustain1;
        private int release1;
    }

    public static class ExsSample {

        private final byte[] reserved1 = new byte[8];
        private int id;
        private final byte[] reserved2 = new byte[8];
        private final byte[] name = new byte[64];
        private final byte[] reserved3 = new byte[4];
        private int length;
        private int rate;
        private byte bitDepth;
        private final byte[] reserved4 = new byte[15];
        private int type;
        private final byte[] reserved5 = new byte[48];
        private final byte[] path = new byte[256];
        private final byte[] fileName = new byte[256];
    }

    public static class ExsParams {

        private final byte[] reserved1 = new byte[8];
        private int id;
        private final byte[] reserved2 = new byte[8];
        private final byte[] name = new byte[64];
        private final byte[] reserved3 = new byte[4];
        private final byte[] keys = new byte[100];
        private final short[] values = new short[100];
    }

    public static class Params {

        // Global
        private short outputVolume;
        private short keyScale;
        private short pitchBendUp;
        private short pitchBendDown;
        private short monoMode;
        private short voices;
        private boolean unison;
        // Start via Vel
        private short transpose;
        private short coarseTune;
        private short fineTune;
        private short glideTime;
        private short pitcher;
        private short pitcherViaVel;
        // Pitch Mod. Wheel
        private boolean filterOn;
        private short filterType;
        private boolean filterFat;
        // Filter Mod. Wheel
        private short filterCutoff;
        private short filterResonance;
        private short filterDrive;
        private short filterViaKey;
        // Filter ADSR via Vel
        private short levelFixed;
        private short levelViaVel;
        // Tremolo
        private short lfo1DecayDelay;
        private short lfo1Rate;
        private short lfo1Waveform;
        private short lfo2Waveform;
        private short lfo2Rate;
        private short lfo3Rate;
        // Envelope
        private short env1Attack;
        private short env1AttackViaVel;
        private short env1Decay;
        private short env1Sustain;
        private short env1Release;
        // Time
        private short timeVia;
        private short timeCurve;
        // Envelope 2
        private short env2Attack;
        private short env2AttackViaVel;
        private short env2Decay;
        private short env2Sustain;
        private short env2Release;
        // Velocity
        private short velocityOffset;
        private short velocityRandom;
        private short velocityXFade;
        private short velocityXFadeType;
        private short coarseTuneRemote;
        private short holdVia;
        private short sampleSelectRandom;
        private short randomDetune;
        // Modulator
        private final short[] destination = new short[10];
        private final short[] source = new short[10];
        private final short[] via = new short[10];
        private final short[] amount = new short[10];
        private final short[] amountVia = new short[10];
        private final boolean[] invert = new boolean[10];
        private final boolean[] invertVia = new boolean[10];
        private final boolean[] bypass = new boolean[10];
    }

    public static EXS newFromByteArray(Path path) throws IOException {
        EXS exs = new EXS();
        exs.name = path.getFileName().toString();

        // Create buffered reader
        SeekableDataInputStream reader = new SeekableDataInputStream(new FileInputStream(path.toFile()).getChannel());

        // Read header
        ExsHeader header = EXS.readHeader(reader);
        if (!new String(header.magic).equals("SOBT") &&
                !new String(header.magic).equals("SOBJ") &&
                !new String(header.magic).equals("TBOS") &&
                !new String(header.magic).equals("JBOS")) {
            throw new IOException("Not an exs file");
        }

        logger.log(Level.INFO, ">>>>>>> " + exs.name + " <<<<<<<<<");
        logger.log(Level.DEBUG, "Magic: " + new String(header.magic));

        if (new String(header.magic).equals("SOBT") || new String(header.magic).equals("SOBJ")) {
            exs.bigEndian = true;
        }

        // Read header again
        header = EXS.readHeader(reader);

        // Determine if the file is size expanded by checking the size of the header
        if (header.size > 0x8000) {
            logger.log(Level.DEBUG, "Size expanded file");
            exs.isSizeExpanded = true;
        }

        exs.size = (int) Files.size(path);
        logger.log(Level.DEBUG, "Size: " + exs.size);

        exs.readChunks(reader);

        List<Map<String, List<Zone>>> ranges = exs.getZonesByKeyRanges(4);
        logger.log(Level.DEBUG, "ranges: " + ranges);

        return exs;
    }

    private static ExsHeader readHeader(SeekableDataInputStream reader) throws IOException {
        ExsHeader header = new ExsHeader();

        // Read reserved1
        reader.read(header.reserved1);

        // Read size
        header.size = reader.readInt();

        // Read reserved2
        reader.read(header.reserved2);

        // Read magic
        reader.read(header.magic);

        return header;
    }

    public List<Map<String, List<Zone>>> getZonesByKeyRanges(int zonesPerRange) {
        Map<String, List<Zone>> zones = new HashMap<>();

        for (Zone zone : this.zones) {
            boolean hasSamples = false;

            if (zone.getSampleIndex() == -1) {
                continue;
            }

            // Check if the zone has samples
            for (int s = 0; s < this.samples.size(); s++) {
                if (zone.getSampleIndex() == s) {
                    hasSamples = true;
                    break;
                }
            }

            if (hasSamples) {
                String z = zone.getKeyLow() + "-" + zone.getKeyHigh();
                if (!zones.containsKey(z)) {
                    zones.put(z, new ArrayList<>());
                }
                zones.get(z).add(zone);
            }
        }

        // Sort zone map by velLow
        for (List<Zone> zoneList : zones.values()) {
            zoneList.sort(Comparator.comparingInt(Zone::getVelLow));
        }

        List<Map<String, List<Zone>>> ranges = new ArrayList<>();
        for (Map.Entry<String, List<Zone>> entry : zones.entrySet()) {
            String s = entry.getKey();
            List<Zone> zoneList = entry.getValue();

            for (int i = 0; i < zoneList.size(); i += zonesPerRange) {
                int end = i + zonesPerRange;
                if (end > zoneList.size()) {
                    end = zoneList.size();
                }

                Map<String, List<Zone>> rangeMap = new HashMap<>();
                rangeMap.put(s, zoneList.subList(i, end));
                ranges.add(rangeMap);
            }
        }

        // Process keys for sorting
        List<String> keys = new ArrayList<>();
        for (Map<String, List<Zone>> range : ranges) {
            for (Map.Entry<String, List<Zone>> entry : range.entrySet()) {
                for (Zone zone : entry.getValue()) {
                    keys.add(zone.getKeyLow() + "-" + zone.getKeyHigh());
                }
            }
        }

        Collections.sort(keys);
        logger.log(Level.DEBUG, "keys: " + keys);

        for (String k : keys) {
            for (Map<String, List<Zone>> z : ranges) {
                if (z.containsKey(k)) {
                    z.get(k).sort(Comparator.comparingInt(Zone::getVelLow));
                }
            }
        }

        return ranges;
    }

    public List<Group> getGroups() {
        List<Group> result = new ArrayList<>();

        for (Group group : this.groups) {
            boolean hasZones = false;
            logger.log(Level.INFO, "Group: " + group.getName() + ", ID: " + group.getID() +
                    ", Select number: " + group.getSelectNumber() +
                    ", Select group: " + group.getSelectGroup());

            for (Zone zone : this.zones) {
                logger.log(Level.DEBUG, "Zone: " + zone.getName() + ", Sample: " + zone.getSampleIndex() +
                        ", Group Index: " + zone.getGroupIndex());

                if (zone.getGroupIndex() == group.getID()) {
                    logger.log(Level.INFO, "-----> Zone has group");
                    if (zone.getSampleIndex() != -1 && this.samples.size() > zone.getSampleIndex()) {
                        hasZones = true;
                        break;
                    }
                }
            }

            if (hasZones) {
                result.add(group);
            }
        }

        if (result.isEmpty()) {
            ExsGroup exsGroup = new ExsGroup();
            exsGroup.id = 0;
            exsGroup.selectNumber = 0;
            exsGroup.selectGroup = -1;

            result.add(new Group(exsGroup, this.name, false));
        }

        logger.log(Level.DEBUG, "-----> Groups: " + result);
        return result;
    }

    private static ExsChunkHeader readChunkHeader(SeekableDataInputStream reader) throws IOException {
        ExsChunkHeader header = new ExsChunkHeader();

        // Read signature
        header.signature = reader.readInt();

        // Read size
        header.size = reader.readInt();

        // Read magic
        reader.read(header.magic);

        return header;
    }

    private static Zone readZone(SeekableDataInputStream reader, int pos) throws IOException {
        reader.position(pos);

        ExsZone exsZone = new ExsZone();

        // Read the zone structure
        reader.read(exsZone.reserved1);
        exsZone.id = reader.readInt();
        reader.read(exsZone.reserved2);
        reader.read(exsZone.name);
        exsZone.opts = reader.readByte();
        exsZone.key = reader.readByte();
        exsZone.fineTuning = reader.readByte();
        exsZone.pan = reader.readByte();
        exsZone.volume = reader.readByte();
        exsZone.scale = reader.readByte();
        exsZone.keyLow = reader.readByte();
        exsZone.keyHigh = reader.readByte();
        exsZone.reserved3 = reader.readByte();
        exsZone.velLow = reader.readByte();
        exsZone.velHigh = reader.readByte();
        exsZone.reserved4 = reader.readByte();
        exsZone.sampleStart = reader.readInt();
        exsZone.sampleEnd = reader.readInt();
        exsZone.loopStart = reader.readInt();
        exsZone.loopEnd = reader.readInt();
        exsZone.loopCrossfade = reader.readInt();
        exsZone.loopTune = reader.readByte();
        exsZone.loopOpts = reader.readInt();
        reader.read(exsZone.reserved5);
        exsZone.coarseTuning = reader.readByte();
        exsZone.reserved6 = reader.readByte();
        exsZone.output = reader.readByte();
        reader.read(exsZone.reserved7);
        exsZone.groupIndex = reader.readInt();
        exsZone.sampleIndex = reader.readInt();
        reader.read(exsZone.reserved8);
        exsZone.sampleFade = reader.readInt();
        exsZone.offset = reader.readInt();

        Zone zone = new Zone(
                exsZone,
                getString(exsZone.name),
                (exsZone.opts & 0x02) == 0,
                (exsZone.opts & (1 << 0)) != 0,
                (exsZone.opts & 0x04) != 0,
                (exsZone.opts & 0x08) != 0,
                (exsZone.loopOpts & 0x01) != 0,
                (exsZone.loopOpts & 0x02) != 0
        );

        return zone;
    }

    private static Group readGroup(SeekableDataInputStream reader, int pos) throws IOException {
        reader.position(pos);

        ExsGroup exsGroup = new ExsGroup();

        // Read the group structure
        reader.read(exsGroup.reserved1);
        exsGroup.id = reader.readInt();
        reader.read(exsGroup.reserved2);
        reader.read(exsGroup.name);
        exsGroup.volume = reader.readByte();
        exsGroup.pan = reader.readByte();
        exsGroup.polyphony = reader.readByte();
        exsGroup.decay = reader.readByte();
        exsGroup.exclusive = reader.readByte();
        exsGroup.velLow = reader.readByte();
        exsGroup.velHigh = reader.readByte();
        reader.read(exsGroup.reserved3);
        exsGroup.decayTime = reader.readInt();
        reader.read(exsGroup.reserved4);
        exsGroup.cutoff = reader.readByte();
        exsGroup.reserved5 = reader.readByte();
        exsGroup.resonance = reader.readByte();
        reader.read(exsGroup.reserved6);
        exsGroup.attack2 = reader.readInt();
        exsGroup.decay2 = reader.readInt();
        exsGroup.sustain2 = reader.readInt();
        exsGroup.release2 = reader.readInt();
        exsGroup.reserved7 = reader.readByte();
        exsGroup.trigger = reader.readByte();
        exsGroup.output = reader.readByte();
        reader.read(exsGroup.reserved8);
        exsGroup.selectGroup = reader.readInt();
        exsGroup.selectType = reader.readByte();
        exsGroup.selectNumber = reader.readByte();
        exsGroup.selectHigh = reader.readByte();
        exsGroup.selectLow = reader.readByte();
        exsGroup.keyLow = reader.readByte();
        exsGroup.keyHigh = reader.readByte();
        reader.read(exsGroup.reserved9);
        exsGroup.hold2 = reader.readInt();
        exsGroup.attack1 = reader.readInt();
        exsGroup.decay1 = reader.readInt();
        exsGroup.sustain1 = reader.readInt();
        exsGroup.release1 = reader.readInt();

        Group group = new Group(
                exsGroup,
                getString(exsGroup.name),
                (exsGroup.decay & 0x40) != 0
        );

        logger.log(Level.DEBUG, "Group: name: " + group.getName());
        return group;
    }

    private static Sample readSample(SeekableDataInputStream reader, int pos) throws IOException {
        reader.position(pos);

        ExsSample exsSample = new ExsSample();

        // Read the sample structure
        reader.read(exsSample.reserved1);
        exsSample.id = reader.readInt();
        reader.read(exsSample.reserved2);
        reader.read(exsSample.name);
        reader.read(exsSample.reserved3);
        exsSample.length = reader.readInt();
        exsSample.rate = reader.readInt();
        exsSample.bitDepth = reader.readByte();
        reader.read(exsSample.reserved4);
        exsSample.type = reader.readInt();
        reader.read(exsSample.reserved5);
        reader.read(exsSample.path);
        reader.read(exsSample.fileName);

        logger.log(Level.DEBUG, "Sample: name: " + getString(exsSample.name) +
                ", filename: " + getString(exsSample.fileName) +
                ", path: " + getString(exsSample.path));

        return new Sample(
                exsSample,
                getString(exsSample.name),
                getString(exsSample.fileName),
                getString(exsSample.path)
        );
    }

    private static ExsParams readParams(SeekableDataInputStream reader, int pos) throws IOException {
        reader.position(pos);

        ExsParams params = new ExsParams();

        // Read the params structure
        reader.read(params.reserved1);
        params.id = reader.readInt();
        reader.read(params.reserved2);
        reader.read(params.name);
        reader.read(params.reserved3);
        reader.read(params.keys);

        // Read values
        for (int i = 0; i < params.values.length; i++) {
            params.values[i] = reader.readShort();
        }

        logger.log(Level.DEBUG, "Params name: " + getString(params.name) + ", " + params);

        return params;
    }

    private void readChunks(SeekableDataInputStream reader) throws IOException {
        int i = 0;

        // Read until end of file
        while (i + 84 < this.size) {
            reader.position(i);
            ExsChunkHeader header = readChunkHeader(reader);
            int chunkType = (header.signature & 0x0F000000) >> 24;

            switch (chunkType) {
                case HEADER_CHUNK:
                    logger.log(Level.DEBUG, "Chunk: " + chunkType + " (exs header chunk), size: " + header.size);
                    break;

                case ZONE_CHUNK:
                    if (header.size < 110) {
                        throw new IOException("Invalid zone chunk size");
                    }
                    Zone zone = readZone(reader, i);
                    zones.add(zone);
                    logger.log(Level.INFO, "Zone: " + zone.getName() + ", size: " + header.size +
                            ", keyLow: " + zone.getKeyLow() + ", keyHigh: " + zone.getKeyHigh() +
                            ", sample: " + zone.getSampleIndex());
                    break;

                case GROUP_CHUNK:
                    logger.log(Level.DEBUG, "Exs chunk type: " + chunkType + " (group), size: " + header.size);
                    Group group = readGroup(reader, i);
                    groups.add(group);
                    break;

                case SAMPLE_CHUNK:
                    logger.log(Level.DEBUG, "Exs chunk type: " + chunkType + " (sample), size: " + header.size);
                    if (header.size != 336 && header.size != 592 && header.size != 600) {
                        throw new IOException("Invalid sample chunk size");
                    }
                    Sample sample = readSample(reader, i);
                    samples.add(sample);
                    break;

                case OPTIONS_CHUNK:
                    logger.log(Level.DEBUG, "Exs chunk type: " + chunkType + " (options), size: " + header.size);
                    ExsParams exsParams = readParams(reader, i);
                    params = newParamsFromExsParams(exsParams);
                    break;

                default:
                    logger.log(Level.DEBUG, "Exs chunk type: " + chunkType + " (unknown)");
                    break;
            }

            i = i + header.size + 84;
        }

        readSequences();
        convertSeqNumbers();

        logger.log(Level.INFO, "Exs " + name + " contains " + groups.size() + " groups, " +
                zones.size() + " zones, " + samples.size() + " samples");
    }

    // Stub methods that would need to be implemented
    private void readSequences() {
        // Implementation needed
    }

    private void convertSeqNumbers() {
        // Implementation needed
    }

    private static String getString(byte[] bytes) {
        // Find the first null byte
        int nullPos = 0;
        while (nullPos < bytes.length && bytes[nullPos] != 0) {
            nullPos++;
        }

        // Convert to string up to null byte
        return new String(bytes, 0, nullPos, StandardCharsets.UTF_8);
    }

    private static Params newParamsFromExsParams(ExsParams exsParams) {
        Params params = new Params();

        for (int i = 0; i < 100; i++) {
            byte key = exsParams.keys[i];
            short value = exsParams.values[i];

            switch (key & 0xff) {
                case 7:
                    params.outputVolume = value;
                    break;
                case 8:
                    params.keyScale = value;
                    break;
                case 3:
                    params.pitchBendUp = value;
                    break;
                case 4:
                    params.pitchBendDown = value;
                    break;
                case 10:
                    params.monoMode = value;
                    break;
                case 5:
                    params.voices = value;
                    break;
                case 171:
                    params.unison = value != 0;
                    break;
                case 45:
                    params.transpose = value;
                    break;
                case 14:
                    params.coarseTune = value;
                    break;
                case 15:
                    params.fineTune = value;
                    break;
                case 20:
                    params.glideTime = value;
                    break;
                case 44:
                    params.filterOn = value != 0;
                    break;
                case 46:
                    params.filterViaKey = value;
                    break;
                case 60:
                    params.lfo1DecayDelay = value;
                    break;
                case 61:
                    params.lfo1Rate = value;
                    break;
                case 62:
                    params.lfo1Waveform = value;
                    break;
                case 63:
                    params.lfo2Rate = value;
                    break;
                case 64:
                    params.lfo2Waveform = value;
                    break;
                case 72:
                    params.pitcher = value;
                    break;
                case 73:
                    params.pitcherViaVel = value;
                    break;
                case 75:
                    params.filterDrive = value;
                    break;
                case 76:
                    params.env1Attack = value;
                    break;
                case 77:
                    params.env1AttackViaVel = value;
                    break;
                case 78:
                    params.env1Decay = value;
                    break;
                case 79:
                    params.env1Sustain = value;
                    break;
                case 80:
                    params.env1Release = value;
                    break;
                case 81:
                    params.env2Sustain = value;
                    break;
                case 82:
                    params.env2Attack = value;
                    break;
                case 83:
                    params.env2AttackViaVel = value;
                    break;
                case 84:
                    params.env2Decay = value;
                    break;
                case 85:
                    params.env2Release = value;
                    break;
                case 89:
                    params.levelViaVel = value;
                    break;
                case 90:
                    params.levelFixed = value;
                    break;
                case 91:
                    params.timeCurve = value;
                    break;
                case 92:
                    params.timeVia = value;
                    break;
                case 95:
                    params.velocityOffset = value;
                    break;
                case 97:
                    params.velocityXFade = value;
                    break;
                case 98:
                    params.randomDetune = value;
                    break;
                case 163:
                    params.sampleSelectRandom = value;
                    break;
                case 164:
                    params.velocityRandom = value;
                    break;
                case 165:
                    params.velocityXFadeType = value;
                    break;
                case 166:
                    params.coarseTune = value;
                    break;
                case 167:
                    params.lfo3Rate = value;
                    break;
                case 170:
                    params.filterFat = value != 0; // 0:off 1:on
                    break;
                case 172:
                    params.holdVia = value;
                    break;
                case 173:
                    params.destination[0] = value;
                    break;
                case 174:
                    params.source[0] = value;
                    break;
                case 175:
                    params.via[0] = value;
                    break;
                case 176:
                    params.amount[0] = value;
                    break;
                case 177:
                    params.amountVia[0] = value;
                    break;
                case 178:
                    params.invertVia[0] = value != 0;
                    break;
                case 179:
                    params.destination[1] = value;
                    break;
                case 180:
                    params.source[1] = value;
                    break;
                case 181:
                    params.via[1] = value;
                    break;
                case 182:
                    params.amount[1] = value;
                    break;
                case 183:
                    params.amountVia[1] = value;
                    break;
                case 184:
                    params.invertVia[1] = value != 0;
                    break;
                case 185:
                    params.destination[2] = value;
                    break;
                case 186:
                    params.source[2] = value;
                    break;
                case 187:
                    params.via[2] = value;
                    break;
                case 188:
                    params.amount[2] = value;
                    break;
                case 189:
                    params.amountVia[2] = value;
                    break;
                case 190:
                    params.invertVia[2] = value != 0;
                    break;
                case 191:
                    params.destination[3] = value;
                    break;
                case 192:
                    params.source[3] = value;
                    break;
                case 193:
                    params.via[3] = value;
                    break;
                case 194:
                    params.amount[3] = value;
                    break;
                case 195:
                    params.amountVia[3] = value;
                    break;
                case 196:
                    params.invertVia[3] = value != 0;
                    break;
                case 197:
                    params.destination[4] = value;
                    break;
                case 198:
                    params.source[4] = value;
                    break;
                case 199:
                    params.via[4] = value;
                    break;
                case 200:
                    params.amount[4] = value;
                    break;
                case 201:
                    params.amountVia[4] = value;
                    break;
                case 202:
                    params.invertVia[4] = value != 0;
                    break;
                case 203:
                    params.destination[5] = value;
                    break;
                case 204:
                    params.source[5] = value;
                    break;
                case 205:
                    params.via[5] = value;
                    break;
                case 206:
                    params.amount[5] = value;
                    break;
                case 207:
                    params.amountVia[5] = value;
                    break;
                case 208:
                    params.invertVia[5] = value != 0;
                    break;
                case 209:
                    params.destination[6] = value;
                    break;
                case 210:
                    params.source[6] = value;
                    break;
                case 211:
                    params.via[6] = value;
                    break;
                case 212:
                    params.amount[6] = value;
                    break;
                case 213:
                    params.amountVia[6] = value;
                    break;
                case 214:
                    params.invertVia[6] = value != 0;
                    break;
                case 215:
                    params.destination[7] = value;
                    break;
                case 216:
                    params.source[7] = value;
                    break;
                case 217:
                    params.via[7] = value;
                    break;
                case 218:
                    params.amount[7] = value;
                    break;
                case 219:
                    params.amountVia[7] = value;
                    break;
                case 220:
                    params.invertVia[7] = value != 0;
                    break;
                case 221:
                    params.destination[8] = value;
                    break;
                case 222:
                    params.source[8] = value;
                    break;
                case 223:
                    params.via[8] = value;
                    break;
                case 224:
                    params.amount[8] = value;
                    break;
                case 225:
                    params.amountVia[8] = value;
                    break;
                case 226:
                    params.invertVia[8] = value != 0;
                    break;
                case 227:
                    params.destination[9] = value;
                    break;
                case 228:
                    params.source[9] = value;
                    break;
                case 229:
                    params.via[9] = value;
                    break;
                case 230:
                    params.amount[9] = value;
                    break;
                case 231:
                    params.amountVia[9] = value;
                    break;
                case 232:
                    params.invertVia[9] = value != 0;
                    break;
                case 233:
                    params.invert[0] = value != 0;
                    break;
                case 234:
                    params.invert[1] = value != 0;
                    break;
                case 235:
                    params.invert[2] = value != 0;
                    break;
                case 236:
                    params.invert[3] = value != 0;
                    break;
                case 237:
                    params.invert[4] = value != 0;
                    break;
                case 238:
                    params.invert[5] = value != 0;
                    break;
                case 239:
                    params.invert[6] = value != 0;
                    break;
                case 240:
                    params.invert[7] = value != 0;
                    break;
                case 241:
                    params.invert[8] = value != 0;
                    break;
                case 242:
                    params.invert[9] = value != 0;
                    break;
                case 243:
                    params.filterType = value;
                    break;
                case 244:
                    params.bypass[0] = value != 0;
                    break;
                case 245:
                    params.bypass[1] = value != 0;
                    break;
                case 246:
                    params.bypass[2] = value != 0;
                    break;
                case 247:
                    params.bypass[3] = value != 0;
                    break;
                case 248:
                    params.bypass[4] = value != 0;
                    break;
                case 249:
                    params.bypass[5] = value != 0;
                    break;
                case 250:
                    params.bypass[6] = value != 0;
                    break;
                case 251:
                    params.bypass[7] = value != 0;
                    break;
                case 252:
                    params.bypass[8] = value != 0;
                    break;
                case 253:
                    params.bypass[9] = value != 0;
                    break;

                default:
                    logger.log(Level.DEBUG, "unknown parameter %d".formatted(i));
            }

            i++;
        }
        //logger.log(Level.INFO, "params: %+v".formatted(params));
        return params;
    }
}
