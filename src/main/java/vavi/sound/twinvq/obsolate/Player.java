/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq.obsolate;

import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import vavi.sound.twinvq.obsolate.TwinVQ.HeaderInfo;
import vavi.sound.twinvq.obsolate.TwinVQ.Index;


/**
 * Dreamplayer - multi format sound player
 */
class Player {
    /** */
    static BStream bstream = BStream.getInstance();

    /** */
    TwinVQ twinVq = TwinVQ.getInstance();

    private int INIT_ERR_DISP_MBOX;

    static final int N = 64;

    /** */
    public static void main(String[] args) throws Exception {
        new Player(args[0]);
    }

    Player(String filename) throws Exception {
        BFile bfile = new BFile(filename, "rb");

        ChunkChunk twinChunk = bstream.getBsHeaderInfo(bfile);
        if (twinChunk == null) {
            throw new IllegalStateException("twinChunk=null");
        }
        HeaderManager headerManager = HeaderManager.create(twinChunk);
        if (headerManager == null) {
            throw new IllegalStateException("theHeaderManager=null");
        }
        HeaderInfo setupInfo = new HeaderInfo();
        getStandardChunkInfo(headerManager, setupInfo);

        Index index = new Index();
        int errcode = twinVq.TvqInitialize(setupInfo, index, INIT_ERR_DISP_MBOX);
        if (errcode != 0) {
            throw new IllegalStateException("TvqInitialize failed.");
        }
        if (bstream.initBsReader(setupInfo) != 0) {
            throw new IllegalStateException("TvqInitialize failed.");
        }

        int frameSize;
        float[] frame;
        byte[] buf;
        frameSize = twinVq.TvqGetFrameSize();
        int frequency = 8000, channels = twinVq.TvqGetNumChannels();
        frame = new float[frameSize * channels];
        buf = new byte[frameSize * channels * 2];

        // init audio
        frequency = switch (setupInfo.samplingRate) {
            case 44 -> 44100;
            default -> frequency;
        };

        AudioFormat format = new AudioFormat(
             AudioFormat.Encoding.PCM_SIGNED,
             frequency,
             16,
             2,
             4,
             frequency * 2,
             false);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        // decoding
//      byte hours, minutes, secs;
        int wout_flag = 2;
        @SuppressWarnings("unused")
        int counter = 0/*, acttime = 0*/;

        while (bstream.readBsFrame(index, bfile) != 0/* &&!skip_song */) {
            twinVq.TvqDecodeFrame(index, frame);
            froat2buf(frame, buf, frameSize, channels);
            if (wout_flag == 0) {
                counter += line.write(buf, 0, frameSize * channels * 2 /* sizeof(short) */);
            }

            wout_flag = Math.max(wout_flag - 1, 0);
        }
        line.drain();
        line.stop();
        line.close();
        bfile.bclose();
    }

    /**
     * 
     */
    HeaderInfo getInfo(BFile bfile, byte[] marker) throws IOException {
        byte[] chunkID = new byte[TwinVQ.KEYWORD_BYTES + TwinVQ.VERSION_BYTES + 1];
        System.arraycopy(marker, 0, chunkID, 0, 4);
        bfile.io.read(chunkID, 4, TwinVQ.VERSION_BYTES);
        int TVQ_version = twinVq.TvqCheckVersion(new String(chunkID));
        if (TVQ_version == TwinVQ.TVQ_UNKNOWN_VERSION) {
            return null;
        }

        byte[] chunkSizeBytes = new byte[4];
        bfile.io.read(chunkSizeBytes, 0, 4);
        int chunkSize = 0; // TODO

        byte[] chunkData = new byte[chunkSize + 1];

        bfile.io.read(chunkData, 0, chunkSize);

        ChunkChunk TwinChunk = new ChunkChunk(new String(chunkID));
        TwinChunk.putData(chunkSize, chunkData);
        byte[] lbuf = new byte[TwinVQ.BUFSIZ];

        getString(lbuf, TwinVQ.KEYWORD_BYTES, bfile);
        if (Arrays.equals(lbuf, "DATA".getBytes())) {
            throw new IllegalStateException("DEBUG: get_info . no 'DATA' chumk was found");
        }

        HeaderManager theHeaderManager = HeaderManager.create(TwinChunk);

        if (theHeaderManager == null) {
            return null;
        }

        HeaderInfo setupInfo = new HeaderInfo();
        getStandardChunkInfo(theHeaderManager, setupInfo);
        return setupInfo;
    }

    static void froat2buf(float[] out, byte[] bufout, int frameSize, int numChannels) {
        int ismp, ich/* , uflag, lflag */;
        int ptr;
        float dtmp;

        // uflag = lflag = 0;

        for (ich = 0; ich < numChannels; ich++) {
            ptr = ich * frameSize; // out
            for (ismp = 0; ismp < frameSize; ismp++) {
                dtmp = out[ptr + ismp];
                if (dtmp >= 0.) {
                    if (dtmp > 32700.) {
                        dtmp = 32700f;
                        // uflag = 1;
                    }
                    bufout[ismp * numChannels + ich] = (byte) (dtmp + 0.5); // TODO float -> short
                } else {
                    if (dtmp < -32700.) {
                        dtmp = -32700f;
                        // lflag = 1;
                    }
                    bufout[ismp * numChannels + ich] = (byte) (dtmp - 0.5); // TODO float -> short
                }
            }
        }
    }

    private static int getString(byte[] buf, int nbytes, BFile bfile) throws IOException {
        int ichar, ibit;
        int[] c = new int[1];

        for (ichar = 0; ichar < nbytes; ichar++) {
            ibit = bfile.getBStream(c, 0, BFile.CHAR_BITS);
            if (ibit < BFile.CHAR_BITS) {
                break;
            }
            buf[ichar] = (byte) c[0];
        }

        buf[ichar] = '\0';
        return ichar;
    }

    private static int getStandardChunkInfo(HeaderManager theManager, HeaderInfo setupInfo) {
        setupInfo = new HeaderInfo();
        setupInfo.id = theManager.getID().getBytes();
        CommChunk commChunk = new CommChunk(theManager.getPrimaryChunk("COMM"), "TWIN97012000");
        setupInfo.channelMode = commChunk.getChannelMode();
        setupInfo.bitRate = commChunk.getBitRate();
        setupInfo.samplingRate = commChunk.getSamplingRate();
        setupInfo.securityLevel = commChunk.getSecurityLevel();
        return 0;
    }
}
