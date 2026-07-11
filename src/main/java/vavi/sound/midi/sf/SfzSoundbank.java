/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.sf;

import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.sun.media.sound.SF2Instrument;
import com.sun.media.sound.SF2InstrumentRegion;
import com.sun.media.sound.SF2Layer;
import com.sun.media.sound.SF2LayerRegion;
import com.sun.media.sound.SF2Region;
import com.sun.media.sound.SF2Sample;
import com.sun.media.sound.SF2Soundbank;
import vavi.sound.sf.SFZ;

import static java.lang.System.getLogger;


/**
 * SfzSoundbank. Converts parsed {@link SFZ} into an in-memory {@link SF2Soundbank}
 * for playability by Gervill.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-09 nsano initial version <br>
 */
public class SfzSoundbank {

    private static final Logger logger = getLogger(SfzSoundbank.class.getName());

    private final SFZ sfz;
    private final Path baseDir;
    private final Map<String, SF2Sample> sampleCache = new HashMap<>();
    private Map<String, Path> fileIndex;

    private SfzSoundbank(SFZ sfz, Path baseDir) {
        this.sfz = sfz;
        this.baseDir = baseDir != null ? baseDir : sfz.getBasePath();
    }

    public static Soundbank getSoundbank(SFZ sfz, Path baseDir) {
        return new SfzSoundbank(sfz, baseDir).toSf2();
    }

    private Soundbank toSf2() {
        SF2Soundbank sf2 = new SF2Soundbank();
        sf2.setName(sfz.getName());

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName(sfz.getName());
        sf2.addResource(layer);

        for (SFZ.Region region : sfz.getRegions()) {
            if (region.sample == null || region.sample.isEmpty()) {
                continue;
            }
            SF2Sample sample = getSample(sf2, region);
            if (sample == null) {
                continue;
            }
            layer.getRegions().add(toRegion(region, sample));
        }

        SF2Instrument instrument = new SF2Instrument(sf2);
        instrument.setName(sfz.getName());
        instrument.setPatch(new Patch(0, 0));
        SF2InstrumentRegion instrumentRegion = new SF2InstrumentRegion();
        instrumentRegion.setLayer(layer);
        instrument.getRegions().add(instrumentRegion);
        sf2.addInstrument(instrument);

        logger.log(Level.DEBUG, "soundbank: " + sfz.getName() + ", regions: " + layer.getRegions().size());
        return sf2;
    }

    private SF2LayerRegion toRegion(SFZ.Region region, SF2Sample sample) {
        SF2LayerRegion sf2Region = new SF2LayerRegion();
        sf2Region.setSample(sample);

        sf2Region.putBytes(SF2Region.GENERATOR_KEYRANGE,
                new byte[] {(byte) (region.keyLo & 0x7f), (byte) (region.keyHi & 0x7f)});
        sf2Region.putBytes(SF2Region.GENERATOR_VELRANGE,
                new byte[] {(byte) (region.velLo & 0x7f), (byte) (region.velHi & 0x7f)});
        sf2Region.putInteger(SF2Region.GENERATOR_OVERRIDINGROOTKEY, region.keyRoot & 0x7f);

        if (region.transpose != 0) {
            sf2Region.putInteger(SF2Region.GENERATOR_COARSETUNE, region.transpose);
        }
        if (region.finetune != 0) {
            sf2Region.putInteger(SF2Region.GENERATOR_FINETUNE, (int) region.finetune);
        }
        if (region.panning != -128) {
            sf2Region.putInteger(SF2Region.GENERATOR_PAN, (int) (region.panning * 5));
        }
        if (region.volume < 0) {
            sf2Region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, (int) (-region.volume * 10));
        }
        if (region.cutoff > 0) {
            double cents = 1200.0 * Math.log(region.cutoff / 8.1758) / Math.log(2.0);
            sf2Region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, (int) Math.round(cents));
        }
        if (region.resonance > 0) {
            sf2Region.putInteger(SF2Region.GENERATOR_INITIALFILTERQ, (int) Math.round(region.resonance * 10));
        }

        // AMPEG Envelope mapping
        if (region.ampEnv.delay > 0) {
            sf2Region.putInteger(SF2Region.GENERATOR_DELAYVOLENV, secondsToCents(region.ampEnv.delay));
        }
        if (region.ampEnv.attack > 0) {
            sf2Region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, secondsToCents(region.ampEnv.attack));
        }
        if (region.ampEnv.hold > 0) {
            sf2Region.putInteger(SF2Region.GENERATOR_HOLDVOLENV, secondsToCents(region.ampEnv.hold));
        }
        if (region.ampEnv.decay > 0) {
            sf2Region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, secondsToCents(region.ampEnv.decay));
        }
        sf2Region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, sustainToCentibels(region.ampEnv.sustainLevel));
        if (region.ampEnv.release > 0) {
            sf2Region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, secondsToCents(region.ampEnv.release));
        }

        long frames = sample.getDataBuffer().capacity() / 2;
        if (region.offset > 0 && region.offset < frames) {
            putOffset(sf2Region, SF2Region.GENERATOR_STARTADDRSOFFSET, SF2Region.GENERATOR_STARTADDRSCOARSEOFFSET, (int) region.offset);
        }
        if (region.end > 0 && region.end < frames) {
            putOffset(sf2Region, SF2Region.GENERATOR_ENDADDRSOFFSET, SF2Region.GENERATOR_ENDADDRSCOARSEOFFSET, (int) (region.end - frames));
        }

        if (region.loopEnd > region.loopStart) {
            if (sample.getEndLoop() <= 0) {
                sample.setStartLoop(region.loopStart);
                sample.setEndLoop(region.loopEnd);
            } else {
                putOffset(sf2Region, SF2Region.GENERATOR_STARTLOOPADDRSOFFSET,
                        SF2Region.GENERATOR_STARTLOOPADDRSCOARSEOFFSET, (int) (region.loopStart - sample.getStartLoop()));
                putOffset(sf2Region, SF2Region.GENERATOR_ENDLOOPADDRSOFFSET,
                        SF2Region.GENERATOR_ENDLOOPADDRSCOARSEOFFSET, (int) (region.loopEnd - sample.getEndLoop()));
            }
            if (region.loopMode == SFZ.LoopMode.SUSTAIN) {
                sf2Region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 3);
            } else {
                sf2Region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
            }
        } else {
            sf2Region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 0);
        }

        return sf2Region;
    }

    private static int secondsToCents(double seconds) {
        if (seconds <= 0.001) return -12000;
        return (int) Math.round(1200.0 * Math.log(seconds) / Math.log(2.0));
    }

    private static int sustainToCentibels(double sustainLevel) {
        if (sustainLevel <= 0) return 1000;
        if (sustainLevel >= 100) return 0;
        return (int) Math.round(-200.0 * Math.log10(sustainLevel / 100.0));
    }

    private static void putOffset(SF2LayerRegion region, int fineGenerator, int coarseGenerator, int frames) {
        int coarse = Math.floorDiv(frames, 32768);
        int fine = frames - coarse * 32768;
        if (fine != 0) {
            region.putInteger(fineGenerator, fine);
        }
        if (coarse != 0) {
            region.putInteger(coarseGenerator, coarse);
        }
    }

    private SF2Sample getSample(SF2Soundbank sf2, SFZ.Region region) {
        return sampleCache.computeIfAbsent(region.sample, s -> {
            Path file = resolveSampleFile(s);
            if (file == null) {
                logger.log(Level.WARNING, "Sample not found: " + s);
                return null;
            }
            try {
                return toSf2Sample(sf2, region, file);
            } catch (IOException | UnsupportedAudioFileException e) {
                logger.log(Level.WARNING, "Cannot read sample: " + file, e);
                return null;
            }
        });
    }

    private static SF2Sample toSf2Sample(SF2Soundbank sf2, SFZ.Region region, Path file)
            throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file.toFile())) {
            AudioFormat source = ais.getFormat();
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    source.getSampleRate(), 16, source.getChannels(), source.getChannels() * 2,
                    source.getSampleRate(), false);
            byte[] pcm;
            try (AudioInputStream converted = AudioSystem.getAudioInputStream(target, ais)) {
                pcm = converted.readAllBytes();
            }
            int channels = target.getChannels();
            byte[] mono;
            if (channels == 1) {
                mono = pcm;
            } else {
                int frames = pcm.length / (2 * channels);
                mono = new byte[frames * 2];
                for (int f = 0; f < frames; f++) {
                    mono[f * 2] = pcm[f * 2 * channels];
                    mono[f * 2 + 1] = pcm[f * 2 * channels + 1];
                }
            }
            SF2Sample sample = new SF2Sample(sf2);
            sample.setName(Path.of(region.sample).getFileName().toString());
            sample.setData(mono);
            sample.setSampleRate((long) target.getSampleRate());
            sample.setOriginalPitch(region.keyRoot);
            sf2.addResource(sample);
            return sample;
        }
    }

    private Path resolveSampleFile(String samplePath) {
        Path p = Path.of(samplePath);
        if (p.isAbsolute() && Files.isRegularFile(p)) {
            return p;
        }
        if (baseDir != null) {
            Path resolved = baseDir.resolve(samplePath);
            if (Files.isRegularFile(resolved)) {
                return resolved;
            }
        }
        // Let's try filename-only resolution if it has folder structure
        String filename = p.getFileName().toString();
        if (baseDir != null) {
            Path resolved = baseDir.resolve(filename);
            if (Files.isRegularFile(resolved)) {
                return resolved;
            }
        }

        if (fileIndex == null) {
            fileIndex = buildFileIndex();
        }
        return fileIndex.get(filename);
    }

    private Map<String, Path> buildFileIndex() {
        Map<String, Path> index = new HashMap<>();
        String property = System.getProperty("vavi.sound.midi.sfz.samples");
        if (property != null) {
            indexFiles(Path.of(property), index);
        }
        // search under sforzando documents folder if exists
        Path sforzando = Path.of(System.getProperty("user.home"), "Documents", "sforzando");
        if (Files.isDirectory(sforzando)) {
            indexFiles(sforzando, index);
        }
        if (baseDir != null) {
            indexFiles(baseDir, index);
        }
        return index;
    }

    private static void indexFiles(Path root, Map<String, Path> index) {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> index.putIfAbsent(p.getFileName().toString(), p));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Cannot index: " + root, e);
        }
    }
}
