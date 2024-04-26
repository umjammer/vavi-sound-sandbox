/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq.block;

import java.util.ArrayList;
import java.util.List;

import vavi.sound.vsq.Block;
import vavi.util.Debug;


/**
 * BPList. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080628 nsano initial version <br>
 */
public class BPList implements Block {

    /**
     * <pre>
     * PitchBendBPList          PIT   -8191〜8191  0
     * PitchBendSensBPList      PBS     1〜24    1 
     * DynamicsBPList           DYN     0〜127   64
     * EpRResidualBPList        BRE     0〜127   0
     * EpRESlopeBPList          BRI     0〜127   64
     * EpRESlopeDepthBPList     CLE     0〜127   0
     * Reso1FreqBPList
     * Reso2FreqBPList
     * Reso3FreqBPList
     * Reso4FreqBPList
     * Reso1BWBPList
     * Reso2BWBPList
     * Reso3BWBPList
     * Reso4BWBPList
     * Reso1AmpBPList
     * Reso2AmpBPList
     * Reso3AmpBPList
     * Reso4AmpBPList
     * GenderFactorBPList       GEN     0〜127   64
     * PortamentoTimingBPList   POR     0〜127   64
     * OpeningBPList            OPE     0〜127   64
     * </pre>
     */
    String id;

    /** */
    public static class Pair {
        public long tick;
        public int id;
        public Pair(long tick, int id) {
            this.tick = tick;
            this.id = id;
        }
    }

    /** */
    protected List<Pair> bps = new ArrayList<>();

    /** */
    public static Block newInstance(String label, List<String> params) {
        BPList block = new BPList();
        block.id = label;
Debug.println("label: " + label);
        for (String param : params) {
            String[] pair = param.split("=");
            block.bps.add(new Pair(Long.parseLong(pair[0]), Integer.parseInt(pair[1])));
        }
        return block;
    }
}
