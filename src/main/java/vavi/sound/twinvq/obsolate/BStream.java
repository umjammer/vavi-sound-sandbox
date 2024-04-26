/*
 * Copyright 1996-2000 (c) NTT Cyber Space Laboratories
 *
 * Released on 2000.05.22 by N. Iwakami
 * Released on 2000.09.06 by N. Iwakami
 */

package vavi.sound.twinvq;

import java.io.IOException;
import java.util.Arrays;

import vavi.sound.twinvq.TwinVQ.BlockType;
import vavi.sound.twinvq.TwinVQ.Index;
import vavi.sound.twinvq.TwinVQ.HeaderInfo;
import vavi.sound.twinvq.TwinVQ.ConfInfo;
import vavi.sound.twinvq.TwinVQ.ConfInfoSubBlock;


class BStream {

    /** */
    TwinVQ twinVq = TwinVQ.getInstance();

    /**
     * bits table for VQ
     */
    static int[][] bits_0 = new int[TwinVQ.N_INTR_TYPE][];

    static int[][] bits_1 = new int[TwinVQ.N_INTR_TYPE][];

    /**
     * lookup parameters
     */
    private ConfInfo cf;

    private int iframe;

    private int TVQ_VERSION;

    /**
     * get string from the bitstream file
     *
     * @return none
     */
    private static int getString(byte[] buf, int nbytes, BFile bfp) throws IOException {
        int ichar, ibit;
        int[] c = new int[1];

        for (ichar = 0; ichar < nbytes; ichar++) {
            ibit = bfp.getBStream(c, 0, BFile.CHAR_BITS);
            if (ibit < BFile.CHAR_BITS) {
                break;
            }
            buf[ichar] = (byte) c[0];
        }

        buf[ichar] = '\0';
        return ichar;
    }

    /**
     * load the TWIN chunk from a bitstream file
     *
     * @return the TWIN chunk
     */
    ChunkChunk loadTwinChunk(BFile bfp) throws IOException {
        int[] chunkSize = new int[1];

        byte[] chunkID = new byte[TwinVQ.KEYWORD_BYTES + TwinVQ.VERSION_BYTES + 1];
        getString(chunkID, TwinVQ.KEYWORD_BYTES + TwinVQ.VERSION_BYTES, bfp);
        TVQ_VERSION = twinVq.TvqCheckVersion(new String(chunkID));
        if (TVQ_VERSION == TwinVQ.TVQ_UNKNOWN_VERSION) {
            throw new IllegalArgumentException(String.format("Header reading error: Unknown version (%s).\n", chunkID));
        }

        if (bfp.getBStream(chunkSize, 0, TwinVQ.ELEM_BYTES * BFile.CHAR_BITS) <= 0) {
            throw new IllegalArgumentException("Header reading error: Failed to get header size.");
        }

        byte[] chunkData = new byte[chunkSize[0] + 1];
        if (getString(chunkData, chunkSize[0], bfp) < chunkSize[0]) {
            throw new IllegalArgumentException("Header reading error: Failed to read header data.");
        }

        ChunkChunk TwinChunk = new ChunkChunk(new String(chunkID));
        TwinChunk.putData(chunkSize[0], chunkData);

        return TwinChunk;
    }

    /**
     * read bitstream header and create the header chunk data
     *
     * @param bfp bitstream file pointer
     * @return returns 1 when error or 0
     */
    ChunkChunk getBsHeaderInfo(BFile bfp) throws IOException {

        ChunkChunk twinChunk = loadTwinChunk(bfp);
        if (twinChunk == null) {
            throw new IllegalArgumentException("Failed to read header. Check the bitstream file.");
        }

        byte[] lbuf = new byte[TwinVQ.BUFSIZ];
        getString(lbuf, TwinVQ.KEYWORD_BYTES, bfp);
        if (Arrays.equals(lbuf, "DATA".getBytes())) {
            throw new IllegalArgumentException(
                    String.format("TwinVQ format error. No \"DATA\" chunk was found. found %s chunk", lbuf));
        }

        return twinChunk;
    }

    /**
     * initialize the bitstream reader Return: Returns 1 when error or returns 0
     */
    int initBsReader(HeaderInfo setupInfo) {

        TVQ_VERSION = twinVq.TvqCheckVersion(new String(setupInfo.id));
        if (TVQ_VERSION == TwinVQ.TVQ_UNKNOWN_VERSION) {
            return 1;
        }

        twinVq.TvqGetConfInfo(cf);

        twinVq.TvqGetVectorInfo(bits_0, bits_1);

        iframe = 0;

        return 0;
    }

    /**
     * get VQ information
     *
     * @return number of bits read
     */
    private int getVqInfo(ConfInfoSubBlock cfg, int[] bits0, int[] bits1, int variableBits, Index index, BFile bfp) throws IOException {
        int idiv;
        int bitcount = 0;

        if (index.btype[0] == BlockType.BLK_LONG.ordinal()) {
            twinVq.TvqUpdateVectorInfo(variableBits, cfg.ndiv, bits0, bits1); // re-calculate
            // VQ
            // bits
        }
        for (idiv = 0; idiv < cfg.ndiv[0]; idiv++) {
            bitcount += bfp.getBStream(index.wvq, idiv, bits0[idiv]); /* CB 0 */
            bitcount += bfp.getBStream(index.wvq, idiv + cfg.ndiv[0], bits1[idiv]); /* CB 1 */
        }

        return bitcount;
    }

    /**
     * get BSE information
     *
     * @return number of bits read
     */
    private static int getBseInfo(ConfInfo cf, ConfInfoSubBlock cfg, Index index, BFile bfp) throws IOException {
        int i_sup, isf, itmp, idiv;
        int bitcount = 0;

        for (i_sup = 0; i_sup < cf.N_CH; i_sup++) {
            for (isf = 0; isf < cfg.nsf; isf++) {
                for (idiv = 0; idiv < cfg.fw_ndiv; idiv++) {
                    itmp = idiv + (isf + i_sup * cfg.nsf) * cfg.fw_ndiv;
                    bitcount += bfp.getBStream(index.fw, itmp, cfg.fw_nbit);
                }
            }
        }
        for (i_sup = 0; i_sup < cf.N_CH; i_sup++) {
            for (isf = 0; isf < cfg.nsf; isf++) {
                bitcount += bfp.getBStream(index.fw_alf, i_sup * cfg.nsf + isf, cf.FW_ARSW_BITS);
            }
        }

        return bitcount;
    }

    /**
     * get gain information
     *
     * @return number of bits read
     */
    private static int getGainInfo(ConfInfo cf, ConfInfoSubBlock cfg, Index index, BFile bfp) throws IOException {
        int i_sup, iptop, isf;
        int bitcount = 0;

        for (i_sup = 0; i_sup < cf.N_CH; i_sup++) {
            iptop = (cfg.nsubg + 1) * i_sup;
            bitcount += bfp.getBStream(index.pow, iptop, cf.GAIN_BITS);
            for (isf = 0; isf < cfg.nsubg; isf++) {
                bitcount += bfp.getBStream(index.pow, iptop + isf + 1, cf.SUB_GAIN_BITS);
            }
        }

        return bitcount;
    }

    /**
     * get LSP information
     *
     * @return number of bits read
     */
    private static int getLspInfo(ConfInfo cf, Index index, BFile bfp) throws IOException {
        int i_sup, itmp;
        int bitcount = 0;

        for (i_sup = 0; i_sup < cf.N_CH; i_sup++) {
            // pred. switch
            bitcount += bfp.getBStream(index.lsp[i_sup], 0, cf.LSP_BIT0);
            // first stage
            bitcount += bfp.getBStream(index.lsp[i_sup], 1, cf.LSP_BIT1);
            // second stage
            for (itmp = 0; itmp < cf.LSP_SPLIT; itmp++) {
                bitcount += bfp.getBStream(index.lsp[i_sup], itmp + 2, cf.LSP_BIT2);
            }
        }

        return bitcount;
    }

    /**
     * get PPC information
     *
     * @return number of bits read
     */
    private static int getPpcInfo(ConfInfo cf, Index index, BFile bfp) throws IOException {
        int idiv, i_sup;
        int bitcount = 0;

        for (idiv = 0; idiv < cf.N_DIV_P; idiv++) {
            bitcount += bfp.getBStream(index.pls, idiv, bits_0[TwinVQ.BlockType.BLK_PPC.ordinal()][idiv]); /* CB0 */
            bitcount += bfp.getBStream(index.pls, idiv + cf.N_DIV_P, bits_1[TwinVQ.BlockType.BLK_PPC.ordinal()][idiv]);/* CB1 */
        }
        for (i_sup = 0; i_sup < cf.N_CH; i_sup++) {
            bitcount += bfp.getBStream(index.pit, i_sup, cf.BASF_BIT);
            bitcount += bfp.getBStream(index.pgain, i_sup, cf.PGAIN_BIT);
        }

        return bitcount;
    }

    /**
     * get EBC information
     *
     * @return number of bits read
     */
    private static int getEbcInfo(ConfInfo cf, ConfInfoSubBlock cfg, Index index, BFile bfp) throws IOException {
        int i_sup, isf, itmp;
        int bitcount = 0;

        for (i_sup = 0; i_sup < cf.N_CH; i_sup++) {
            for (isf = 0; isf < cfg.nsf; isf++) {
                int indexSfOffset = isf * (cfg.ncrb - cfg.ebc_crb_base) - cfg.ebc_crb_base;
                for (itmp = cfg.ebc_crb_base; itmp < cfg.ncrb; itmp++) {
                    bitcount += bfp.getBStream(index.bc[i_sup], itmp + indexSfOffset, cfg.ebc_bits);
                }
            }
        }

        return bitcount;
    }

    /**
     * read bitstream frame
     *
     * @param index Output: quantization indexes
     * @param bfp file pointer
     * @return 1: successful reading, 0: imcompleted reading
     */
    int readBsFrame(Index index, BFile bfp) throws IOException {
        // Variables
        ConfInfoSubBlock cfg;
        int variableBits;
        int bitcount;
        int numFixedBitsPerFrame = twinVq.TvqGetNumFixedBitsPerFrame();

        // Initialization
        variableBits = 0;
        bitcount = 0;

        // read block independent factors
        // Window type
        bitcount += bfp.getBStream(index.w_type, 0, cf.BITS_WTYPE);
        if (twinVq.TvqWtypeToBtype(index.w_type[0], index.btype) != 0) {
            System.err.printf("Error: unknown window type: %d\n", index.w_type);
            return 0;
        }
        int btype = index.btype[0];

        // read block dependent factors
        // set the block dependent parameters table
        cfg = cf.cfg[btype];

        bitcount += variableBits;

        // Interleaved vector quantization
        bitcount += getVqInfo(cfg, bits_0[btype], bits_1[btype], variableBits, index, bfp);

        // Bark-scale envelope
        bitcount += getBseInfo(cf, cfg, index, bfp);
        // Gain
        bitcount += getGainInfo(cf, cfg, index, bfp);
        // LSP
        bitcount += getLspInfo(cf, index, bfp);
        // PPC
        if (cfg.ppc_enable != 0) {
            bitcount += getPpcInfo(cf, index, bfp);
        }
        // Energy Balance Calibration
        if (cfg.ebc_enable != 0) {
            bitcount += getEbcInfo(cf, cfg, index, bfp);
        }

        iframe += 1;

        return bitcount == numFixedBitsPerFrame ? 1 : 0;
    }

    /**
     * skip frame
     *
     * @return skiped frames
     */
    long skipFrame(BFile bfp, long step) throws IOException {

        int numBits = twinVq.TvqGetNumFixedBitsPerFrame();
        iframe += (int) step;
        return bfp.bseek(numBits * step, BFile.BSEEK_CUR);
    }

    /**
     * returns current frame point
     *
     * @return current frame point
     */
    int getBsFramePoint() {
        return iframe;
    }

    //----

    /** */
    private static final BStream instance = new BStream();

    /** */
    private BStream() {
    }

    /** */
    public static BStream getInstance() {
        return instance;
    }
}
