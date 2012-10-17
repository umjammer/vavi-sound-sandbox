/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mp3;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Mp3InputStream.
 * <li> TODO 今のところ {@link #read()} を使用してはいけません。
 * 
 * @author 小杉 篤史 (Kosugi Atsushi)
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 1.00 original version <br>
 * @version 2.00 030817 nsano java port <br>
 */
public class Mp3InputStream extends FilterInputStream {

    /** */
    private Mp3Decoder.MpegDecodeInfo decodeInfo;

    /** */
    private Mp3Decoder decoder = new Mp3Decoder();

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
        byte[] buf = new byte[4096]; // TODO 4096 byte までに firstSync があると仮定している 
        int readBytes = 0;
        while (readBytes < buf.length) {
             int l = in.read(buf, readBytes, buf.length - readBytes);
             if (l == -1) {
                 throw new EOFException("unexpected EOF");
             }
             readBytes += l;
        }

        // find first sync
        int firstSyncAddress = decoder.findSync(buf, 0, readBytes);
System.err.println("firstSyncAddress: " + StringUtil.toHex8(firstSyncAddress));
        decodeInfo = decoder.getInfo(buf, firstSyncAddress, readBytes - firstSyncAddress);
System.err.println(StringUtil.paramStringDeep(decodeInfo));
        
        //
        in.reset();
System.err.println("mp3 in.available(): " + in.available());
        int length = firstSyncAddress;
//  	int length = firstSyncAddress + 4 + (decodeInfo.header.mode != 3 ? 32 : 17);
System.err.println("skip length: " + length);
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

System.err.println("---- mp3 ----");
System.err.println("size        : " + size);
System.err.println("dataSize    : " + dataSize);

System.err.println("channels    : " + channels);
System.err.println("frequency   : " + frequency + " [Hz]");
System.err.println("bitRate     : " + bitRate + " [bit/s]");
System.err.println("totalFrames : " + totalFrames);
System.err.println("totalSamples: " + totalSamples);
System.err.println("totalSeconds: " + totalSeconds + " [s]");
    }

    /**
     * @see java.io.InputStream#available()
     * @throws IOException
     */
    public int available() throws IOException {
System.err.println("wave available: " + in.available() + ", " + decodeInfo.inputSize + ", " + decodeInfo.outputSize);
        return in.available() / decodeInfo.inputSize * decodeInfo.outputSize;
    }

    /**
     * @throws UnsupportedOperationException
     */
    public int read() throws IOException {
        throw new UnsupportedOperationException("use #read(byte[], int, int)");
    }

    /**
     * @see java.io.InputStream#read(byte[], int, int)
     * @throws IllegalArgumentException When length is short than DecodeInfo#outputSize to be thrown.
     */
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
                int syncAddress = decoder.findSync(buf, 0, readBytes);
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
    
    //----
    
    /**
     * The program entry point.
     */
    public static void main(String[] args) throws Exception {

        int sampleRate = 41100;
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
Debug.println(format);

        InputStream is = new Mp3InputStream(new BufferedInputStream(new FileInputStream(args[0])));
Debug.println("available: " + is.available());

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        byte[] buf = new byte[4608];
        int l = 0;

        while (is.available() > 0) {
            l = is.read(buf, 0, buf.length);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();

        System.exit(0);
    }
}

/* */
