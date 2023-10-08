/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq;



/**
 * TwinVQ
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070202 initial version <br>
 */
final class TwinVQ {

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
    static final int TVQ_UNKNOWN_VERSION = -1;

    static final int V2 = 0;

    static final int V2PP = 1;

    static final int N_VERSIONS = 2;

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
    static final int N_BTYPE = 3;

    /**
     * number of interleave types, enum BLOCK_TYPE is commonly used for
     * detecting interleave types.
     */
    static final int N_INTR_TYPE = 4;

    /** maximum number of channels */
    static final int N_CH_MAX = 2;

    /** type definition of code information interface */
    static class Index {
        /** block type */
        int[] w_type = new int[1];

        int[] btype = new int[1];

        /** FBC info */
        int[][] segment_sw = new int[N_CH_MAX][];

        int[][] band_sw = new int[N_CH_MAX][];

        int[][] fg_intensity = new int[N_CH_MAX][];

        /** VQ info */
        int[] wvq;

        /** BSE info */
        int[] fw;

        int[] fw_alf;

        /** gain info */
        int[] pow;

        /** LSP info */
        int[][] lsp = new int[N_CH_MAX][];

        /** PPC info */
        int[] pit = new int[N_CH_MAX];

        int[] pls;

        int[] pgain = new int[N_CH_MAX];

        /** EBC info */
        int[][] bc = new int[N_CH_MAX][];

        Void manager;
    }

    /* type definition of tvqConfInfoSubBlock */
    static class ConfInfoSubBlock {
        /** subframe size */
        int sf_sz;

        /** number of subframes */
        int nsf;

        /** number of division of weighted interleave vector quantization */
        int[] ndiv = new int[1];

        /** number of Bark-scale subbands */
        int ncrb;

        /** number of division of BSE VQ */
        int fw_ndiv;

        /** number of bits for BSE VQ */
        int fw_nbit;

        /** number of sub-blocks for gain coding */
        int nsubg;

        /** PPC switch */
        int ppc_enable;

        /** EBC switch */
        int ebc_enable;

        /** EBC base band */
        int ebc_crb_base;

        /** EBC bits */
        int ebc_bits;

        /** FBC switch */
        int fbc_enable;

        /** FBC number of segments */
        int fbc_n_segment;

        /** FBC number of subbands */
        int fbc_nband;

        /** FBC subband table */
        int[] fbc_crb_tbl;
    }

    /** type definition of tvqConfInfo */
    static class ConfInfo {
        /** frame configuration */
        int N_CH;

        /** window type coding */
        int BITS_WTYPE;

        /** LSP coding */
        int LSP_BIT0;

        int LSP_BIT1;

        int LSP_BIT2;

        int LSP_SPLIT;

        /** Bark-scale envelope coding */
        int FW_ARSW_BITS;

        /** gain coding */
        int GAIN_BITS;

        int SUB_GAIN_BITS;

        /** pitch excitation */
        int N_DIV_P;

        int BASF_BIT;

        int PGAIN_BIT;

        /** block type dependent parameters */
        ConfInfoSubBlock[] cfg = new ConfInfoSubBlock[N_BTYPE];
    }

    /*
     * Definitions about TwinVQ bitstream header
     */
    static final int BUFSIZ = 1024;

    static final int KEYWORD_BYTES = 4;

    static final int VERSION_BYTES = 8;

    static final int ELEM_BYTES = 8 /* sizeof(unsigned long) */;

    /*
     */
    static class HeaderInfo {
        byte[] id = new byte[KEYWORD_BYTES + VERSION_BYTES + 1];

        int size;

        /* Common Chunk */
        /** channel mode (mono:0/stereo:1) */
        int channelMode;

        /** bit rate (kbit/s) */
        int bitRate;

        /** sampling rate (44.1 kHz -> 44) */
        int samplingRate;

        /** security level (always 0) */
        int securityLevel;

        /** Text Chunk */
        byte[] name = new byte[BUFSIZ];

        byte[] comt = new byte[BUFSIZ];

        byte[] auth = new byte[BUFSIZ];

        byte[] cpyr = new byte[BUFSIZ];

        byte[] file = new byte[BUFSIZ];

        /** add by OKAMOTO 99.12.21 */
        byte[] extr = new byte[BUFSIZ];

        /** Data size chunk */
        int dsiz;

        /** extended tags - added by Pawel Garbacz */
        byte[] albm = new byte[BUFSIZ];

        byte[] year = new byte[BUFSIZ];

        byte[] trck = new byte[BUFSIZ];
    }

    // encoding

    static class EncSpecificInfo {
        int N_CAN_GLOBAL;
    }

    native int TvqEncInitialize(HeaderInfo setupInfo, EncSpecificInfo encInfo, Index index, int dispErrorMessageBox);

    native void TvqEncTerminate(Index index);

    native void TvqEncGetVectorInfo(int[][] bits0, int[][] bits1);

    native void TvqEncResetFrameCounter();

    // TwinVQ encoder function
    native void TvqEncodeFrame(float[] sig_in, Index index);

    native void TvqEncUpdateVectorInfo(int varbits, int ndiv, int[] bits0, int[] bits1);

    native void TvqEncSetFrameCounter(int position);

    // TwinVQ query functions
    native int TvqEncGetFrameSize();

    native int TvqEncGetNumChannels();

    native int TvqEncGetNumFixedBitsPerFrame();

    native void TvqEncGetSetupInfo(HeaderInfo setupInfo);

    native float TvqEncGetSamplingRate();

    native int TvqEncGetBitRate();

    native void TvqEncGetConfInfo(ConfInfo cf);

    native int TvqEncGetNumFrames();

    native int TvqGetVersionID(int versionNum, String versionString);

    native int TvqEncCheckVersion(String strTvqID);

    native int TvqEncGetModuleVersion(String versionString);

    // decoding

    native int TvqInitialize(HeaderInfo setupInfo, Index index, int dispErrorMessageBox);

    native void TvqTerminate(Index index);

    native void TvqGetVectorInfo(int[][] bits0, int[][] bits1);

    native void TvqResetFrameCounter();

    // TwinVQ decoder function
    native void TvqDecodeFrame(Index indexp, float[] out);

    native int TvqWtypeToBtype(int w_type, int[] btype);

    native void TvqUpdateVectorInfo(int varbits, int[] ndiv, int[] bits0, int[] bits1);

    native void TvqSetFrameCounter(int position);

    /** TwinVQ query functions */
    native int TvqCheckVersion(String versionID);

    /** setup information */
    native void TvqGetSetupInfo(HeaderInfo setupInfo);

    /** configuration information */
    native void TvqGetConfInfo(ConfInfo cf);

    /** frame size */
    native int TvqGetFrameSize();

    /** number of channels */
    native int TvqGetNumChannels();

    /** total bitrate */
    native int TvqGetBitRate();

    /** sampling rate */
    native float TvqGetSamplingRate();

    /** number of fixed bits per frame */
    native int TvqGetNumFixedBitsPerFrame();

    /** number of decoded frame */
    native int TvqGetNumFrames();

    native int TvqGetModuleVersion(String versionString);

    // V2PLUS SUPPORT
    /**
     * TwinVQ FB coding tool control count number of used bits
     */
    native void TvqFbCountUsedBits(int nbit);

    /** query average bitrate for the tool */
    native float TvqGetFbCurrentBitrate();

    /** query total number of used bits */
    native int TvqGetFbTotalBits();

    //

    private static final TwinVQ instance = new TwinVQ();

    private TwinVQ() {
    }

    public static TwinVQ getInstance() {
        return instance;
    }
}

/* */
