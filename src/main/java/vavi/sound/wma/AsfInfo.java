/*
 * WMA v1/v2 decoder. ASF stream properties.
 */

package vavi.sound.wma;

import java.util.List;


/**
 * Audio stream properties and raw data packets extracted from an ASF/WMA
 * container by {@link AsfDemuxer}. Mirrors the object returned by the
 * {@code demuxASF} function of the {@code @audio/wma-decode} npm package.
 */
public final class AsfInfo {

    /** WMA format tag: 0x0160 (v1), 0x0161 (v2), 0x0162 (Pro), 0x0163 (Lossless). */
    public final int formatTag;
    public final int channels;
    public final int sampleRate;
    /** Average bit rate in bits/second. */
    public final int bitRate;
    /** WMA block/frame size in bytes (WAVEFORMATEX nBlockAlign). */
    public final int blockAlign;
    public final int bitsPerSample;
    /** Codec-specific extra bytes (WAVEFORMATEX cbSize bytes), may be empty. */
    public final byte[] codecData;
    /** ASF data-packet size in bytes. */
    public final int packetSize;
    /** Raw ASF data packets (each {@code packetSize} bytes). */
    public final List<byte[]> packets;

    AsfInfo(int formatTag, int channels, int sampleRate, int bitRate, int blockAlign,
            int bitsPerSample, byte[] codecData, int packetSize, List<byte[]> packets) {
        this.formatTag = formatTag;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.bitRate = bitRate;
        this.blockAlign = blockAlign;
        this.bitsPerSample = bitsPerSample;
        this.codecData = codecData;
        this.packetSize = packetSize;
        this.packets = packets;
    }
}
