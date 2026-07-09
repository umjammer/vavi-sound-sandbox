/*
 * SampleFormatSFZ.cpp
 * -------------------
 * Purpose: Loading and saving SFZ instruments.
 * Notes  : (currently none)
 * Authors: OpenMPT Devs
 * The OpenMPT source code is released under the BSD license. Read LICENSE for more details.
 */

package vavi.sound.sf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.getLogger;


/**
 * SFZ parser library. Parses SFZ format files.
 *
 * @author OpenMPT Devs
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-09 nsano initial version <br>
 */
public class SFZ {

    private static final Logger logger = getLogger(SFZ.class.getName());

    public enum LoopMode {
        UNSPECIFIED,
        CONTINUOUS,
        ONE_SHOT,
        SUSTAIN,
        NO_LOOP
    }

    public enum LoopType {
        UNSPECIFIED,
        FORWARD,
        BACKWARD,
        ALTERNATE
    }

    public static class Control {
        public String defaultPath = "";
        public int octaveOffset = 0;
        public int noteOffset = 0;

        public void parse(String key, String value) {
            switch (key) {
                case "default_path":
                    defaultPath = value.replace('\\', '/');
                    if (!defaultPath.isEmpty() && !defaultPath.endsWith("/")) {
                        defaultPath += "/";
                    }
                    break;
                case "octave_offset":
                    octaveOffset = Integer.parseInt(value);
                    break;
                case "note_offset":
                    noteOffset = Integer.parseInt(value);
                    break;
            }
        }
    }

    public static class Envelope {
        public double startLevel = 0;
        public double delay = 0;
        public double attack = 0;
        public double hold = 0;
        public double decay = 0;
        public double sustainLevel = 100;
        public double release = 0;
        public double depth = 0;

        public void parse(String key, String value) {
            try {
                double v = Double.parseDouble(value);
                switch (key) {
                    case "start":
                        startLevel = v;
                        break;
                    case "delay":
                        delay = v;
                        break;
                    case "attack":
                        attack = v;
                        break;
                    case "hold":
                        hold = v;
                        break;
                    case "decay":
                        decay = v;
                        break;
                    case "sustain":
                        sustainLevel = v;
                        break;
                    case "release":
                        release = v;
                        break;
                    case "depth":
                        depth = v;
                        break;
                }
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Invalid envelope value: " + value + " for key: " + key, e);
            }
        }
    }

    public static class Region {
        public String sample = "";
        public String globalName = "";
        public String regionName = "";
        public Envelope ampEnv = new Envelope();
        public Envelope pitchEnv = new Envelope();
        public Envelope filterEnv = new Envelope();

        public long loopStart = 0;
        public long loopEnd = 0;
        public long end = Long.MAX_VALUE;
        public long offset = 0;

        public LoopMode loopMode = LoopMode.UNSPECIFIED;
        public LoopType loopType = LoopType.UNSPECIFIED;
        public double loopCrossfade = 0;

        public double cutoff = 0;
        public double resonance = 0;
        public double volume = 0;
        public double amplitude = 100;
        public double pitchBend = 200;

        public double pitchLfoFade = 0;
        public double pitchLfoDepth = 0;
        public double pitchLfoFreq = 0;

        public double panning = -128; // -128 means unset
        public double finetune = 0;
        public int transpose = 0;

        public int keyLo = 0;
        public int keyHi = 127;
        public int keyRoot = 60;

        public int velLo = 0;
        public int velHi = 127;

        public boolean invertPhase = false;
        public int group = 0;
        public int offBy = 0;
        public String trigger = "attack";

        public Region copy() {
            Region r = new Region();
            r.sample = this.sample;
            r.globalName = this.globalName;
            r.regionName = this.regionName;

            r.ampEnv.startLevel = this.ampEnv.startLevel;
            r.ampEnv.delay = this.ampEnv.delay;
            r.ampEnv.attack = this.ampEnv.attack;
            r.ampEnv.hold = this.ampEnv.hold;
            r.ampEnv.decay = this.ampEnv.decay;
            r.ampEnv.sustainLevel = this.ampEnv.sustainLevel;
            r.ampEnv.release = this.ampEnv.release;
            r.ampEnv.depth = this.ampEnv.depth;

            r.pitchEnv.startLevel = this.pitchEnv.startLevel;
            r.pitchEnv.delay = this.pitchEnv.delay;
            r.pitchEnv.attack = this.pitchEnv.attack;
            r.pitchEnv.hold = this.pitchEnv.hold;
            r.pitchEnv.decay = this.pitchEnv.decay;
            r.pitchEnv.sustainLevel = this.pitchEnv.sustainLevel;
            r.pitchEnv.release = this.pitchEnv.release;
            r.pitchEnv.depth = this.pitchEnv.depth;

            r.filterEnv.startLevel = this.filterEnv.startLevel;
            r.filterEnv.delay = this.filterEnv.delay;
            r.filterEnv.attack = this.filterEnv.attack;
            r.filterEnv.hold = this.filterEnv.hold;
            r.filterEnv.decay = this.filterEnv.decay;
            r.filterEnv.sustainLevel = this.filterEnv.sustainLevel;
            r.filterEnv.release = this.filterEnv.release;
            r.filterEnv.depth = this.filterEnv.depth;

            r.loopStart = this.loopStart;
            r.loopEnd = this.loopEnd;
            r.end = this.end;
            r.offset = this.offset;
            r.loopMode = this.loopMode;
            r.loopType = this.loopType;
            r.loopCrossfade = this.loopCrossfade;
            r.cutoff = this.cutoff;
            r.resonance = this.resonance;
            r.volume = this.volume;
            r.amplitude = this.amplitude;
            r.pitchBend = this.pitchBend;
            r.pitchLfoFade = this.pitchLfoFade;
            r.pitchLfoDepth = this.pitchLfoDepth;
            r.pitchLfoFreq = this.pitchLfoFreq;
            r.panning = this.panning;
            r.finetune = this.finetune;
            r.transpose = this.transpose;
            r.keyLo = this.keyLo;
            r.keyHi = this.keyHi;
            r.keyRoot = this.keyRoot;
            r.velLo = this.velLo;
            r.velHi = this.velHi;
            r.invertPhase = this.invertPhase;
            r.group = this.group;
            r.offBy = this.offBy;
            r.trigger = this.trigger;
            return r;
        }

        public void parse(String key, String value, Control control) {
            try {
                if (key.startsWith("ampeg_")) {
                    ampEnv.parse(key.substring(6), value);
                } else if (key.startsWith("pitcheg_")) {
                    pitchEnv.parse(key.substring(8), value);
                } else if (key.startsWith("fileg_")) {
                    filterEnv.parse(key.substring(6), value);
                } else {
                    switch (key) {
                        case "sample":
                            sample = (control.defaultPath + value).replace('\\', '/');
                            break;
                        case "global_label":
                            globalName = value;
                            break;
                        case "region_label":
                            regionName = value;
                            break;
                        case "lokey":
                            keyLo = readKey(value, control);
                            break;
                        case "hikey":
                            keyHi = readKey(value, control);
                            break;
                        case "pitch_keycenter":
                            keyRoot = readKey(value, control);
                            break;
                        case "key":
                            keyLo = keyHi = keyRoot = readKey(value, control);
                            break;
                        case "lovel":
                            velLo = Integer.parseInt(value);
                            break;
                        case "hivel":
                            velHi = Integer.parseInt(value);
                            break;
                        case "loop_mode":
                            switch (value) {
                                case "loop_continuous":
                                    loopMode = LoopMode.CONTINUOUS;
                                    break;
                                case "one_shot":
                                    loopMode = LoopMode.ONE_SHOT;
                                    break;
                                case "loop_sustain":
                                    loopMode = LoopMode.SUSTAIN;
                                    break;
                                case "no_loop":
                                    loopMode = LoopMode.NO_LOOP;
                                    break;
                                default:
                                    loopMode = LoopMode.UNSPECIFIED;
                                    break;
                            }
                            break;
                        case "loop_start":
                            loopStart = Long.parseLong(value);
                            break;
                        case "loop_end":
                            loopEnd = Long.parseLong(value);
                            break;
                        case "offset":
                            offset = Long.parseLong(value);
                            break;
                        case "end":
                            end = Long.parseLong(value);
                            break;
                        case "volume":
                            volume = Double.parseDouble(value);
                            break;
                        case "amplitude":
                            amplitude = Double.parseDouble(value);
                            break;
                        case "pan":
                            panning = Double.parseDouble(value);
                            break;
                        case "transpose":
                            transpose = Integer.parseInt(value);
                            break;
                        case "tune":
                            finetune = Double.parseDouble(value);
                            break;
                        case "cutoff":
                            cutoff = Double.parseDouble(value);
                            break;
                        case "resonance":
                            resonance = Double.parseDouble(value);
                            break;
                        case "group":
                            group = Integer.parseInt(value);
                            break;
                        case "off_by":
                            offBy = Integer.parseInt(value);
                            break;
                        case "trigger":
                            trigger = value;
                            break;
                    }
                }
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Invalid value for key " + key + ": " + value, e);
            }
        }
    }

    private final String name;
    private final Path basePath;
    private final Control control = new Control();
    private final List<Region> regions = new ArrayList<>();

    public SFZ(Path file) {
        this.name = file.getFileName().toString().replaceFirst("\\.[^.]+$", "");
        this.basePath = file.toAbsolutePath().getParent();
    }

    public String getName() {
        return name;
    }

    public Path getBasePath() {
        return basePath;
    }

    public Control getControl() {
        return control;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public static SFZ read(Path file) throws IOException {
        SFZ sfz = new SFZ(file);
        sfz.parse(file);
        return sfz;
    }

    private void parse(Path file) throws IOException {
        if (!Files.exists(file)) {
            logger.log(Level.WARNING, "SFZ file not found: " + file);
            return;
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        Region globalDefaults = new Region();
        Region groupDefaults = new Region();
        Region currentRegion = null;
        String currentSection = "";

        for (String line : lines) {
            int commentIdx = line.indexOf("//");
            if (commentIdx >= 0) {
                line = line.substring(0, commentIdx);
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("#include")) {
                Pattern p = Pattern.compile("#include\\s+[\"<]([^\">]+)[\">]");
                Matcher m = p.matcher(line);
                if (m.find()) {
                    String includeFile = m.group(1);
                    Path includePath = file.getParent().resolve(includeFile);
                    parse(includePath);
                }
                continue;
            }

            List<String> tokens = tokenize(line);
            for (String token : tokens) {
                if (token.startsWith("<") && token.endsWith(">")) {
                    currentSection = token.substring(1, token.length() - 1).toLowerCase();
                    if ("region".equals(currentSection)) {
                        if (currentRegion != null) {
                            regions.add(currentRegion);
                        }
                        currentRegion = groupDefaults.copy();
                    } else if ("group".equals(currentSection)) {
                        if (currentRegion != null) {
                            regions.add(currentRegion);
                            currentRegion = null;
                        }
                        groupDefaults = globalDefaults.copy();
                    } else if ("global".equals(currentSection)) {
                        if (currentRegion != null) {
                            regions.add(currentRegion);
                            currentRegion = null;
                        }
                        globalDefaults = new Region();
                        groupDefaults = globalDefaults.copy();
                    } else if ("control".equals(currentSection)) {
                        if (currentRegion != null) {
                            regions.add(currentRegion);
                            currentRegion = null;
                        }
                    }
                } else {
                    int eqIdx = token.indexOf('=');
                    if (eqIdx > 0) {
                        String key = token.substring(0, eqIdx).trim();
                        String value = token.substring(eqIdx + 1).trim();
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }

                        if ("control".equals(currentSection)) {
                            control.parse(key, value);
                        } else if ("global".equals(currentSection)) {
                            globalDefaults.parse(key, value, control);
                            groupDefaults.parse(key, value, control);
                        } else if ("group".equals(currentSection)) {
                            groupDefaults.parse(key, value, control);
                        } else if ("region".equals(currentSection)) {
                            if (currentRegion == null) {
                                currentRegion = groupDefaults.copy();
                            }
                            currentRegion.parse(key, value, control);
                        }
                    }
                }
            }
        }
        if (currentRegion != null) {
            regions.add(currentRegion);
        }
    }

    private static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                sb.append(c);
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            tokens.add(sb.toString());
        }
        return tokens;
    }

    public static int readKey(String value, Control control) {
        if (value == null || value.isEmpty()) return 0;
        char c = value.charAt(0);
        if (Character.isDigit(c)) {
            try {
                return Math.max(0, Math.min(127, Integer.parseInt(value) + control.octaveOffset * 12 + control.noteOffset));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (value.length() < 2) return 0;
        int[] keys = {9, 11, 0, 2, 4, 5, 7}; // a,b,c,d,e,f,g
        int key;
        if (c >= 'A' && c <= 'G') key = keys[c - 'A'];
        else if (c >= 'a' && c <= 'g') key = keys[c - 'a'];
        else return 0;

        int octaveStart = 1;
        if (value.charAt(1) == '#') {
            key++;
            octaveStart = 2;
        } else if (value.charAt(1) == 'b' || value.charAt(1) == 'B') {
            key--;
            octaveStart = 2;
        }
        if (octaveStart >= value.length()) return 0;

        try {
            int octave = Integer.parseInt(value.substring(octaveStart));
            key += (octave + 1) * 12;
            key += control.octaveOffset * 12 + control.noteOffset;
            return Math.max(0, Math.min(127, key));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
