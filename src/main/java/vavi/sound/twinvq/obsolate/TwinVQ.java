/*
 * (c)Copyright 1996-2000 NTT Cyber Space Laboratories
 *                Released on 2000.05.22 by N. Iwakami
 *                Modified on 2000.05.25 by N. Iwakami
 *                Released on 2000.09.06 by N. Iwakami
 */

package vavi.sound.twinvq.obsolate;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import com.sun.jna.Library;
import com.sun.jna.Structure;


/**
 * TwinVQ
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070202 initial version <br>
 */
interface TwinVQ extends Library {

    TwinVQ twinVq = new MyTwinVQ(); // Native.load("TwinVQ", TwinVQ.class);

    /** */
    static String asciiz(byte[] b) {
        return new String(b).replace("\u0000", "");
    }

//#region twinvq.h

    /** Initialization error code */
    enum InitErrorCode {
        /** no error */
        TVQ_NO_ERROR,
        /** general */
        TVQ_ERROR,
        /** wrong version */
        TVQ_ERROR_VERSION,
        /** channel setting error */
        TVQ_ERROR_CHANNEL,
        /** wrong coding mode */
        TVQ_ERROR_MODE,
        /** inner parameter setting error */
        TVQ_ERROR_PARAM,
        /** wrong number of VQ pre-selection candidates, used only in encoder */
        TVQ_ERROR_N_CAN,
    }

    /** version ID */
    int TVQ_UNKNOWN_VERSION = -1;

    int V2 = 0;

    int V2PP = 1;

    int N_VERSIONS = 2;

    /** window types */
    enum WindowType {
        ONLY_LONG_WINDOW,
        LONG_SHORT_WINDOW,
        ONLY_SHORT_WINDOW,
        SHORT_LONG_WINDOW,
        SHORT_MEDIUM_WINDOW,
        MEDIUM_LONG_WINDOW,
        LONG_MEDIUM_WINDOW,
        MEDIUM_SHORT_WINDOW,
        ONLY_MEDIUM_WINDOW,
    }

    /** block types */
    enum BlockType {
        BLK_SHORT,
        BLK_MEDIUM,
        BLK_LONG,
        BLK_PPC,
    }

    /** number of block types */
    int N_BTYPE = 3;

    /**
     * number of interleave types, enum BLOCK_TYPE is commonly used for
     * detecting interleave types.
     */
    int N_INTR_TYPE = 4;

    /** maximum number of channels */
    int N_CH_MAX = 2;

    /** type definition of code information interface */
    class Index extends Structure {
        /** block type */
        public final int[] w_type = new int[1];

        public final int[] btype = new int[1];

        /** FBC info */
        public final int[][] segment_sw = new int[N_CH_MAX][];

        public final int[][] band_sw = new int[N_CH_MAX][];

        public final int[][] fg_intensity = new int[N_CH_MAX][];

        /** VQ info */
        public int[] wvq;

        /** BSE info */
        public int[] fw;

        public int[] fw_alf;

        /** gain info */
        public int[] pow;

        /** LSP info */
        public final int[][] lsp = new int[N_CH_MAX][];

        /** PPC info */
        public final int[] pit = new int[N_CH_MAX];

        public int[] pls;

        public final int[] pgain = new int[N_CH_MAX];

        /** EBC info */
        public final int[][] bc = new int[N_CH_MAX][];

        public byte[] manager;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("w_type", "btype", "segment_sw", "band_sw", "fg_intensity", "wvq", "fw", "fw_alf",
                    "pow", "lsp", "pit", "pls", "pgain", "bc", "manager");
        }

        @Override public String toString() {
            return new StringJoiner(", ", Index.class.getSimpleName() + "[", "]")
                    .add("w_type=" + Arrays.toString(w_type))
                    .add("btype=" + Arrays.toString(btype))
                    .add("segment_sw=" + Arrays.toString(segment_sw))
                    .add("band_sw=" + Arrays.toString(band_sw))
                    .add("fg_intensity=" + Arrays.toString(fg_intensity))
                    .add("wvq=" + Arrays.toString(wvq))
                    .add("fw=" + Arrays.toString(fw))
                    .add("fw_alf=" + Arrays.toString(fw_alf))
                    .add("pow=" + Arrays.toString(pow))
                    .add("lsp=" + Arrays.toString(lsp))
                    .add("pit=" + Arrays.toString(pit))
                    .add("pls=" + Arrays.toString(pls))
                    .add("pgain=" + Arrays.toString(pgain))
                    .add("bc=" + Arrays.toString(bc))
                    .add("manager=" + manager)
                    .toString();
        }
    }

    /* type definition of tvqConfInfoSubBlock */
    class ConfInfoSubBlock extends Structure {
        /** subframe size */
        public int sf_sz;

        /** number of subframes */
        public int nsf;

        /** number of division of weighted interleave vector quantization */
        public final int[] ndiv = new int[1];

        /** number of Bark-scale subbands */
        public int ncrb;

        /** number of division of BSE VQ */
        public int fw_ndiv;

        /** number of bits for BSE VQ */
        public int fw_nbit;

        /** number of sub-blocks for gain coding */
        public int nsubg;

        /** PPC switch */
        public int ppc_enable;

        /** EBC switch */
        public int ebc_enable;

        /** EBC base band */
        public int ebc_crb_base;

        /** EBC bits */
        public int ebc_bits;

        /** FBC switch */
        public int fbc_enable;

        /** FBC number of segments */
        public int fbc_n_segment;

        /** FBC number of subbands */
        public int fbc_nband;

        /** FBC subband table */
        public int[] fbc_crb_tbl = new int[1]; // TODO

        @Override
        protected List<String> getFieldOrder() {
            return List.of("sf_sz", "nsf", "ndiv", "ncrb", "fw_ndiv", "fw_nbit", "nsubg", "ppc_enable",
                    "ebc_enable", "ebc_crb_base", "ebc_bits", "fbc_enable", "fbc_n_segment", "fbc_nband", "fbc_crb_tbl");
        }
    }

    /** type definition of tvqConfInfo */
    class ConfInfo extends Structure {
        /** frame configuration */
        public int N_CH;

        /** window type coding */
        public int BITS_WTYPE;

        /** LSP coding */
        public int LSP_BIT0;

        public int LSP_BIT1;

        public int LSP_BIT2;

        public int LSP_SPLIT;

        /** Bark-scale envelope coding */
        public int FW_ARSW_BITS;

        /** gain coding */
        public int GAIN_BITS;

        public int SUB_GAIN_BITS;

        /** pitch excitation */
        public int N_DIV_P;

        public int BASF_BIT;

        public int PGAIN_BIT;

        /** block type dependent parameters */
        public final ConfInfoSubBlock[/* N_BTYPE */] cfg = { new ConfInfoSubBlock(), new ConfInfoSubBlock(), new ConfInfoSubBlock() };

        @Override
        protected List<String> getFieldOrder() {
            return List.of("N_CH", "BITS_WTYPE", "LSP_BIT0", "LSP_BIT1", "LSP_BIT2", "LSP_SPLIT",
                    "FW_ARSW_BITS", "GAIN_BITS", "SUB_GAIN_BITS", "N_DIV_P", "BASF_BIT", "PGAIN_BIT", "cfg");
        }
    }

    /*
     * Definitions about TwinVQ bitstream header
     */
    int BUFSIZ = 1024;

    int KEYWORD_BYTES = 4;

    int VERSION_BYTES = 8;

    int ELEM_BYTES = 4; // sizeof(unsigned long) TODO

    /** */
    class HeaderInfo extends Structure {
        public byte[] id = new byte[KEYWORD_BYTES + VERSION_BYTES + 1];

        public int size;

        /* Common Chunk */
        /** channel mode (mono:0/stereo:1) */
        public int channelMode;

        /** bit rate (kbit/s) */
        public int bitRate;

        /** sampling rate (44.1 kHz -> 44) */
        public int samplingRate;

        /** security level (always 0) */
        public int securityLevel;

        /** Text Chunk */
        public final byte[] name = new byte[BUFSIZ];

        public final byte[] comt = new byte[BUFSIZ];

        public final byte[] auth = new byte[BUFSIZ];

        public final byte[] cpyr = new byte[BUFSIZ];

        public final byte[] file = new byte[BUFSIZ];

        /** add by OKAMOTO 99.12.21 */
        public final byte[] extr = new byte[BUFSIZ];

        /** Data size chunk */
        public int dsiz;

        /** extended tags - added by Pawel Garbacz */
        public final byte[] albm = new byte[BUFSIZ];

        public final byte[] year = new byte[BUFSIZ];

        public final byte[] trck = new byte[BUFSIZ];

        @Override
        protected List<String> getFieldOrder() {
            return List.of("id", "size", "channelMode", "bitRate", "samplingRate", "securityLevel", "name",
                    "comt", "auth", "cpyr", "file", "extr", "dsiz", "albm", "year", "trck");
        }

        @Override public String toString() {
            return new StringJoiner(", ", HeaderInfo.class.getSimpleName() + "[", "]")
                    .add("id=" + asciiz(id))
                    .add("size=" + size)
                    .add("channelMode=" + channelMode)
                    .add("bitRate=" + bitRate)
                    .add("samplingRate=" + samplingRate)
                    .add("securityLevel=" + securityLevel)
                    .add("name=" + asciiz(name))
                    .add("comt=" + asciiz(comt))
                    .add("auth=" + asciiz(auth))
                    .add("cpyr=" + asciiz(cpyr))
                    .add("file=" + asciiz(file))
                    .add("extr=" + asciiz(extr))
                    .add("dsiz=" + dsiz)
                    .add("albm=" + asciiz(albm))
                    .add("year=" + asciiz(year))
                    .add("trck=" + asciiz(trck))
                    .toString();
        }
    }

//#endregion

//#region tvqenc.h

    // encoding

    class EncSpecificInfo {
        int N_CAN_GLOBAL;
    }

//#endregion

//#region tvqdec.h

    int TvqEncInitialize(HeaderInfo setupInfo, EncSpecificInfo encInfo, Index index, int dispErrorMessageBox);

    void TvqEncTerminate(Index index);

    void TvqEncGetVectorInfo(int[][] bits0, int[][] bits1);

    void TvqEncResetFrameCounter();

    // TwinVQ encoder function
    void TvqEncodeFrame(float[] sig_in, Index index);

    void TvqEncUpdateVectorInfo(int varbits, int ndiv, int[] bits0, int[] bits1);

    void TvqEncSetFrameCounter(int position);

    // TwinVQ query functions
    int TvqEncGetFrameSize();

    int TvqEncGetNumChannels();

    int TvqEncGetNumFixedBitsPerFrame();

    void TvqEncGetSetupInfo(HeaderInfo setupInfo);

    float TvqEncGetSamplingRate();

    int TvqEncGetBitRate();

    void TvqEncGetConfInfo(ConfInfo cf);

    int TvqEncGetNumFrames();

    int TvqGetVersionID(int versionNum, String versionString);

    int TvqEncCheckVersion(String strTvqID);

    int TvqEncGetModuleVersion(String versionString);

    // decoding

    int TvqInitialize(HeaderInfo setupInfo, Index index, int dispErrorMessageBox);

    void TvqTerminate(Index index);

    void TvqGetVectorInfo(int[][] bits0, int[][] bits1);

    void TvqResetFrameCounter();

    // TwinVQ decoder function
    void TvqDecodeFrame(Index indexp, float[] out);

    int TvqWtypeToBtype(int w_type, int[] btype);

    void TvqUpdateVectorInfo(int varbits, int[] ndiv, int[] bits0, int[] bits1);

    void TvqSetFrameCounter(int position);

    /** TwinVQ query functions */
    int TvqCheckVersion(String versionID);

    /** setup information */
    void TvqGetSetupInfo(HeaderInfo setupInfo);

    /** configuration information */
    void TvqGetConfInfo(ConfInfo cf);

    /** frame size */
    int TvqGetFrameSize();

    /** number of channels */
    int TvqGetNumChannels();

    /** total bitrate */
    int TvqGetBitRate();

    /** sampling rate */
    float TvqGetSamplingRate();

    /** number of fixed bits per frame */
    int TvqGetNumFixedBitsPerFrame();

    /** number of decoded frame */
    int TvqGetNumFrames();

    int TvqGetModuleVersion(byte[] versionString);

//#region V2PLUS_SUPPORT

    // TwinVQ FB coding tool control

    /** count number of used bits */
    void TvqFbCountUsedBits(int nbit);

    /** query average bitrate for the tool */
    float TvqGetFbCurrentBitrate();

    /** query total number of used bits */
    int TvqGetFbTotalBits();

//#endregion

//#endregion
}
