/*
 * https://github.com/lostromb/concentus
 */

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.concentus.OpusApplication;
import org.concentus.OpusDecoder;
import org.concentus.OpusEncoder;
import org.concentus.OpusMode;
import org.concentus.OpusSignal;
import org.gagravarr.opus.OpusAudioData;
import org.gagravarr.opus.OpusFile;
import org.gagravarr.opus.OpusInfo;
import org.gagravarr.opus.OpusTags;
import org.junit.Test;


/**
 *
 * @author lostromb
 */
public class OpusTest {

    /**
     * @param args 0: in.raw, 1: out.opus
     */
    public static void main(String[] args) throws Exception {
        String in = args[0];
        String out = args[1];

        InputStream is = new FileInputStream(in);
        OpusEncoder encoder = new OpusEncoder(48000, 2, OpusApplication.OPUS_APPLICATION_AUDIO);
        encoder.setBitrate(96000);
        encoder.setSignalType(OpusSignal.OPUS_SIGNAL_MUSIC);
        encoder.setComplexity(10);

        OutputStream os = new FileOutputStream(out);
        OpusInfo info = new OpusInfo();
        info.setNumChannels(2);
        info.setSampleRate(48000);
        OpusTags tags = new OpusTags();
        tags.setVendor("Concentus");
        tags.addComment("title", "A test!");
        OpusFile file = new OpusFile(os, info, tags);
        int packetSamples = 960;
        byte[] inBuf = new byte[packetSamples * 2 * 2];
        byte[] dataPacket = new byte[1275];
        long start = System.currentTimeMillis();
        while (is.available() >= inBuf.length) {
            int bytesRead = is.read(inBuf, 0, inBuf.length);
            short[] pcm = bytesToShorts(inBuf, 0, inBuf.length);
            int bytesEncoded = encoder.encode(pcm, 0, packetSamples, dataPacket, 0, 1275);
            byte[] packet = new byte[bytesEncoded];
            System.arraycopy(dataPacket, 0, packet, 0, bytesEncoded);
            OpusAudioData data = new OpusAudioData(packet);
            file.writeAudioData(data);
        }
        file.close();

        long end = System.currentTimeMillis();
        System.err.println(in + " -> " + out);
        System.err.println("Time was " + (end - start) + "ms");
        is.close();
        os.close();
        System.err.println("Done!");
    }

    static final String inFile = "tmp/hoshi.raw";
    static final String outFile = "tmp/hoshi2.raw";

    @Test
    public void test() throws Exception {
        FileInputStream fileIn = new FileInputStream(inFile);
        OpusEncoder encoder = new OpusEncoder(48000, 2, OpusApplication.OPUS_APPLICATION_AUDIO);
        encoder.setBitrate(96000);
        encoder.setForceMode(OpusMode.MODE_CELT_ONLY);
        encoder.setSignalType(OpusSignal.OPUS_SIGNAL_MUSIC);
        encoder.setComplexity(0);

        OpusDecoder decoder = new OpusDecoder(48000, 2);

        FileOutputStream fileOut = new FileOutputStream(outFile);
        int packetSamples = 960;
        byte[] inBuf = new byte[packetSamples * 2 * 2];
        byte[] dataPacket = new byte[1275];
        long start = System.currentTimeMillis();
        while (fileIn.available() >= inBuf.length) {
            int bytesRead = fileIn.read(inBuf, 0, inBuf.length);
            short[] pcm = bytesToShorts(inBuf, 0, inBuf.length);
            int bytesEncoded = encoder.encode(pcm, 0, packetSamples, dataPacket, 0, 1275);
//                System.out.println(bytesEncoded + " bytes encoded");

            int samplesDecoded = decoder.decode(dataPacket, 0, bytesEncoded, pcm, 0, packetSamples, false);
//                System.out.println(samplesDecoded + " samples decoded");
            byte[] bytesOut = shortsToBytes(pcm);
            fileOut.write(bytesOut, 0, bytesOut.length);
        }

        long end = System.currentTimeMillis();
        System.out.println("Time was " + (end - start) + "ms");
        fileIn.close();
        fileOut.close();
        System.out.println("Done!");
    }

    /**
     * Converts interleaved byte samples (such as what you get from a capture device)
     * into linear short samples (that are much easier to work with)
     */
    static short[] bytesToShorts(byte[] input) {
        return bytesToShorts(input, 0, input.length);
    }

    /**
     * Converts interleaved byte samples (such as what you get from a capture device)
     * into linear short samples (that are much easier to work with)
     */
    static short[] bytesToShorts(byte[] input, int offset, int length) {
        short[] processedValues = new short[length / 2];
        for (int c = 0; c < processedValues.length; c++) {
            short a = (short) ((input[(c * 2) + offset]) & 0xff);
            short b = (short) ((input[(c * 2) + 1 + offset]) << 8);
            processedValues[c] = (short) (a | b);
        }

        return processedValues;
    }

    /**
     * Converts linear short samples into interleaved byte samples, for writing to a file, waveout device, etc.
     */
    static byte[] shortsToBytes(short[] input) {
        return shortsToBytes(input, 0, input.length);
    }

    /**
     * Converts linear short samples into interleaved byte samples, for writing to a file, waveout device, etc.
     */
    static byte[] shortsToBytes(short[] input, int offset, int length) {
        byte[] processedValues = new byte[length * 2];
        for (int c = 0; c < length; c++) {
            processedValues[c * 2] = (byte) (input[c + offset] & 0xff);
            processedValues[c * 2 + 1] = (byte) ((input[c + offset] >> 8) & 0xff);
        }

        return processedValues;
    }
}
