/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.sf;

import javax.sound.midi.Soundbank;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.sun.media.sound.ModelPatch;
import com.sun.media.sound.SF2GlobalRegion;
import com.sun.media.sound.SF2Instrument;
import com.sun.media.sound.SF2InstrumentRegion;
import com.sun.media.sound.SF2Layer;
import com.sun.media.sound.SF2LayerRegion;
import com.sun.media.sound.SF2Modulator;
import com.sun.media.sound.SF2Region;
import com.sun.media.sound.SF2Sample;
import com.sun.media.sound.SF2Soundbank;
import vavi.sound.sf.SFont;

import static vavi.sound.sf.SFont.Generator.Gen_Instrument;
import static vavi.sound.sf.SFont.Generator.Gen_KeyRange;
import static vavi.sound.sf.SFont.Generator.Gen_SampleId;
import static vavi.sound.sf.SFont.Generator.Gen_VelRange;


/**
 * SfSoundbank. converts a decompressed {@link SFont.SoundFont} (SF3/SF4)
 * into an in-memory {@link SF2Soundbank} so that gervill
 * ({@code SoftSynthesizer}) can play it.
 * <p>
 * mirrors the zone semantics of the JDK's own SF2 reader: a zone whose
 * generators carry a sample/instrument id becomes a region, a zone
 * without one becomes the global zone.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-04 nsano initial version <br>
 */
public class SfSoundbank {

    private SfSoundbank() {
    }

    public static Soundbank toSoundbank(SFont.SoundFont sf) {
        SF2Soundbank sf2 = new SF2Soundbank();
        if (sf.getName() != null) sf2.setName(sf.getName());
        if (sf.getCreator() != null) sf2.setVendor(sf.getCreator());
        if (sf.getComment() != null) sf2.setDescription(sf.getComment());
        if (sf.getEngine() != null) sf2.setTargetEngine(sf.getEngine());
        if (sf.getProduct() != null) sf2.setProduct(sf.getProduct());
        if (sf.getDate() != null) sf2.setCreationDate(sf.getDate());
        if (sf.getTools() != null) sf2.setTools(sf.getTools());

        // samples
        List<SF2Sample> samples = new ArrayList<>();
        for (SFont.Sample s : sf.getSamples()) {
            SF2Sample sample = new SF2Sample(sf2);
            sample.setName(s.name);
            byte[] data = new byte[s.sampleDataSize * Short.BYTES];
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(s.sampleData, 0, s.sampleDataSize);
            sample.setData(data);
            sample.setSampleRate(s.samplerate);
            sample.setOriginalPitch(s.origpitch);
            sample.setPitchCorrection((byte) s.pitchadj);
            sample.setSampleLink(s.sampleLink);
            sample.setSampleType(s.sampletype & ~(SFont.SampleType.TypeVorbis.v | SFont.SampleType.TypeFlac.v));
            // loop offsets are relative to the sample start after decompression
            sample.setStartLoop(s.loopstart);
            sample.setEndLoop(s.loopend);
            sf2.addResource(sample);
            samples.add(sample);
        }

        // instruments -> layers
        List<SF2Layer> layers = new ArrayList<>();
        for (SFont.Instrument instrument : sf.getInstruments()) {
            SF2Layer layer = new SF2Layer(sf2);
            layer.setName(instrument.name);
            sf2.addResource(layer);
            layers.add(layer);
            for (SFont.Zone zone : instrument.zones) {
                SF2LayerRegion region = new SF2LayerRegion();
                Integer sampleId = fill(region, zone, Gen_SampleId);
                if (sampleId != null) {
                    if (sampleId >= 0 && sampleId < samples.size()) {
                        region.setSample(samples.get(sampleId));
                        layer.getRegions().add(region);
                    }
                } else {
                    layer.setGlobalZone(toGlobalRegion(region));
                }
            }
        }

        // presets -> instruments
        for (SFont.Preset preset : sf.getPresets()) {
            SF2Instrument instrument = new SF2Instrument(sf2);
            instrument.setName(preset.name);
            instrument.setPatch(preset.bank == 128 ?
                    new ModelPatch(0, preset.preset, true) :
                    new ModelPatch(preset.bank << 7, preset.preset, false));
            instrument.setLibrary(preset.library);
            instrument.setGenre(preset.genre);
            instrument.setMorphology(preset.morphology);
            for (SFont.Zone zone : preset.zones) {
                SF2InstrumentRegion region = new SF2InstrumentRegion();
                Integer layerId = fill(region, zone, Gen_Instrument);
                if (layerId != null) {
                    if (layerId >= 0 && layerId < layers.size()) {
                        region.setLayer(layers.get(layerId));
                        instrument.getRegions().add(region);
                    }
                } else {
                    instrument.setGlobalZone(toGlobalRegion(region));
                }
            }
            sf2.addInstrument(instrument);
        }

        return sf2;
    }

    /**
     * copies zone generators and modulators into the region.
     *
     * @param terminal the generator that links to a sample or layer, consumed apart
     * @return the terminal generator value, null when the zone has none (global zone)
     */
    private static Integer fill(SF2Region region, SFont.Zone zone, SFont.Generator terminal) {
        Integer terminalValue = null;
        for (SFont.GeneratorList g : zone.generators) {
            if (g.gen == null) {
                continue;
            }
            if (g.gen == terminal) {
                terminalValue = g.amount.word & 0xffff;
                continue;
            }
            short value;
            if (g.gen == Gen_KeyRange || g.gen == Gen_VelRange) {
                value = (short) ((g.amount.bytes.lo & 0xff) | ((g.amount.bytes.hi & 0xff) << 8));
            } else {
                value = g.amount.word;
            }
            region.getGenerators().put(g.gen.ordinal(), value);
        }
        for (SFont.ModulatorList m : zone.modulators) {
            SF2Modulator modulator = new SF2Modulator();
            modulator.setSourceOperator(m.src);
            modulator.setDestinationOperator(m.dst == null ? 0 : m.dst.ordinal());
            modulator.setAmount((short) m.amount);
            modulator.setAmountSourceOperator(m.amtSrc);
            modulator.setTransportOperator(m.transform == null ? 0 : m.transform.v);
            region.getModulators().add(modulator);
        }
        return terminalValue;
    }

    private static SF2GlobalRegion toGlobalRegion(SF2Region region) {
        SF2GlobalRegion global = new SF2GlobalRegion();
        global.getGenerators().putAll(region.getGenerators());
        global.getModulators().addAll(region.getModulators());
        return global;
    }
}
