/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq.obsolate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import vavi.sound.twinvq.obsolate.TwinVQ.HeaderInfo;
import vavi.sound.twinvq.obsolate.TwinVQ.Index;

import static vavi.sound.twinvq.obsolate.TwinVQ.twinVq;


/**
 * Dreamplayer - multi format sound player
 */
class Player {

    /** */
    static final BStream bstream = BStream.getInstance();

    private static final int INIT_ERR_DISP_MBOX = 0;

    static final int N = 64;

    /** */
    public static void main(String[] args) throws Exception {
        new Player(args[0]);
    }

    /**
     * @param filename in file
     */
    Player(String filename) throws Exception {
        BFile bfile = new BFile(filename, "r");

        ChunkChunk twinChunk = bstream.getBsHeaderInfo(bfile);
        if (twinChunk == null) {
            throw new IllegalStateException("twinChunk=null");
        }
        HeaderManager headerManager = HeaderManager.create(twinChunk);
        if (headerManager == null) {
            throw new IllegalStateException("theHeaderManager=null");
        }
        HeaderInfo setupInfo = getStandardChunkInfo(headerManager);

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
        int counter = 0 /*, acttime = 0 */;
        short[] bufS = new short[buf.length / Short.BYTES];
        ShortBuffer sb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();

        while (bstream.readBsFrame(index, bfile) != 0 /* && !skip_song */) {
            twinVq.TvqDecodeFrame(index, frame);
            fr2buf(frame, bufS, frameSize, channels);
            sb.put(bufS);
            sb.flip();
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

        BStream.getString(lbuf, TwinVQ.KEYWORD_BYTES, bfile);
        if (Arrays.equals(lbuf, "DATA".getBytes())) {
            throw new IllegalStateException("DEBUG: get_info . no 'DATA' chumk was found");
        }

        HeaderManager theHeaderManager = HeaderManager.create(TwinChunk);

        if (theHeaderManager == null) {
            return null;
        }

        HeaderInfo setupInfo = getStandardChunkInfo(theHeaderManager);
        return setupInfo;
    }

    /**
     * Copies frame data to output buffer.
     *
     * @param out input data frame
     * @param bufout output data buffer array
     * @param frameSize frame size
     * @param numChannels number of channels
     */
    static void fr2buf(float[] out, short[] bufout, int frameSize, int numChannels) {
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
                    bufout[ismp * numChannels + ich] = (short) (dtmp + 0.5);
                } else {
                    if (dtmp < -32700.) {
                        dtmp = -32700f;
                        // lflag = 1;
                    }
                    bufout[ismp * numChannels + ich] = (short) (dtmp - 0.5);
                }
            }
        }
    }

    private static HeaderInfo getStandardChunkInfo(HeaderManager theManager) {
        HeaderInfo setupInfo = new HeaderInfo();
        setupInfo.id = theManager.getID().getBytes();
        CommChunk commChunk = new CommChunk(theManager.getPrimaryChunk("COMM"), "TWIN97012000");
        setupInfo.channelMode = commChunk.getChannelMode();
        setupInfo.bitRate = commChunk.getBitRate();
        setupInfo.samplingRate = commChunk.getSamplingRate();
        setupInfo.securityLevel = commChunk.getSecurityLevel();
        return setupInfo;
    }
}
