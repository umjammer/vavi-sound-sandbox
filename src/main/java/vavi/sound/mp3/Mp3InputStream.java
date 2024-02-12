/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mp3;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Mp3InputStream.
 * <li> TODO don't use {@link #read()} currently
 * 
 * @author 小杉 篤史 (Kosugi Atsushi)
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 1.00 original version <br>
 * @version 2.00 030817 nsano java port <br>
 */
public class Mp3InputStream extends FilterInputStream {

    /** */
    private final Mp3Decoder.MpegDecodeInfo decodeInfo;

    /** */
    private final Mp3Decoder decoder = new Mp3Decoder();

    /**
     * @throws IOException
     */
    public Mp3InputStream(InputStream in) throws IOException {
        super(in);

        // check stream
        if (!in.markSupported()) {
            throw new IllegalArgumentException("mark not supported");
        }
        int size = in.available();
        in.mark(4096);

        // read temporary
        byte[] buf = new byte[4096]; // TODO assume there is firstSync in 4096 byte
        int readBytes = 0;
        while (readBytes < buf.length) {
             int l = in.read(buf, readBytes, buf.length - readBytes);
             if (l == -1) {
                 throw new EOFException("unexpected EOF");
             }
             readBytes += l;
        }

        // find first sync
        int firstSyncAddress = Mp3Decoder.findSync(buf, 0, readBytes);
Debug.printf("firstSyncAddress: %08x", firstSyncAddress);
        decodeInfo = decoder.getInfo(buf, firstSyncAddress, readBytes - firstSyncAddress);
Debug.println(StringUtil.paramStringDeep(decodeInfo, 2));

        //
        in.reset();
Debug.println("mp3 in.available(): " + in.available());
        int length = firstSyncAddress;
//      int length = firstSyncAddress + 4 + (decodeInfo.header.mode != 3 ? 32 : 17);
Debug.println("skip length: " + length);
        int skipBytes = 0;
        while (skipBytes < length) {
            int l  = (int) in.skip(length - skipBytes);
            if (l == -1) {
                throw new EOFException("unexpected EOF");
            }
            skipBytes += l;
        }

        // collect data
        int dataSize = size - firstSyncAddress;
        int frames = dataSize / decodeInfo.inputSize;

        int channels = decodeInfo.channels;
        int frequency = decodeInfo.frequency;
        int bitRate = decodeInfo.bitRate;
        int totalFrames = frames;
        int totalSamples = (frames * decodeInfo.outputSize * 8) / 16 / decodeInfo.channels;
        int totalSeconds = totalSamples / decodeInfo.frequency;

Debug.println("---- mp3 ----");
Debug.println("size        : " + size);
Debug.println("dataSize    : " + dataSize);

Debug.println("channels    : " + channels);
Debug.println("frequency   : " + frequency + " [Hz]");
Debug.println("bitRate     : " + bitRate + " [bit/s]");
Debug.println("totalFrames : " + totalFrames);
Debug.println("totalSamples: " + totalSamples);
Debug.println("totalSeconds: " + totalSeconds + " [s]");
    }

    /**
     * @see java.io.InputStream#available()
     * @throws IOException
     */
    @Override
    public int available() throws IOException {
Debug.println("wave available: " + in.available() + ", " + decodeInfo.inputSize + ", " + decodeInfo.outputSize);
        return in.available() / decodeInfo.inputSize * decodeInfo.outputSize;
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException("use #read(byte[], int, int)");
    }

    /**
     * @see java.io.InputStream#read(byte[], int, int)
     * @throws IllegalArgumentException When length is shorter than DecodeInfo#outputSize to be thrown.
     */
    @Override
    public int read(byte[] data, int offset, int length) throws IOException {

        if (!skipNextSync()) {
Debug.println("End of Frames");
            return -1;
        }

        Mp3Decoder.MpegDecodeParam param = new Mp3Decoder.MpegDecodeParam();

//Debug.println("length: " + length);
//Debug.println("decodeInfo: " + StringUtil.paramString(decodeInfo));
        param.inputSize = decodeInfo.inputSize;
//Debug.println("param.inputSize: " + param.inputSize);
        param.inputBuf = new byte[param.inputSize];
        int readBytes = 0;
        while (readBytes < param.inputSize) {
            int l = in.read(param.inputBuf, readBytes, param.inputSize - readBytes);
            if (l == -1) {
                throw new EOFException("unexpected EOF");
            }
            readBytes += l;
        }

        //
Debug.println("mp3:\n" + StringUtil.getDump(param.inputBuf, 16));
        decoder.prepareDecode(param.inputBuf, 0, param.inputSize);

        param.outputSize = decodeInfo.outputSize;
        param.outputBuf = data;

        decoder.decode(param);

        return param.outputSize;
    }

    /**
     * @throws IOException
     */
    public void seekByFrame(int frame) throws IOException {
        int sample = (frame * decodeInfo.outputSize * 8) / 16 / decodeInfo.channels;
        seek(sample);
    }

    /**
     * @throws IOException
     */
    public void seekBySec(int sec) throws IOException {
        int sample = sec * decodeInfo.frequency;
        seek(sample);
    }

    /**
     * @throws IOException
     */
    private boolean skipNextSync() throws IOException {

        while (true) {

            //
            in.mark(1024);

            // read temporary
            byte[] buf = new byte[decodeInfo.inputSize];
            int readBytes = 0;
            while (readBytes < buf.length) {
                 int l = in.read(buf, readBytes, buf.length - readBytes);
                 if (l == -1) {
                     throw new EOFException("unexpected EOF");
                 }
                 readBytes += l;
            }

            try {
                int syncAddress = Mp3Decoder.findSync(buf, 0, readBytes);
                in.reset();
                int length = syncAddress;
                int l = 0;
                while (l < length) {
                    l += (int) in.skip(length - l);
                }
Debug.println("skip: " + length);
                return true;
            } catch (IllegalArgumentException e) {
Debug.println("no more sync");
                in.reset();
                return false; // no more data
            }
        }
    }

    /**
     * @throws IOException
     */
    private void seek(int seekSample) throws IOException {
        //
        Mp3Decoder.MpegDecodeParam param = new Mp3Decoder.MpegDecodeParam();

        param.inputSize = decodeInfo.inputSize;
        param.inputBuf = new byte[param.inputSize];

exit:
        while (true) {
            int readBytes = 0;
            readBytes = in.read(param.inputBuf, readBytes, param.inputSize - readBytes);

            if (readBytes == 0) {
                // no more data
                break;
            }

            //
            while (true) {

                try {
                    decoder.decode(param);
                } catch (IllegalArgumentException e) {
                    if (!skipNextSync()) {
                        break exit;
                    }
                    continue;
                }

                int firstSample = (param.outputSize * 8) / decodeInfo.bitRate / decodeInfo.channels;

                if (firstSample >= seekSample) {
                    break exit;
                }
            }
        }
    }
}

/* */
