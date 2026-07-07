/*
 * WMA v1/v2 decoder. ASF container demuxer.
 *
 * Ported from the pure-JS ASF demuxer of the @audio/wma-decode npm package
 * (https://www.npmjs.com/package/@audio/wma-decode), functions demuxASF /
 * parsePacket / parseStreamProps / parseFileProps.
 */

package vavi.sound.wma;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Parses an ASF ("Advanced Systems Format", the {@code .wma} container) and
 * extracts the audio stream's {@code WAVEFORMATEX} properties plus the raw data
 * packets. The compressed WMA payloads are then de-packetised with
 * {@link #parsePacket}.
 * <p>
 * Direct port of the JavaScript ASF demuxer in {@code @audio/wma-decode}.
 *
 * @see "https://www.npmjs.com/package/@audio/wma-decode"
 */
public final class AsfDemuxer {

    private AsfDemuxer() {}

    // ASF object GUIDs (16 bytes each, stored little-endian).
    private static final int[] GUID_HEADER = {
            0x30, 0x26, 0xb2, 0x75, 0x8e, 0x66, 0xcf, 0x11, 0xa6, 0xd9, 0x00, 0xaa, 0x00, 0x62, 0xce, 0x6c};
    private static final int[] GUID_FILE_PROPS = {
            0xa1, 0xdc, 0xab, 0x8c, 0x47, 0xa9, 0xcf, 0x11, 0x8e, 0xe4, 0x00, 0xc0, 0x0c, 0x20, 0x53, 0x65};
    private static final int[] GUID_STREAM_PROPS = {
            0x91, 0x07, 0xdc, 0xb7, 0xb7, 0xa9, 0xcf, 0x11, 0x8e, 0xe6, 0x00, 0xc0, 0x0c, 0x20, 0x53, 0x65};
    private static final int[] GUID_AUDIO_MEDIA = {
            0x40, 0x9e, 0x69, 0xf8, 0x4d, 0x5b, 0xcf, 0x11, 0xa8, 0xfd, 0x00, 0x80, 0x5f, 0x5c, 0x44, 0x2b};
    private static final int[] GUID_DATA = {
            0x36, 0x26, 0xb2, 0x75, 0x8e, 0x66, 0xcf, 0x11, 0xa6, 0xd9, 0x00, 0xaa, 0x00, 0x62, 0xce, 0x6c};

    /** Returns true if the buffer begins with the ASF header object GUID. */
    public static boolean isAsf(byte[] buf) {
        return buf != null && buf.length >= 16 && guidEq(buf, 0, GUID_HEADER);
    }

    private static boolean guidEq(byte[] buf, int off, int[] guid) {
        for (int i = 0; i < 16; i++) {
            if ((buf[off + i] & 0xFF) != guid[i]) {
                return false;
            }
        }
        return true;
    }

    private static int u16(byte[] b, int o) {
        return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8);
    }

    private static long u32(byte[] b, int o) {
        return ((b[o] & 0xFFL)) | ((b[o + 1] & 0xFFL) << 8)
                | ((b[o + 2] & 0xFFL) << 16) | ((b[o + 3] & 0xFFL) << 24);
    }

    private static long u64(byte[] b, int o) {
        return u32(b, o) | (u32(b, o + 4) << 32);
    }

    /**
     * Parses the ASF container and returns the audio stream properties and data
     * packets.
     *
     * @throws IllegalArgumentException if the buffer is not a valid ASF/WMA file
     */
    public static AsfInfo demux(byte[] buf) {
        if (buf == null || buf.length < 30 || !guidEq(buf, 0, GUID_HEADER)) {
            throw new IllegalArgumentException("Not an ASF/WMA file");
        }

        long headerSize = u64(buf, 16);
        long numObjects = u32(buf, 24);

        WaveFormat audio = null;
        int packetSize = 0;
        long datSize = 0;
        int datOff = 0;
        long totalPackets = 0;

        int pos = 30;
        long headerEnd = Math.min(headerSize, buf.length);
        for (int i = 0; i < numObjects && pos < headerEnd - 24; i++) {
            long objSize = u64(buf, pos + 16);
            if (objSize < 24) {
                break;
            }
            int objEnd = (int) (pos + Math.min(objSize, headerEnd - pos));

            if (guidEq(buf, pos, GUID_STREAM_PROPS)) {
                WaveFormat a = parseStreamProps(buf, pos + 24, objEnd);
                if (a != null) {
                    audio = a;
                }
            } else if (guidEq(buf, pos, GUID_FILE_PROPS)) {
                int ps = parseFileProps(buf, pos + 24, objEnd);
                if (ps >= 0) {
                    packetSize = ps;
                }
            }
            pos = objEnd;
        }

        if (audio == null) {
            throw new IllegalArgumentException("No audio stream in ASF");
        }

        // Data Object after header.
        pos = (int) headerSize;
        if (pos + 50 <= buf.length && guidEq(buf, pos, GUID_DATA)) {
            datSize = u64(buf, pos + 16);
            totalPackets = u64(buf, pos + 24 + 16);
            datOff = pos + 24 + 26;
        }

        List<byte[]> packets = new ArrayList<>();
        if (datOff != 0 && packetSize > 0) {
            long datEnd = Math.min(pos + datSize, buf.length);
            int ppos = datOff;
            while (ppos + packetSize <= datEnd) {
                packets.add(Arrays.copyOfRange(buf, ppos, ppos + packetSize));
                ppos += packetSize;
            }
        } else if (datOff != 0 && totalPackets > 0) {
            long datEnd = Math.min(pos + datSize, buf.length);
            if (datOff < datEnd) {
                packets.add(Arrays.copyOfRange(buf, datOff, (int) datEnd));
            }
        }

        return new AsfInfo(audio.formatTag, audio.channels, audio.sampleRate,
                (int) (audio.avgBytesPerSec * 8L), audio.blockAlign, audio.bitsPerSample,
                audio.codecData, packetSize, packets);
    }

    private static WaveFormat parseStreamProps(byte[] buf, int start, int end) {
        if (start + 54 > end) {
            return null;
        }
        if (!guidEq(buf, start, GUID_AUDIO_MEDIA)) {
            return null;
        }
        int waveOff = start + 54;
        if (waveOff + 18 > end) {
            return null;
        }
        WaveFormat w = new WaveFormat();
        w.formatTag = u16(buf, waveOff);
        w.channels = u16(buf, waveOff + 2);
        w.sampleRate = (int) u32(buf, waveOff + 4);
        w.avgBytesPerSec = (int) u32(buf, waveOff + 8);
        w.blockAlign = u16(buf, waveOff + 12);
        w.bitsPerSample = u16(buf, waveOff + 14);
        int cbSize = u16(buf, waveOff + 16);
        if (cbSize > 0 && waveOff + 18 + cbSize <= end) {
            w.codecData = Arrays.copyOfRange(buf, waveOff + 18, waveOff + 18 + cbSize);
        } else {
            w.codecData = new byte[0];
        }
        return w;
    }

    /** @return packet size, or -1 if the object is too short. */
    private static int parseFileProps(byte[] buf, int start, int end) {
        if (start + 80 > end) {
            return -1;
        }
        // Max packet size at offset 72 (min at 68, always equal for WMA).
        return (int) u32(buf, start + 72);
    }

    /**
     * Splits an ASF data packet into its compressed audio payload(s). Direct
     * port of the JS {@code parsePacket}.
     *
     * @param pkt the raw packet bytes
     * @return the payload byte arrays contained in the packet
     */
    public static List<byte[]> parsePacket(byte[] pkt) {
        List<byte[]> payloads = new ArrayList<>();
        if (pkt == null || pkt.length < 3) {
            return payloads;
        }
        int pos = 0;

        int ecFlags = pkt[pos++] & 0xFF;
        if ((ecFlags & 0x80) != 0) {
            int ecLen = ecFlags & 0x0F;
            pos += ecLen;
        }
        if (pos >= pkt.length) {
            return payloads;
        }

        int ppFlags = pkt[pos++] & 0xFF;
        int lenFlags = pkt[pos++] & 0xFF;

        boolean multiplePayloads = (ppFlags & 0x01) != 0;
        int seqType = (ppFlags >> 1) & 0x03;
        int padType = (ppFlags >> 3) & 0x03;
        int pktLenType = (ppFlags >> 5) & 0x03;

        int repType = lenFlags & 0x03;
        int offType = (lenFlags >> 2) & 0x03;
        int medNumType = (lenFlags >> 4) & 0x03;

        pos += fieldSize(pktLenType);
        pos += fieldSize(seqType);

        int padLen = 0;
        if (padType == 1) {
            padLen = pkt[pos++] & 0xFF;
        } else if (padType == 2) {
            padLen = u16(pkt, pos);
            pos += 2;
        } else if (padType == 3) {
            padLen = (int) u32(pkt, pos);
            pos += 4;
        }

        // Send time (4 bytes) + duration (2 bytes).
        pos += 6;
        if (pos >= pkt.length) {
            return payloads;
        }

        if (!multiplePayloads) {
            pos += 1; // stream number
            pos += fieldSize(medNumType);
            pos += fieldSize(offType);
            int repLen = readVarField(pkt, pos, repType);
            pos += fieldSize(repType);
            if (repLen == 1) {
                pos += 1; // compressed: presentation time delta
            } else {
                pos += repLen;
            }
            int payloadLen = pkt.length - pos - padLen;
            if (payloadLen > 0 && pos + payloadLen <= pkt.length) {
                payloads.add(Arrays.copyOfRange(pkt, pos, pos + payloadLen));
            }
        } else {
            int payloadFlags = pkt[pos++] & 0xFF;
            int numPayloads = payloadFlags & 0x3F;
            int payLenType = (payloadFlags >> 6) & 0x03;
            for (int i = 0; i < numPayloads && pos < pkt.length; i++) {
                pos += 1; // stream number
                pos += fieldSize(medNumType);
                pos += fieldSize(offType);
                int repLen = readVarField(pkt, pos, repType);
                pos += fieldSize(repType);
                if (repLen == 1) {
                    pos += 1;
                } else {
                    pos += repLen;
                }
                int payLen;
                if (payLenType == 1) {
                    payLen = pkt[pos++] & 0xFF;
                } else if (payLenType == 2) {
                    payLen = u16(pkt, pos);
                    pos += 2;
                } else if (payLenType == 3) {
                    payLen = (int) u32(pkt, pos);
                    pos += 4;
                } else {
                    payLen = pkt.length - pos;
                }
                if (payLen > 0 && pos + payLen <= pkt.length) {
                    payloads.add(Arrays.copyOfRange(pkt, pos, pos + payLen));
                }
                pos += payLen;
            }
        }
        return payloads;
    }

    private static int fieldSize(int type) {
        return type == 0 ? 0 : type == 1 ? 1 : type == 2 ? 2 : 4;
    }

    private static int readVarField(byte[] buf, int off, int type) {
        if (type == 0) {
            return 0;
        }
        if (type == 1) {
            return off < buf.length ? (buf[off] & 0xFF) : 0;
        }
        if (type == 2) {
            return (off + 2 <= buf.length) ? u16(buf, off) : 0;
        }
        return (off + 4 <= buf.length) ? (int) u32(buf, off) : 0;
    }

    private static final class WaveFormat {
        int formatTag;
        int channels;
        int sampleRate;
        int avgBytesPerSec;
        int blockAlign;
        int bitsPerSample;
        byte[] codecData;
    }
}
