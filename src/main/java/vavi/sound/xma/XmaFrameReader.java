/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;


/**
 * De-packetises an XMA stream into per-substream frames.
 * <p>
 * Direct translation of {@code Echo/Container/XmaFrameReader.cs}.
 *
 * @see "https://github.com/IsaacMarovitz/Echo"
 */
public final class XmaFrameReader {

    private static final int FRAME_SIZE_BITS = 15;
    private static final int END_OF_STREAM_MARKER = 0x7FFF;
    private static final int PAYLOAD_BYTES_PER_PACKET = XmaContainer.PACKET_SIZE_BYTES - 4;

    private static final class StreamState {
        byte[] bits = new byte[PAYLOAD_BYTES_PER_PACKET * 4];
        int fill;
        int readBitPos;
        final Deque<XmaFrame> ready = new ArrayDeque<>();
    }

    private final XmaContainer container;
    private final int numStreams;
    private final StreamState[] streams;
    private final byte[] packetBuf = new byte[XmaContainer.PACKET_SIZE_BYTES];

    private int nextStream;
    private boolean sourceExhausted;

    public XmaFrameReader(XmaContainer container) {
        this.container = container;
        this.numStreams = container.streamInfo().numStreams;
        this.streams = new StreamState[numStreams];
        for (int s = 0; s < numStreams; s++) {
            streams[s] = new StreamState();
        }
    }

    public int numStreams() {
        return numStreams;
    }

    /**
     * Pull one frame from each stream. Returns false when any stream is
     * exhausted (the file ended before producing the next aligned frame for
     * every stream — happens at EOF).
     */
    public boolean readFrameSet(XmaFrame[] frames) {
        if (frames.length != numStreams) {
            throw new IllegalArgumentException("frames length must equal numStreams.");
        }

        while (true) {
            boolean allReady = true;
            for (int s = 0; s < numStreams; s++) {
                if (streams[s].ready.isEmpty()) {
                    allReady = false;
                }
            }
            if (allReady) {
                break;
            }

            if (!pumpOnePacket()) {
                return false;
            }
        }

        for (int s = 0; s < numStreams; s++) {
            frames[s] = streams[s].ready.removeFirst();
        }
        return true;
    }

    private boolean pumpOnePacket() {
        if (sourceExhausted) {
            return false;
        }
        if (!container.readPacket(packetBuf)) {
            sourceExhausted = true;
            return false;
        }

        int streamIdx = nextStream;
        nextStream = (nextStream + 1) % numStreams;

        StreamState st = streams[streamIdx];
        ensureCapacity(st, st.fill + PAYLOAD_BYTES_PER_PACKET);
        System.arraycopy(packetBuf, 4, st.bits, st.fill, PAYLOAD_BYTES_PER_PACKET);
        st.fill += PAYLOAD_BYTES_PER_PACKET;

        drainFramesFromStream(st);
        return true;
    }

    private static void drainFramesFromStream(StreamState st) {
        while (true) {
            int available = st.fill * 8 - st.readBitPos;
            if (available < FRAME_SIZE_BITS) {
                return;
            }

            BitReader sizeReader = new BitReader(st.bits, st.readBitPos, FRAME_SIZE_BITS);
            int frameSize = sizeReader.readBits(FRAME_SIZE_BITS);

            if (frameSize == END_OF_STREAM_MARKER || frameSize == 0) {
                if (!skipToNextPacket(st)) {
                    return;
                }
                continue;
            }
            if (available < frameSize) {
                return; // not yet complete
            }

            int frameBytes = (frameSize + 7) / 8;
            byte[] frameData = new byte[frameBytes];
            copyBits(st.bits, st.readBitPos, frameData, 0, frameSize);

            st.ready.addLast(new XmaFrame(frameData, 0, frameSize));

            st.readBitPos += frameSize;
            compactBuffer(st);
        }
    }

    private static void ensureCapacity(StreamState st, int requiredBytes) {
        if (st.bits.length >= requiredBytes) {
            return;
        }
        int newSize = st.bits.length;
        while (newSize < requiredBytes) {
            newSize *= 2;
        }
        st.bits = Arrays.copyOf(st.bits, newSize);
    }

    private static void compactBuffer(StreamState st) {
        int dropPackets = st.readBitPos / (PAYLOAD_BYTES_PER_PACKET * 8);
        if (dropPackets == 0) {
            return;
        }

        int dropBytes = dropPackets * PAYLOAD_BYTES_PER_PACKET;
        int remaining = st.fill - dropBytes;
        System.arraycopy(st.bits, dropBytes, st.bits, 0, remaining);
        st.fill = remaining;
        st.readBitPos -= dropBytes * 8;
    }

    private static boolean skipToNextPacket(StreamState st) {
        final int packetBits = PAYLOAD_BYTES_PER_PACKET * 8;
        int next = (st.readBitPos / packetBits + 1) * packetBits;
        if (next > st.fill * 8) {
            return false;
        }
        st.readBitPos = next;
        return true;
    }

    private static void copyBits(byte[] src, int srcBitPos, byte[] dst, int dstBitPos, int count) {
        BitReader reader = new BitReader(src, srcBitPos, count);
        int written = dstBitPos;
        int remaining = count;
        while (remaining > 0) {
            int take = remaining < 8 ? remaining : 8;
            int chunk = reader.readBits(take);
            int byteIndex = written >> 3;
            int bitInByte = written & 7;
            int shift = 8 - bitInByte - take;
            if (shift >= 0) {
                dst[byteIndex] |= (byte) (chunk << shift);
            } else {
                dst[byteIndex] |= (byte) (chunk >> -shift);
                dst[byteIndex + 1] |= (byte) ((chunk << (8 + shift)) & 0xFF);
            }
            written += take;
            remaining -= take;
        }
    }
}
