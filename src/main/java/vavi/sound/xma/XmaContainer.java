/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;

import java.io.IOException;


/**
 * Parses an XMA container (RIFF/WAVE with an XMA1/XMA2 {@code fmt } chunk) or a
 * raw 2 KiB packet stream, and hands out 2048-byte packets.
 * <p>
 * Direct translation of {@code Echo/Container/XmaContainer.cs}. The C# original
 * works on a seekable {@code Stream}; this port operates on the whole file held
 * in a {@code byte[]}.
 *
 * @see "https://github.com/IsaacMarovitz/Echo"
 */
public final class XmaContainer {

    private static final int WAVE_FORMAT_XMA1 = 0x0165;
    private static final int WAVE_FORMAT_XMA2 = 0x0166;

    /** One XMA packet is 2048 bytes. */
    public static final int PACKET_SIZE_BYTES = 2048;

    private final byte[] data;
    private final int dataEnd;
    private int pos;

    private final XmaStreamInfo streamInfo;

    private XmaContainer(byte[] data, int dataStart, int dataEnd, XmaStreamInfo info) {
        this.data = data;
        this.pos = dataStart;
        this.dataEnd = dataEnd;
        this.streamInfo = info;
    }

    public XmaStreamInfo streamInfo() {
        return streamInfo;
    }

    public static XmaContainer open(byte[] data) throws IOException {
        return openInternal(data);
    }

    public static XmaContainer openRaw(byte[] data, XmaStreamInfo info) {
        return new XmaContainer(data, 0, data.length, info);
    }

    private static XmaContainer openInternal(byte[] data) throws IOException {
        if (data.length < 12) {
            throw new IOException("File too short to contain a header.");
        }

        boolean isRiff =
                data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F' &&
                data[8] == 'W' && data[9] == 'A' && data[10] == 'V' && data[11] == 'E';

        if (!isRiff) {
            throw new IOException(
                    "Not a RIFF/WAVE .xma file. Use openRaw() for headerless frame streams.");
        }

        return parseRiff(data);
    }

    /**
     * Parses just the RIFF header and {@code fmt } chunk to obtain the stream
     * info, without requiring the {@code data} chunk. Intended for SPI probing
     * where only a prefix of the file is available.
     *
     * @param head a prefix of the file (must include the RIFF header and the
     *             {@code fmt } chunk)
     * @return the parsed stream info
     * @throws IOException if the prefix is not a recognisable XMA header
     */
    public static XmaStreamInfo probeInfo(byte[] head) throws IOException {
        if (head.length < 12) {
            throw new IOException("Too short to contain a header.");
        }
        boolean isRiff =
                head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F' &&
                head[8] == 'W' && head[9] == 'A' && head[10] == 'V' && head[11] == 'E';
        if (!isRiff) {
            throw new IOException("Not a RIFF/WAVE .xma file.");
        }

        int p = 12;
        while (p + 8 <= head.length) {
            long id = readU32LE(head, p);
            long size = readU32LE(head, p + 4);
            int chunkBody = p + 8;
            if (id == 0x20746D66L) { // 'fmt '
                int avail = Math.min((int) size, head.length - chunkBody);
                return parseFmtChunk(head, chunkBody, avail);
            }
            long nextChunk = chunkBody + size + (size & 1);
            if (nextChunk <= p || nextChunk > head.length) {
                break;
            }
            p = (int) nextChunk;
        }
        throw new IOException("No 'fmt ' chunk found in the probed prefix.");
    }

    private static XmaContainer parseRiff(byte[] data) throws IOException {
        int fileEnd = data.length;
        XmaStreamInfo info = null;
        int dataStart = -1;
        int dataEnd = -1;

        int p = 12;
        while (p + 8 <= fileEnd) {
            long id = readU32LE(data, p);
            long size = readU32LE(data, p + 4);
            int chunkBody = p + 8;
            long nextChunk = chunkBody + size + (size & 1); // chunks pad to even

            switch ((int) id) {
                case 0x20746D66 -> // 'fmt '
                        info = parseFmtChunk(data, chunkBody, (int) size);
                case 0x61746164 -> { // 'data'
                    dataStart = chunkBody;
                    dataEnd = (int) (chunkBody + size);
                }
                default -> {
                }
            }

            p = (int) nextChunk;
        }

        if (info == null) {
            throw new IOException("RIFF .xma is missing a 'fmt ' chunk.");
        }
        if (dataStart < 0) {
            throw new IOException("RIFF .xma is missing a 'data' chunk.");
        }
        if ((dataEnd - dataStart) % PACKET_SIZE_BYTES != 0) {
            throw new IOException(
                    "data chunk size is not a multiple of 2048 bytes (one XMA packet).");
        }

        return new XmaContainer(data, dataStart, dataEnd, info);
    }

    private static XmaStreamInfo parseFmtChunk(byte[] data, int off, int size) throws IOException {
        if (size < 18) {
            throw new IOException("fmt chunk too small for WAVEFORMATEX.");
        }
        if (off + size > data.length) {
            throw new IOException("Truncated fmt chunk.");
        }

        byte[] buf = new byte[size];
        System.arraycopy(data, off, buf, 0, size);

        int formatTag = readU16LE(buf, 0);

        return switch (formatTag) {
            case WAVE_FORMAT_XMA1 -> parseXma1Fmt(buf);
            case WAVE_FORMAT_XMA2 -> {
                int channels = readU16LE(buf, 2);
                int sampleRate = (int) readU32LE(buf, 4);
                yield parseXma2Fmt(buf, channels, sampleRate);
            }
            default -> throw new IOException(String.format(
                    "Unsupported wFormatTag 0x%04X; expected XMA1 (0x0165) or XMA2 (0x0166).",
                    formatTag));
        };
    }

    private static XmaStreamInfo parseXma2Fmt(byte[] buf, int channels, int sampleRate) {
        // XMA2WAVEFORMATEX layout (Microsoft, XAudio2 / XACT 3):
        //   offset 18 : wNumStreams (2)
        int numStreams = 1;
        if (buf.length >= 20) {
            numStreams = readU16LE(buf, 18);
        }

        // Defensive clamp: NumStreams must be 1..ceil(channels/2). Some encoders
        // write 0 when they really mean 1.
        if (numStreams == 0) {
            numStreams = 1;
        }
        int maxStreams = (channels + 1) / 2;
        if (numStreams > maxStreams) {
            numStreams = maxStreams;
        }

        XmaStreamInfo info = new XmaStreamInfo();
        info.version = XmaVersion.Xma2;
        info.sampleRate = sampleRate;
        info.channels = channels;
        info.numStreams = numStreams;
        return info;
    }

    private static XmaStreamInfo parseXma1Fmt(byte[] buf) throws IOException {
        // Observed XMA1 fmt-chunk layout in Xbox 360 game files:
        //   0x08 nb_streams (2)
        //   0x10 + 16*N per-stream descriptors:
        //       +0x00 SampleRate (4), +0x0D Channels (1)
        final int streamDescSize = 16;
        final int streamArrayBase = 0x10;
        final int minSize = streamArrayBase + streamDescSize;

        if (buf.length < minSize) {
            throw new IOException(String.format(
                    "XMA1 fmt chunk too small (%d bytes); expected at least %d.",
                    buf.length, minSize));
        }

        int streamCount = readU16LE(buf, 0x08);
        if (streamCount == 0) {
            throw new IOException("XMA1 fmt chunk reports zero streams.");
        }

        int needed = streamArrayBase + streamCount * streamDescSize;
        if (buf.length < needed) {
            throw new IOException(String.format(
                    "XMA1 fmt chunk truncated: need %d bytes for %d stream(s), have %d.",
                    needed, streamCount, buf.length));
        }

        int totalChannels = 0;
        int sampleRate = 0;

        for (int s = 0; s < streamCount; s++) {
            int off = streamArrayBase + s * streamDescSize;
            long streamRate = readU32LE(buf, off);
            int chans = buf[off + 0x0D] & 0xFF;

            if (s == 0) {
                sampleRate = (int) streamRate;
            }
            totalChannels += chans;
        }

        XmaStreamInfo info = new XmaStreamInfo();
        info.version = XmaVersion.Xma1;
        info.sampleRate = sampleRate;
        info.channels = totalChannels;
        info.numStreams = streamCount;
        return info;
    }

    /**
     * Reads the next 2048-byte packet into the supplied buffer. Returns false
     * when the data chunk is exhausted.
     */
    public boolean readPacket(byte[] destination) {
        if (destination.length < PACKET_SIZE_BYTES) {
            throw new IllegalArgumentException("Destination must be at least 2048 bytes.");
        }
        if (pos >= dataEnd) {
            return false;
        }
        if (pos + PACKET_SIZE_BYTES > dataEnd) {
            return false;
        }
        System.arraycopy(data, pos, destination, 0, PACKET_SIZE_BYTES);
        pos += PACKET_SIZE_BYTES;
        return true;
    }

    private static int readU16LE(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    private static long readU32LE(byte[] b, int off) {
        return (b[off] & 0xFFL)
                | ((b[off + 1] & 0xFFL) << 8)
                | ((b[off + 2] & 0xFFL) << 16)
                | ((b[off + 3] & 0xFFL) << 24);
    }
}
