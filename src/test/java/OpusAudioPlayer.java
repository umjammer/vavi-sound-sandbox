/*
 * https://github.com/lostromb/concentus
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.concentus.OpusDecoder;
import org.concentus.OpusException;
import org.gagravarr.opus.OpusAudioData;

import vavi.sound.sampled.opus.OpusInpputStream;


/**
 * OpusAudioPlayer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/11/18 umjammer initial version <br>
 */
public class OpusAudioPlayer {
    private static int BUFFER_SIZE = 1024 * 1024;
    private static int INPUT_SAMPLERATE = 48000;
    private static int OUTPUT_SAMPLERATE = 48000;

    private OpusInpputStream oggFile;
    private OpusDecoder decoder;

    private ByteBuffer decodeBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    private int channels;

    public static void main(String[] args) throws Exception {
        OpusAudioPlayer opusAudioPlayer = new OpusAudioPlayer(new File(args[0]));
        opusAudioPlayer.play();
    }

    public OpusAudioPlayer(File audioFile) throws IOException {
        try {
            oggFile = new OpusInpputStream(new FileInputStream(audioFile));
            decoder = new OpusDecoder(INPUT_SAMPLERATE, 2);
            channels = oggFile.getInfo().getNumChannels();
        } catch (OpusException e) {
            throw new IOException(e);
        }
    }

    private byte[] decode(byte[] packetData) throws OpusException {
        int decodedSamples = decoder.decode(packetData, 0, packetData.length, decodeBuffer.array(), 0, BUFFER_SIZE, false);
        if (decodedSamples < 0) {
            System.err.println("Decode error: " + decodedSamples);
            decodeBuffer.clear();
            return null;
        }
        decodeBuffer.position(decodedSamples * 2 * channels); // 2 bytes per sample
        decodeBuffer.flip();

        byte[] decodedData = new byte[decodeBuffer.remaining()];
        decodeBuffer.get(decodedData);
        decodeBuffer.flip();
        return decodedData;
    }

    public void play() throws IOException {
        try {
            int totalDecodedBytes = 0;
            SourceDataLine line = AudioSystem.getSourceDataLine(new AudioFormat(OUTPUT_SAMPLERATE, 16, channels, true, false));
            line.open();
            line.start();
            OpusAudioData nextPacket = oggFile.getNextAudioPacket();
            while (nextPacket != null) {
                byte[] decodedData = decode(nextPacket.getData());
                if (decodedData != null) {
                    // Write packet to SourceDataLine
                    line.write(decodedData, 0, decodedData.length);
                    totalDecodedBytes += decodedData.length;
                }
                nextPacket = oggFile.getNextAudioPacket();
            }
            line.drain();
            line.close();
            System.err.println(String.format("Decoded to %d bytes", totalDecodedBytes));
        } catch (LineUnavailableException | OpusException e) {
            throw new IOException(e);
       }
    }
}