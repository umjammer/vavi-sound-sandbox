/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.SourceDataLine;

import vavi.sound.twinvq.LibAV.AVCodecContext;
import vavi.sound.twinvq.LibAV.AVFormatContext;
import vavi.sound.twinvq.LibAV.AVFrame;
import vavi.sound.twinvq.LibAV.AVInputFormat;
import vavi.sound.twinvq.LibAV.AVPacket;
import vavi.sound.twinvq.TwinVQDec.TwinVQContext;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavi.util.properties.annotation.PropsEntity.Util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static vavi.sound.SoundUtil.volume;


@PropsEntity(url = "file:local.properties")
class TwinVQTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "twinvq")
    String twinvq = "src/test/resources/test.vqf";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            Util.bind(this);
        }
Debug.print("volume: " + volume);
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    // ----

    int somefun(int x) throws IOException { return 0; }

    @FunctionalInterface
    interface ThrowingFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    ThrowingFunction<Integer, Integer, IOException> fun;

    @Test
    void testF() {
        fun = this::somefun;
    }

    // ----

    @Test
    void test1() throws Exception {
        TwinVQData d = new TwinVQData();
//        IntStream.range(0, d.cb0808l0.length).map(i -> d.cb0808l0[i]).forEach(System.err::println);
    }

    String out = "tmp/twinvq-vavi-out.pcm";

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void testFF() throws Exception {

        AVInputFormat inputFormat = VFQ.ff_vqf_demuxer;

        // probe
        DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(Path.of(twinvq))));
        dis.mark(12);
        byte[] probe = new byte[12];
        dis.readFully(probe);
        dis.reset();
        if (inputFormat.read_probe.apply(probe) == 0) {
            throw new IllegalArgumentException("not vfq");
        }

        // header
        AVFormatContext formatContext = new AVFormatContext();
        formatContext.pb = dis;
        inputFormat.read_header.apply(formatContext);

        // decoder
        AVCodecContext codecContext = formatContext.streams[0].codecpar;
        codecContext.priv_data = new TwinVQContext();
        TwinVQDec.twinvq_decode_init(codecContext);

        // Audio output setup
        int sampleRate = codecContext.sample_rate;
        int channels = codecContext.ch_layout.nb_channels;
        AudioFormat audioFormat = new AudioFormat(
                Encoding.PCM_SIGNED,
                sampleRate,
                16,
                channels,
                channels * 2,
                sampleRate,
                false);
System.err.println("Audio format: " + audioFormat);

        Info info = new Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();
        volume(line, volume);

        AVFrame frame = new AVFrame();
        int[] got_frame_ptr = new int[1];
        int frameCount = 0;
        int maxFrames = 10000; // Limit for testing
        while (frameCount < maxFrames) {
            AVPacket packet = inputFormat.read_packet.apply(formatContext);
            if (packet == null) break; // End of file
            int r = TwinVQ.ff_twinvq_decode_frame(codecContext, frame, got_frame_ptr, packet);
            if (r < 0) break; // Decode error

            if (got_frame_ptr[0] != 0 && frame.extended_data != null) {
                float[][] audioData = (float[][]) frame.extended_data;
                int samples = frame.nb_samples;

                // Debug: print first few samples of first frame
                if (frameCount < 3) {
                    System.err.printf("Frame %d: samples=%d, ch0[0..3]=[%.6f, %.6f, %.6f, %.6f]%n",
                            frameCount, samples,
                            audioData[0][0], audioData[0][1], audioData[0][2], audioData[0][3]);
                }

                // Convert float to 16-bit PCM and play
                byte[] pcmData = new byte[samples * channels * 2];
                for (int i = 0; i < samples; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        float sample = audioData[ch][i];
                        // Clamp to [-1.0, 1.0] and convert to 16-bit
                        sample = Math.max(-1.0f, Math.min(1.0f, sample));
                        short s = (short) (sample * 32767);
                        int idx = (i * channels + ch) * 2;
                        pcmData[idx] = (byte) (s & 0xff);
                        pcmData[idx + 1] = (byte) ((s >> 8) & 0xff);
                    }
                }
                line.write(pcmData, 0, pcmData.length);
                frameCount++;
            }
        }

        line.drain();
        line.close();

        TwinVQ.ff_twinvq_decode_close(codecContext);
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void testCompareWithFFmpeg() throws Exception {
        String ffmpegRef = "tmp/twinvq.ffmpeg.wav";
        String javaOut = "tmp/twinvq.java.raw";

        // Decode with Java implementation and save to file
        AVInputFormat inputFormat = VFQ.ff_vqf_demuxer;
        DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(Path.of(twinvq))));
        dis.mark(12);
        byte[] probe = new byte[12];
        dis.readFully(probe);
        dis.reset();
        if (inputFormat.read_probe.apply(probe) == 0) {
            throw new IllegalArgumentException("not vfq");
        }

        AVFormatContext formatContext = new AVFormatContext();
        formatContext.pb = dis;
        inputFormat.read_header.apply(formatContext);

        AVCodecContext codecContext = formatContext.streams[0].codecpar;
        codecContext.priv_data = new TwinVQContext();
        TwinVQDec.twinvq_decode_init(codecContext);

        int sampleRate = codecContext.sample_rate;
        int channels = codecContext.ch_layout.nb_channels;
        System.err.println("Sample rate: " + sampleRate + ", channels: " + channels);

        // Collect decoded samples
        java.util.List<short[]> allSamples = new java.util.ArrayList<>();
        java.util.List<float[][]> allFloatSamples = new java.util.ArrayList<>();  // Keep float values for debugging
        AVFrame frame = new AVFrame();
        int[] got_frame_ptr = new int[1];
        int maxFrames = 100;
        int frameCount = 0;

        while (frameCount < maxFrames) {
            AVPacket packet = inputFormat.read_packet.apply(formatContext);
            if (packet == null) break;
            int r = TwinVQ.ff_twinvq_decode_frame(codecContext, frame, got_frame_ptr, packet);
            if (r < 0) break;

            if (got_frame_ptr[0] != 0 && frame.extended_data != null) {
                float[][] audioData = (float[][]) frame.extended_data;
                int samples = frame.nb_samples;

                // Keep a copy of the float data for debugging
                float[][] floatCopy = new float[channels][samples];
                for (int ch = 0; ch < channels; ch++) {
                    System.arraycopy(audioData[ch], 0, floatCopy[ch], 0, samples);
                }
                allFloatSamples.add(floatCopy);

                // Debug: print float stats for all frames
                float maxCh0 = 0, maxCh1 = 0;
                for (int i = 0; i < samples; i++) {
                    if (Math.abs(audioData[0][i]) > Math.abs(maxCh0)) maxCh0 = audioData[0][i];
                    if (channels > 1 && Math.abs(audioData[1][i]) > Math.abs(maxCh1)) maxCh1 = audioData[1][i];
                }
                if (frameCount < 15 || Math.abs(maxCh0) > 0.5 || Math.abs(maxCh1) > 0.5) {
                    System.err.printf("Frame %d: samples=%d, ch0 max=%.6f, ch1 max=%.6f%n", frameCount, samples, maxCh0, maxCh1);
                }

                short[] pcmSamples = new short[samples * channels];
                for (int i = 0; i < samples; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        float sample = audioData[ch][i];
                        sample = Math.max(-1.0f, Math.min(1.0f, sample));
                        pcmSamples[i * channels + ch] = (short) (sample * 32767);
                    }
                }
                allSamples.add(pcmSamples);
                frameCount++;
            }
        }
        TwinVQ.ff_twinvq_decode_close(codecContext);
        dis.close();

        // Save Java output to file
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(Path.of(javaOut))))) {
            for (short[] samples : allSamples) {
                for (short s : samples) {
                    dos.writeByte(s & 0xff);
                    dos.writeByte((s >> 8) & 0xff);
                }
            }
        }
        System.err.println("Java output saved to: " + javaOut + ", frames: " + frameCount);

        // Load FFmpeg reference (WAV file - skip header)
        AudioInputStream ais = AudioSystem.getAudioInputStream(Path.of(ffmpegRef).toFile());
        AudioFormat fmt = ais.getFormat();
        System.err.println("FFmpeg format: " + fmt);
        byte[] ffmpegBytes = ais.readAllBytes();
        ais.close();

        // Convert FFmpeg bytes to shorts
        ShortBuffer ffmpegShorts = ByteBuffer.wrap(ffmpegBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] ffmpegSamples = new short[ffmpegShorts.remaining()];
        ffmpegShorts.get(ffmpegSamples);

        // Convert Java output to flat array
        int totalJavaSamples = allSamples.stream().mapToInt(a -> a.length).sum();
        short[] javaSamples = new short[totalJavaSamples];
        int pos = 0;
        for (short[] samples : allSamples) {
            System.arraycopy(samples, 0, javaSamples, pos, samples.length);
            pos += samples.length;
        }

        // Compare ALL available samples
        int compareCount = Math.min(javaSamples.length, ffmpegSamples.length);
        System.err.println("Comparing " + compareCount + " samples (Java: " + javaSamples.length + ", FFmpeg: " + ffmpegSamples.length + ")");

        // Calculate statistics
        double sumDiff = 0, sumDiffSq = 0, maxDiff = 0;
        int maxDiffIdx = 0;
        int javaMaxAbs = 0, javaMaxAbsIdx = 0;
        int ffmpegMaxAbs = 0, ffmpegMaxAbsIdx = 0;
        int exactMatches = 0;
        int closeMatches = 0; // within 10
        int signMismatches = 0;

        for (int i = 0; i < compareCount; i++) {
            double diff = javaSamples[i] - ffmpegSamples[i];
            sumDiff += Math.abs(diff);
            sumDiffSq += diff * diff;
            if (Math.abs(diff) > maxDiff) {
                maxDiff = Math.abs(diff);
                maxDiffIdx = i;
            }
            if (Math.abs(javaSamples[i]) > javaMaxAbs) {
                javaMaxAbs = Math.abs(javaSamples[i]);
                javaMaxAbsIdx = i;
            }
            if (Math.abs(ffmpegSamples[i]) > ffmpegMaxAbs) {
                ffmpegMaxAbs = Math.abs(ffmpegSamples[i]);
                ffmpegMaxAbsIdx = i;
            }
            if (javaSamples[i] == ffmpegSamples[i]) exactMatches++;
            if (Math.abs(diff) <= 10) closeMatches++;
            if (javaSamples[i] != 0 && ffmpegSamples[i] != 0 &&
                (javaSamples[i] > 0) != (ffmpegSamples[i] > 0)) signMismatches++;
        }

        double meanAbsDiff = sumDiff / compareCount;
        double rmsDiff = Math.sqrt(sumDiffSq / compareCount);

        System.err.println("=== COMPARISON STATISTICS ===");
        System.err.println("Mean absolute difference: " + meanAbsDiff);
        System.err.println("RMS difference: " + rmsDiff);
        System.err.println("Max difference: " + maxDiff + " at sample " + maxDiffIdx);
        System.err.println("Java max amplitude: " + javaMaxAbs + " at sample " + javaMaxAbsIdx);
        System.err.println("FFmpeg max amplitude: " + ffmpegMaxAbs + " at sample " + ffmpegMaxAbsIdx);
        System.err.println("Exact matches: " + exactMatches + " / " + compareCount + " (" + (100.0 * exactMatches / compareCount) + "%)");
        System.err.println("Close matches (within 10): " + closeMatches + " / " + compareCount + " (" + (100.0 * closeMatches / compareCount) + "%)");
        System.err.println("Sign mismatches: " + signMismatches + " (" + (100.0 * signMismatches / compareCount) + "%)");

        // Correlation coefficient
        double javaSum = 0, ffmpegSum = 0;
        for (int i = 0; i < compareCount; i++) {
            javaSum += javaSamples[i];
            ffmpegSum += ffmpegSamples[i];
        }
        double javaMean = javaSum / compareCount;
        double ffmpegMean = ffmpegSum / compareCount;
        double covariance = 0, javaVar = 0, ffmpegVar = 0;
        for (int i = 0; i < compareCount; i++) {
            double jd = javaSamples[i] - javaMean;
            double fd = ffmpegSamples[i] - ffmpegMean;
            covariance += jd * fd;
            javaVar += jd * jd;
            ffmpegVar += fd * fd;
        }
        double correlation = covariance / Math.sqrt(javaVar * ffmpegVar);
        System.err.println("Correlation coefficient: " + correlation);

        // Check for channel swap or inversion
        double corrSwapped = 0, corrInverted = 0;
        double swapCov = 0, invCov = 0;
        for (int i = 0; i < compareCount - 1; i += 2) {
            // Check if L/R channels are swapped
            double jdL = javaSamples[i] - javaMean;
            double jdR = javaSamples[i + 1] - javaMean;
            double fdL = ffmpegSamples[i] - ffmpegMean;
            double fdR = ffmpegSamples[i + 1] - ffmpegMean;
            swapCov += jdL * fdR + jdR * fdL;
            // Check if signal is inverted
            invCov += (-javaSamples[i] - javaMean) * fdL + (-javaSamples[i + 1] - javaMean) * fdR;
        }
        corrSwapped = swapCov / Math.sqrt(javaVar * ffmpegVar);
        corrInverted = invCov / Math.sqrt(javaVar * ffmpegVar);
        System.err.println("Correlation if channels swapped: " + corrSwapped);
        System.err.println("Correlation if signal inverted: " + corrInverted);

        // Print first 40 samples comparison (20 stereo pairs)
        System.err.println("\n=== First 40 samples (20 stereo pairs) ===");
        System.err.println("Index\tJava L\tJava R\tFFmpeg L\tFFmpeg R\tDiff L\tDiff R");
        for (int i = 0; i < Math.min(40, compareCount); i += 2) {
            System.err.printf("%d\t%d\t%d\t%d\t%d\t%d\t%d%n",
                i/2, javaSamples[i], javaSamples[i+1],
                ffmpegSamples[i], ffmpegSamples[i+1],
                javaSamples[i] - ffmpegSamples[i], javaSamples[i+1] - ffmpegSamples[i+1]);
        }

        // Find first non-silent region and compare
        int nonSilentStart = -1;
        for (int i = 0; i < compareCount; i++) {
            if (Math.abs(ffmpegSamples[i]) > 1000) {
                nonSilentStart = i;
                break;
            }
        }
        if (nonSilentStart >= 0) {
            System.err.println("\n=== First non-silent region (FFmpeg > 1000, starting at " + nonSilentStart + ") ===");
            System.err.println("Index\tJava\tFFmpeg\tDiff\tRatio");
            for (int i = nonSilentStart; i < Math.min(nonSilentStart + 40, compareCount); i++) {
                double ratio = ffmpegSamples[i] != 0 ? (double) javaSamples[i] / ffmpegSamples[i] : 0;
                System.err.printf("%d\t%d\t%d\t%d\t%.3f%n", i, javaSamples[i], ffmpegSamples[i],
                    javaSamples[i] - ffmpegSamples[i], ratio);
            }

            // Check what float values we have at these positions
            int samplesPerFrame = 2048;
            int channelCount = 2;
            int samplesPerFrameTotal = samplesPerFrame * channelCount;
            int frameIdx = nonSilentStart / samplesPerFrameTotal;
            int posInFrame = nonSilentStart % samplesPerFrameTotal;
            System.err.println("\n=== Float values at non-silent region ===");
            System.err.println("Sample " + nonSilentStart + " is in frame " + frameIdx + ", position " + posInFrame);
            if (frameIdx < allFloatSamples.size()) {
                float[][] floatFrame = allFloatSamples.get(frameIdx);
                // Find where the non-zero values are in this frame
                int firstNonZero = -1, lastNonZero = -1;
                float maxVal = 0;
                int maxIdx = 0;
                for (int i = 0; i < floatFrame[0].length; i++) {
                    float v = Math.max(Math.abs(floatFrame[0][i]), Math.abs(floatFrame[1][i]));
                    if (v > 0.001) {
                        if (firstNonZero < 0) firstNonZero = i;
                        lastNonZero = i;
                    }
                    if (v > maxVal) {
                        maxVal = v;
                        maxIdx = i;
                    }
                }
                System.err.println("Frame " + frameIdx + " non-zero range: " + firstNonZero + " to " + lastNonZero + ", max=" + maxVal + " at " + maxIdx);

                // Show samples around the max value
                System.err.println("Frame " + frameIdx + " samples around max (index " + maxIdx + "):");
                for (int i = Math.max(0, maxIdx - 10); i < Math.min(floatFrame[0].length, maxIdx + 10); i++) {
                    System.err.printf("  [%d] ch0=%.6f, ch1=%.6f%n", i, floatFrame[0][i], floatFrame[1][i]);
                }

                // Also check frame 8 which had larger values
                if (frameIdx + 1 < allFloatSamples.size()) {
                    float[][] nextFrame = allFloatSamples.get(frameIdx + 1);
                    float maxNext = 0;
                    int maxNextIdx = 0;
                    for (int i = 0; i < nextFrame[0].length; i++) {
                        float v = Math.max(Math.abs(nextFrame[0][i]), Math.abs(nextFrame[1][i]));
                        if (v > maxNext) {
                            maxNext = v;
                            maxNextIdx = i;
                        }
                    }
                    System.err.println("Frame " + (frameIdx+1) + " max=" + maxNext + " at " + maxNextIdx);
                    System.err.println("Frame " + (frameIdx+1) + " samples around max:");
                    for (int i = Math.max(0, maxNextIdx - 5); i < Math.min(nextFrame[0].length, maxNextIdx + 5); i++) {
                        System.err.printf("  [%d] ch0=%.6f, ch1=%.6f%n", i, nextFrame[0][i], nextFrame[1][i]);
                    }
                }
            }
        }

        // Print samples around max diff
        System.err.println("\n=== Samples around max diff (index " + maxDiffIdx + ") ===");
        System.err.println("Index\tJava\tFFmpeg\tDiff");
        for (int i = Math.max(0, maxDiffIdx - 10); i < Math.min(compareCount, maxDiffIdx + 10); i++) {
            System.err.printf("%d\t%d\t%d\t%d%n", i, javaSamples[i], ffmpegSamples[i], javaSamples[i] - ffmpegSamples[i]);
        }
    }
}