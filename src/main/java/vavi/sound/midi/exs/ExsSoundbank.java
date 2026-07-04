/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.exs;

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
import vavi.sound.exs.EXS;

import static java.lang.System.getLogger;


/**
 * ExsSoundbank. converts an {@link EXS} instrument into an in-memory
 * {@link SF2Soundbank} so that gervill ({@code SoftSynthesizer}) can play it.
 * <p>
 * system property
 * <li>"vavi.sound.midi.exs.samples" ... a directory searched recursively for sample audio files</li>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-04 nsano initial version <br>
 */
public class ExsSoundbank {

    private static final Logger logger = getLogger(ExsSoundbank.class.getName());

    /** GarageBand/Logic library layout: instruments and samples are sibling trees */
    private static final String SAMPLER_FILES = "Sampler Files";

    private final EXS exs;

    /** directory of the .exs file, nullable (e.g. when read from a stream) */
    private final Path baseDir;

    /** sample file name -> located path, built lazily from search roots */
    private Map<String, Path> fileIndex;

    /** exs sample index -> converted sample, null value means "failed, don't retry" */
    private final Map<Integer, SF2Sample> sampleCache = new HashMap<>();

    private ExsSoundbank(EXS exs, Path baseDir) {
        this.exs = exs;
        this.baseDir = baseDir;
    }

    /**
     * @param baseDir directory the exs file was loaded from, used to locate sample files. nullable
     */
    public static Soundbank getSoundbank(EXS exs, Path baseDir) {
        return new ExsSoundbank(exs, baseDir).toSf2();
    }

    private Soundbank toSf2() {
        SF2Soundbank sf2 = new SF2Soundbank();
        sf2.setName(exs.getName());

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName(exs.getName());
        sf2.addResource(layer);

        for (EXS.Zone zone : exs.getZones()) {
            int index = zone.getSampleIndex();
            if (index < 0 || index >= exs.getSamples().size()) {
                continue;
            }
            SF2Sample sample = getSample(sf2, index);
            if (sample == null) {
                continue;
            }
            layer.getRegions().add(toRegion(zone, sample));
        }

        SF2Instrument instrument = new SF2Instrument(sf2);
        instrument.setName(exs.getName());
        instrument.setPatch(new Patch(0, 0));
        SF2InstrumentRegion instrumentRegion = new SF2InstrumentRegion();
        instrumentRegion.setLayer(layer);
        instrument.getRegions().add(instrumentRegion);
        sf2.addInstrument(instrument);

        logger.log(Level.DEBUG, "soundbank: " + exs.getName() + ", regions: " + layer.getRegions().size());
        return sf2;
    }

    private SF2LayerRegion toRegion(EXS.Zone zone, SF2Sample sample) {
        SF2LayerRegion region = new SF2LayerRegion();
        region.setSample(sample);

        region.putBytes(SF2Region.GENERATOR_KEYRANGE,
                new byte[] {(byte) (zone.getKeyLow() & 0x7f), (byte) (zone.getKeyHigh() & 0x7f)});
        if (zone.isVelocityRangeOn()) {
            region.putBytes(SF2Region.GENERATOR_VELRANGE,
                    new byte[] {(byte) (zone.getVelLow() & 0x7f), (byte) (zone.getVelHigh() & 0x7f)});
        }
        region.putInteger(SF2Region.GENERATOR_OVERRIDINGROOTKEY, zone.getKey() & 0x7f);
        if (zone.getFineTuning() != 0) {
            region.putInteger(SF2Region.GENERATOR_FINETUNE, zone.getFineTuning());
        }
        if (zone.getCoarseTuning() != 0) {
            region.putInteger(SF2Region.GENERATOR_COARSETUNE, zone.getCoarseTuning());
        }
        if (zone.getPan() != 0) {
            region.putInteger(SF2Region.GENERATOR_PAN, zone.getPan() * 10);
        }
        if (zone.getVolume() < 0) {
            // exs volume is in dB, sf2 initial attenuation is in centibels
            region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -zone.getVolume() * 10);
        }
        if (!zone.isPitchTracking()) {
            region.putInteger(SF2Region.GENERATOR_SCALETUNING, 0);
        }

        long frames = sample.getDataBuffer().capacity() / 2;
        int start = zone.getSampleStart();
        if (start > 0 && start < frames) {
            putOffset(region, SF2Region.GENERATOR_STARTADDRSOFFSET, SF2Region.GENERATOR_STARTADDRSCOARSEOFFSET, start);
        }
        int end = zone.getSampleEnd();
        if (end > 0 && end < frames) {
            putOffset(region, SF2Region.GENERATOR_ENDADDRSOFFSET, SF2Region.GENERATOR_ENDADDRSCOARSEOFFSET, (int) (end - frames));
        }

        if (zone.isLoopOn() && !zone.isOneShot() && zone.getLoopEnd() > zone.getLoopStart()) {
            if (sample.getEndLoop() <= 0) {
                // first looping zone defines the sample loop, others express theirs as offsets
                sample.setStartLoop(zone.getLoopStart());
                sample.setEndLoop(zone.getLoopEnd());
            } else {
                putOffset(region, SF2Region.GENERATOR_STARTLOOPADDRSOFFSET,
                        SF2Region.GENERATOR_STARTLOOPADDRSCOARSEOFFSET, (int) (zone.getLoopStart() - sample.getStartLoop()));
                putOffset(region, SF2Region.GENERATOR_ENDLOOPADDRSOFFSET,
                        SF2Region.GENERATOR_ENDLOOPADDRSCOARSEOFFSET, (int) (zone.getLoopEnd() - sample.getEndLoop()));
            }
            region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        } else {
            region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 0);
        }

        return region;
    }

    /** stores a sample frame offset split into sf2 fine (14 bit) and coarse (32768 frames) generators */
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

    private SF2Sample getSample(SF2Soundbank sf2, int index) {
        return sampleCache.computeIfAbsent(index, i -> {
            EXS.Sample exsSample = exs.getSamples().get(i);
            Path file = resolveSampleFile(exsSample);
            if (file == null) {
                logger.log(Level.WARNING, "sample not found: " + exsSample.getFileName() + " (" + exsSample.getPath() + ")");
                return null;
            }
            try {
                return toSf2Sample(sf2, exsSample, file);
            } catch (IOException | UnsupportedAudioFileException e) {
                logger.log(Level.WARNING, "cannot read sample: " + file + ": " + e);
                return null;
            }
        });
    }

    /** decodes a sample audio file into a 16 bit signed little endian mono sf2 sample */
    private static SF2Sample toSf2Sample(SF2Soundbank sf2, EXS.Sample exsSample, Path file)
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
            sample.setName(exsSample.getName());
            sample.setData(mono);
            sample.setSampleRate((long) target.getSampleRate());
            sample.setOriginalPitch(60); // zones always carry an overriding root key
            sf2.addResource(sample);
            return sample;
        }
    }

    private Path resolveSampleFile(EXS.Sample sample) {
        String fileName = sample.getFileName();
        if (fileName == null || fileName.isEmpty()) {
            fileName = sample.getName();
        }
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        if (sample.getPath() != null && !sample.getPath().isEmpty()) {
            Path p = Path.of(sample.getPath(), fileName);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        if (baseDir != null) {
            Path p = baseDir.resolve(fileName);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }

        if (fileIndex == null) {
            fileIndex = buildFileIndex();
        }
        return fileIndex.get(fileName);
    }

    /** indexes audio files under the samples property directory and the GarageBand/Logic "Sampler Files" tree */
    private Map<String, Path> buildFileIndex() {
        Map<String, Path> index = new HashMap<>();
        String property = System.getProperty("vavi.sound.midi.exs.samples");
        if (property != null) {
            indexFiles(Path.of(property), index);
        }
        for (Path dir = baseDir; dir != null; dir = dir.getParent()) {
            Path samplerFiles = dir.resolve(SAMPLER_FILES);
            if (Files.isDirectory(samplerFiles)) {
                indexFiles(samplerFiles, index);
                break;
            }
        }
        return index;
    }

    private static void indexFiles(Path root, Map<String, Path> index) {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> index.putIfAbsent(p.getFileName().toString(), p));
        } catch (IOException e) {
            logger.log(Level.WARNING, "cannot index: " + root + ": " + e);
        }
    }
}
