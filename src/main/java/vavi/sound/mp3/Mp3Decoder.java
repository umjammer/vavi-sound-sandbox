/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mp3;

import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * MPEG Audio Layer III Decorder.
 *
 * @author 小杉 篤史 (Kosugi Atsushi)
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 1.00 original version <br>
 * @version 2.00 030817 nsano java port <br>
 */
class Mp3Decoder {
    /** */
    private static class GrInfo {
        int length;
        int bigValues;
        int gain;
        GrInfo(int length, int bigValues, int gain) {
            this.length = length;
            this.bigValues = bigValues;
            this.gain = gain;
        }
        public String toString() {
            return StringUtil.paramString(this);
        }
    }

    /** */
    static class MpegDecodeParam {
        MpegHeader header;
        /** mp3 */
        byte[] inputBuf;
        int inputSize;
        /** pcm */
        byte[] outputBuf;
        int outputSize;
    }

    /** */
    static class MpegDecodeInfo {
        MpegHeader header;
        /** 出力チャネル */
        int channels;
        /** サンプリングレート[Hz] */
        int frequency;
        /** ビットレート[bit/s] */
        int bitRate;
        /** 1フレームの入力サイズ */
        int inputSize;
        /** 1フレームの出力サイズ */
        int outputSize;
    }

    /** ヘッダ情報 4 byte */
    static class MpegHeader {
        static final int[][] m_bitrate = {
            // layer1
            { -1, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, -1 },
            // layer2
            { -1, 32, 48, 56,  64,  80,  96, 112, 128, 160, 192, 224, 256, 320, 384, -1 },
            // layer3
            { -1, 32, 40, 48,  56,  64,  80,  96, 112, 128, 160, 192, 224, 256, 320, -1 }
        };
        static final int[][] m_frequency = {
            // mpeg1.0
            { 44100, 48000, 32000 },
            // mpeg2.0
            { 22050, 24000, 16000 }
        };

        /**
         * <ul>
         * <li>00 - MPEG Version 2.5
         * <li>01 - reserved
         * <li>10 - MPEG Version 2
         * <li>11 - MPEG Version 1
         * </ul>
         */
        int version;
        /**
         * <ul>
         * <li>00 - reserved
         * <li>01 - Layer III
         * <li>10 - Layer II
         * <li>11 - Layer I
         * </ul>
         */
        int layer;
        /** */
        int bitrate;
        /** */
        int frequency;
        /**
         * <ol>
         * <li> stereo
         * <li> joint stereo
         * <li> dual channel
         * <li> single channel
         * </ol>
         */
        int mode;
        /** ヘッダ情報の取得 */
        public MpegHeader(byte[] buf, int offset) {
Debug.println("offset: " + offset);
            this.version   = (buf[offset + 1] & 0x18) >> 3;
            this.layer     = (buf[offset + 1] & 0x06) >> 1;
            this.bitrate   = m_bitrate[3 - layer][(buf[offset + 2] & 0xf0) >> 4];
            this.frequency = m_frequency[version == 3 ? 0 : 1][(buf[offset + 2] & 0x0c) >> 2];
            this.mode      = (buf[offset + 3] & 0xc0) >> 6;
Debug.println(StringUtil.paramString(this));
        }
    }

    private static final int GR_MAX = 2;
    private static final int GR_SIZE = 576;
    private static final int GR_MAXSIZE = GR_SIZE * GR_MAX;
    private static final int CH_MAX = 2;
    private static final int HAN_SIZE = 512;
    private static final int SCALE_BLOCK = 36;
    private static final int SCALE_RANGE = 64;
    private static final int SCALE = 32768;
    private static final int SB_SIZE = 32;
    private static final int SS_SIZE = 18;
//    private static final int NBUF = 8 * 1024;
//    private static final int BUF_TRIGGER = NBUF - 1500;

    /** */
    private int m_channels;
    private int m_freq;
    private int m_frame_size;
    private int m_pcm_size;

    /**
     * searches nearest sync address.
     * @return relative address from offset
     */
    public static int findSync(byte[] buf, int offset, int size) {
        size -= 3;

        if (size <= 0) {
            throw new IndexOutOfBoundsException(size + " < 0");
        }

        int i = 0;

        for (; i < size - 1; i++) {
            if ((buf[offset + i] & 0xff) == 0xff && (buf[offset + i + 1] & 0xe0) == 0xe0) {
                break;
            }
//System.err.println("buf[" + (i + offset) + "]: " + StringUtil.toHex2(buf[i + offset]));
        }

        if (i == size - 1) {
            throw new IllegalArgumentException("result: " + i + "/ size: " + size);
        }

        return i;
    }

    /** */
    public MpegDecodeInfo getInfo(byte[] mpeg, int offset, int size) {
        MpegDecodeInfo info = new MpegDecodeInfo();

        int p = 0;

        if (size < 156) {
            throw new IllegalArgumentException("size: " + size + " < 156");
        }

        info.header = new MpegHeader(mpeg, offset);

        MpegHeader header = info.header;
        p += 4;

        if (header.mode != 3) {
            p += 32;
        } else {
            p += 17;
        }

        info.bitRate = header.bitrate * 1000;
        info.frequency = header.frequency;
        info.outputSize = GR_MAXSIZE;
        info.inputSize = (144 * info.bitRate) / info.frequency;

        if (header.mode == 3) {
            info.channels = 1;
        } else {
            info.channels = 2;
        }

        info.outputSize *= (info.channels * 2);

        m_freq = info.frequency;
        m_pcm_size = info.outputSize;

        return info;
    }

    /** */
    public void prepareDecode(byte[] mpeg, int offset, int size) {
        MpegDecodeInfo info = getInfo(mpeg, offset, size);
        MpegHeader header = info.header;

        m_channels = (header.mode == 3) ? 1 : 2;

        l3dhybrid_init();
    }

    /**
     * @throws IllegalArgumentException when param#inputSize <= 4
     */
    public void decode(MpegDecodeParam param) throws IllegalArgumentException {
        if (param.inputSize <= 4) {
            throw new IllegalArgumentException("inputSize: " + param.inputSize + " <= 4");
        }

        param.header = new MpegHeader(param.inputBuf, 0);

        MpegHeader header = param.header;

        m_frame_size = (144 * header.bitrate * 1000) / m_freq;

        if (param.inputSize < m_frame_size) {
            throw new IllegalArgumentException("inputSize: " + param.inputSize + " < frameSize");
        }

        decode_frame(param.outputBuf, param.inputBuf);

        param.inputSize = m_frame_size;
        param.outputSize = m_pcm_size;
//Debug.println(StringUtil.paramString(param));
    }

    /**
     * @param outBuf pcm
     * @param inBuf mp3
     */
    private void decode_frame(byte[] outBuf, byte[] inBuf) {
        int main_pos_bit = 0;
        int channels = m_channels;
        GrInfo[][] info = new GrInfo[GR_MAX][CH_MAX];
        int[] dec = new int[GR_SIZE];
        float[] spec = new float[GR_SIZE];
        float[] sample = new float[GR_SIZE];

        int inBuf_pointer = 0;

        bitget_init(inBuf, 4);

        int side_size = l3dstream_sideinfo(info, channels);
        inBuf_pointer += (4 + side_size);

        for (int gr = 0; gr < 2; gr++) {
            for (int ch = 0; ch < channels; ch++) {
                GrInfo gr_info = info[gr][ch];

                // extract bitstream
                bitget_init(inBuf, inBuf_pointer + (main_pos_bit >> 3));
                int bit0 = (main_pos_bit & 7);

                if (bit0 != 0) {
                    bitget(bit0);
                }

                main_pos_bit += gr_info.length;
                bitget_init_end(inBuf_pointer + ((main_pos_bit + 39) >> 3));

                // huffman decode
                l3huff_decode(dec, gr_info);

                // dequantum
                l3dequantum(dec, spec, gr_info);

                // hybrid filter bank
                l3dhybrid(sample, spec, ch);

                // output to buffer
                for (int i = 0; i < GR_SIZE; i++) {
                    if (sample[i] > 1.0) {
                        sample[i] = (float) (1.0 - 1.0e-5);
                    }

                    if (sample[i] < -1.0) {
                        sample[i] = (float) (-1.0 + 1.0e-5);
                    }

                    outBuf[(((gr * GR_SIZE) + i) * channels) + ch] = (byte) (sample[i] * (SCALE - 1));
                }
            }
        }
    }

    /**
     * dequantum
     */
    private static void l3dequantum(int[] samp, float[] spec, GrInfo gr_info) {
        float xs = (float) Math.pow(2.0, (gr_info.gain - 210.0) / 4);

        for (int i = 0; i < GR_SIZE; i++) {
            spec[i] = (float) (xs * samp[i] * Math.pow(Math.abs(samp[i]), 1.0 / 3.0));
        }
    }

    /**
     * huffman decoder
     */
    private void l3huff_decode(int[] dec, GrInfo gr_info) {
        int big_values = gr_info.bigValues * 2;

        huff_decodebits(dec, big_values - 4);

        for (int i = big_values; i < GR_SIZE; i++) {
            dec[i] = 0;
        }
    }

    /**
     * extracts bitstream
     */
    private int l3dstream_sideinfo(GrInfo[][] info, int channels) {
        int size;

        bitget(9);

        if (channels == 1) {
            bitget(5);
            size = 17;
        } else {
            bitget(3);
            size = 32;
        }

        for (int ch = 0; ch < channels; ch++) {
            bitget(4);
        }

        for (int gr = 0; gr < 2; gr++) {
            for (int ch = 0; ch < channels; ch++) {
                info[gr][ch] = new GrInfo(bitget(12), bitget(9), bitget(8));
Debug.println(info[gr][ch]);
                bitget(4);
                bitget(1);
                bitget(5);
                bitget(5);
                bitget(5);
                bitget(4);
                bitget(3);
                bitget(1);
                bitget(1);
                bitget(1);
            }
        }

        return size;
    }

    /**
     * huffman encoder
     */
    private static final int[] huff_tbl_16 = {
            0xff000001, 0x00000003, 0x01000000,
            0xff000001, 0x00000009, 0x00000003,
            0xff000001, 0x00000003, 0x01000100,
            0xff000001, 0x01010100, 0x01010000,
            0xff000001, 0x00000030, 0x00000003,
            0xff000001, 0x0000000c, 0x00000003,
            0xff000001, 0x00000006, 0x00000003,
            0xff000001, 0x01020000, 0x01000200,
            0xff000001, 0x01020100, 0x01010200,
            0xff000001, 0x0000000c, 0x00000003,
            0xff000001, 0x00000006, 0x00000003,
            0xff000001, 0x01030100, 0x01010300,
            0xff000001, 0x02000300, 0x01020200,
            0xff000001, 0x00000003, 0x0000000c,
            0xff000001, 0x00000006, 0x00000003,
            0xff000001, 0x0000012f, 0x01040100,
            0xff000001, 0x00000126, 0x00000129,
            0xff000001, 0x00000006, 0x00000003,
            0xff000001, 0x01030200, 0x01020300,
            0xff000001, 0x01010400, 0x00000126,
            0xff000001, 0x00000003, 0x00000014,
            0xff000004,
            0x00000022, 0x0000002b, 0x00000034, 0x040f0f00,
            0x00000039, 0x0000003e, 0x00000041, 0x04020f00,
            0x00000052, 0x040f0100, 0x04010f00, 0x00000055,
            0x00000066, 0x00000077, 0x00000088, 0x00000099,
            0xff000004,
            0x00000099, 0x000000aa, 0x000000b3, 0x000000bc,
            0x000000c5, 0x000000ce, 0x000000d7, 0x000000e0,
            0x000000e9, 0x000000ee, 0x000000f3, 0x000000f6,
            0x000000fb, 0x00000100, 0x04010500, 0x00000103,
            0xff000003,
            0x030f0e00, 0x030e0f00, 0x030f0d00, 0x030d0f00,
            0x030f0c00, 0x030c0f00, 0x030f0b00, 0x030b0f00,
            0xff000003,
            0x020f0a00, 0x020f0a00, 0x030a0f00, 0x030f0900,
            0x03090f00, 0x03080f00, 0x020f0800, 0x020f0800,
            0xff000002,
            0x020f0700, 0x02070f00, 0x020f0600, 0x02060f00,
            0xff000002,
            0x020f0500, 0x02050f00, 0x010f0400, 0x010f0400,
            0xff000001, 0x01040f00, 0x01030f00,
            0xff000004,
            0x01000f00, 0x01000f00, 0x01000f00, 0x01000f00,
            0x01000f00, 0x01000f00, 0x01000f00, 0x01000f00,
            0x020f0300, 0x020f0300, 0x020f0300, 0x020f0300,
            0x000000e2, 0x000000f3, 0x000000fc, 0x00000105,
            0xff000001, 0x010f0200, 0x010f0000,
            0xff000004,
            0x000000fa, 0x000000ff, 0x00000104,
            0x00000109, 0x0000010c, 0x00000111, 0x00000116,
            0x00000119, 0x0000011e, 0x00000123, 0x00000128,
            0x04030e00, 0x0000012d, 0x00000130, 0x00000133,
            0x00000136, 0xff000004, 0x00000128, 0x0000012b,
            0x0000012e, 0x040d0001, 0x00000131, 0x00000134,
            0x00000137, 0x040c0300, 0x0000013a, 0x040c0100,
            0x04000c00, 0x0000013d, 0x03020e00, 0x03020e00,
            0x040e0200, 0x040e0100, 0xff000004, 0x04030d00,
            0x040d0200, 0x04020d00, 0x04010d00, 0x040b0300,
            0x0000012f, 0x030d0100, 0x030d0100, 0x04040c00,
            0x040b0600, 0x04030c00, 0x04070a00, 0x030c0200,
            0x030c0200, 0x04020c00, 0x04050b00, 0xff000004,
            0x04010c00, 0x040c0000, 0x040b0400, 0x04040b00,
            0x040a0600, 0x04060a00, 0x03030b00, 0x03030b00,
            0x040a0500, 0x04050a00, 0x030b0200, 0x030b0200,
            0x03020b00, 0x03020b00, 0x030b0100, 0x030b0100,
            0xff000004, 0x03010b00, 0x03010b00, 0x040b0000,
            0x04000b00, 0x04090600, 0x04060900, 0x040a0400,
            0x04040a00, 0x04080700, 0x04070800, 0x03030a00,
            0x03030a00, 0x040a0300, 0x04090500, 0x030a0200,
            0x030a0200, 0xff000004, 0x04050900, 0x04080600,
            0x03010a00, 0x03010a00, 0x04060800, 0x04070700,
            0x03040900, 0x03040900, 0x04090400, 0x04070500,
            0x03070600, 0x03070600, 0x02020a00, 0x02020a00,
            0x02020a00, 0x02020a00, 0xff000003, 0x020a0100,
            0x020a0100, 0x030a0000, 0x03000a00, 0x03090300,
            0x03030900, 0x03080500, 0x03050800, 0xff000003,
            0x02090200, 0x02090200, 0x02020900, 0x02020900,
            0x03060700, 0x03090000, 0x02090100, 0x02090100,
            0xff000003, 0x02010900, 0x02010900, 0x03000900,
            0x03080400, 0x03040800, 0x03050700, 0x03080300,
            0x03030800, 0xff000003, 0x03060600, 0x03080200,
            0x02020800, 0x02020800, 0x03070400, 0x03040700,
            0x02080100, 0x02080100, 0xff000003, 0x02010800,
            0x02010800, 0x02000800, 0x02000800, 0x03080000,
            0x03060500, 0x02070300, 0x02070300, 0xff000003,
            0x02030700, 0x02030700, 0x03050600, 0x03060400,
            0x02070200, 0x02070200, 0x02020700, 0x02020700,
            0xff000003, 0x03040600, 0x03050500, 0x02070000,
            0x02070000, 0x01070100, 0x01070100, 0x01070100,
            0x01070100, 0xff000002, 0x01010700, 0x01010700,
            0x02000700, 0x02060300, 0xff000002, 0x02030600,
            0x02050400, 0x02040500, 0x02060200, 0xff000001,
            0x01020600, 0x01060100, 0xff000002, 0x01010600,
            0x01010600, 0x02060000, 0x02000600, 0xff000002,
            0x01030500, 0x01030500, 0x02050300, 0x02040400,
            0xff000001, 0x01050200, 0x01020500, 0xff000001,
            0x01050100, 0x01050000, 0xff000001, 0x01040300,
            0x01030400, 0xff000001, 0x01000500, 0x01040200,
            0xff000001, 0x01020400, 0x01030300, 0xff000001,
            0x01040000, 0x01000400, 0xff000004, 0x040e0c00,
            0x00000086, 0x030e0d00, 0x030e0d00, 0x03090e00,
            0x03090e00, 0x040a0e02, 0x04090d00, 0x020e0e00,
            0x020e0e00, 0x020e0e00, 0x020e0e02, 0x030d0e00,
            0x030d0e00, 0x030b0e00, 0x030b0e00, 0xff000003,
            0x020e0b00, 0x020e0b00, 0x020d0c00, 0x020d0c00,
            0x030c0d00, 0x030b0d00, 0x020e0a00, 0x020e0a00,
            0xff000003, 0x020c0c00, 0x020c0c00, 0x030d0a00,
            0x030a0d00, 0x030e0700, 0x030c0a00, 0x020a0c00,
            0x020a0c00, 0xff000003, 0x03090c00, 0x030d0700,
            0x020e0500, 0x020e0500, 0x010d0b00, 0x010d0b00,
            0x010d0b00, 0x010d0b00, 0xff000002, 0x010e0900,
            0x010e0900, 0x020c0b00, 0x020b0c00, 0xff000002,
            0x020e0800, 0x02080e00, 0x020d0900, 0x02070e00,
            0xff000002, 0x020b0b00, 0x020d0800, 0x02080d00,
            0x020e0600, 0xff000001, 0x01060e00, 0x010c0900,
            0xff000002, 0x020b0a00, 0x020a0b00, 0x02050e00,
            0x02070d00, 0xff000002, 0x010e0400, 0x010e0400,
            0x02040e00, 0x020c0800, 0xff000001, 0x01080c00,
            0x010e0300, 0xff000002, 0x010d0600, 0x010d0600,
            0x02060d00, 0x020b0900, 0xff000002, 0x02090b00,
            0x020a0a00, 0x01010e00, 0x01010e00, 0xff000002,
            0x01040d00, 0x01040d00, 0x02080b00, 0x02090a00,
            0xff000002, 0x010b0700, 0x010b0700, 0x02070b00,
            0x02000d00, 0xff000001, 0x010e0000, 0x01000e00,
            0xff000001, 0x010d0500, 0x01050d00, 0xff000001,
            0x010c0700, 0x01070c00, 0xff000001, 0x010d0400,
            0x010b0800, 0xff000001, 0x010a0900, 0x010c0600,
            0xff000001, 0x01060c00, 0x010d0300,
            0xff000001, 0x010c0500, 0x01050c00,
            0xff000001, 0x010a0800, 0x01080a00,
            0xff000001, 0x01090900, 0x010c0400,
            0xff000001, 0x01060b00, 0x010a0700,
            0xff000001, 0x010b0500, 0x01090800,
            0xff000001, 0x01080900, 0x01090700,
            0xff000001, 0x01070900, 0x01080800,
            0xff000001, 0x010c0e00, 0x010d0d00
    };

    /** */
    private static int getPtr(int i) {
        return huff_tbl_16[i];
    }

    /** */
    private static int getY(int i) {
        return ((huff_tbl_16[i] & 0xff00) >> 8) & 0xff;
    }

    /** */
    private static int getX(int i) {
        return ((huff_tbl_16[i] & 0xff0000) >> 16) & 0xff;
    }

    /** */
    private static int signBits(int i) {
        return ((huff_tbl_16[i] & 0xff000000) >> 24) & 0xff;
    }

    /** */
    private static int purgeBits(int i) {
        return huff_tbl_16[i] & 0xff;
    }

    /**
     * huffman decoder
     */
    private void huff_decodebits(int[] xy, int n) {
        int code;
        int point;

        if (n <= 0) {
            return;
        }

        n = n >> 1;

        for (int i = 0; i < n; i++) {
            point = 0;

            while (true) {
                int bits = signBits(point);
//Debug.println("bits: " + bits);
                code = bitget2(bits) + 1;
//Debug.println("code: " + code);

                if (purgeBits(code + point) != 0) {
                    break;
                }

                point += getPtr(code + point);
                mac_bitget_purge(bits);
            }

            mac_bitget_purge(purgeBits(code + point));

            int x = getX(code + point);
            int y = getY(code + point);

            if (x == 15) {
                x += bitget_lb(13);
            }

            if (x != 0) {
                if (mac_bitget_1bit() != 0) {
                    x = -x;
                }
            }

            if (y == 15) {
                y += bitget_lb(13);
            }

            if (y != 0) {
                if (mac_bitget_1bit() != 0) {
                    y = -y;
                }
            }

            xy[i * 2] = (byte) x;
            xy[(i * 2) + 1] = (byte) y;
        }
    }

    //----

    /** */
    private int m_bits;
    /** */
    private long m_bitbuf;
    /** base */
    private int m_bs_ptr;
    /** base */
    private int m_bs_ptr0;
    /** */
    private int m_bs_ptr_end;

    /** */
    private byte[] base;

    /**
     * @param buf mp3
     * @param p offset
     */
    private void bitget_init(byte[] buf, int p) {
        base = buf;
        m_bs_ptr0 = m_bs_ptr = p;
        m_bits = 0;
        m_bitbuf = 0;
    }

    /**
     * extract bitstream
     */
    private void bitget_init_end(int buf_end) {
        m_bs_ptr_end = buf_end;
    }

    /** */
    private int bitget(int n) {

        if (m_bits < n) {
            while (m_bits <= 24) {
Debug.printf("%02x", base[m_bs_ptr]);
                m_bitbuf = ((m_bitbuf << 8)) | (base[m_bs_ptr++] & 0xff);
                m_bits += 8;
            }
        }

        m_bits -= n;
        int x = (int) ((m_bitbuf >> m_bits));
        m_bitbuf -= ((long) x << m_bits);

        return x;
    }

    /** */
    private int bitget_lb(int n) {

        if (m_bits < (n + 2)) {
            while (m_bits <= 24) {
                m_bitbuf = ((m_bitbuf << 8)) | (base[m_bs_ptr++] & 0xff);
                m_bits += 8;
            }
        }

        m_bits -= n;
        int x = (int) ((m_bitbuf >> m_bits));
        m_bitbuf -= ((long) x << m_bits);

        return x;
    }

    /** */
    private int bitget2(int n) {

        if (m_bits < (9 + 2)) {
            while (m_bits <= 24) {
                m_bitbuf = ((m_bitbuf << 8)) | (base[m_bs_ptr++] & 0xff);
                m_bits += 8;
            }
        }

        int x = (int) ((m_bitbuf >> (m_bits - n)));

        return x;
    }

    /** */
    private void mac_bitget_purge(int n) {
        m_bits -= n;
        m_bitbuf -= (((m_bitbuf >> m_bits) << m_bits));
    }

    /** */
    private int mac_bitget_1bit() {

        m_bits--;
        int code = (int) ((m_bitbuf >> m_bits));
        m_bitbuf -= ((long) code << m_bits);

        return code;
    }

    //----

    /* */
    private void l3dhybrid_init() {
        l3dalias_init();
        l3imdct_init();
    }

    /**
     * hybrid filter bank
     */
    private void l3dhybrid(float[] sample, float[] spec, int ch) {
        float[][] sbsamp = new float[SS_SIZE][SB_SIZE];

        // butterfly computation
        l3dalias(spec);

        // MDCT
        l3imdct(sbsamp, m_dsbsampprv[ch], spec);

        // sub band filter
        for (int ss = 0; ss < SS_SIZE; ss++) {
            l3dsubband(sample, ss * SB_SIZE, sbsamp[ss], ch);
        }
    }

    private final float[][] m_imdcttbl = new float[SCALE_BLOCK / 2][SCALE_BLOCK];
    private final float[][] m_imdctcos = new float[SCALE_BLOCK / 2][SCALE_BLOCK];

    /**
     */
    private final float[] m_imdctwin = new float[SCALE_BLOCK];
    private final float[][][] m_dsbsampprv = new float[CH_MAX][SS_SIZE][SB_SIZE];

    /** */
    private void l3imdct_init() {
        imdct_init();
    }

    /**
     * IMDCT
     */
    private void l3imdct(float[][] samp, float[][] sampprv, float[] spec) {
        float[] sampnow = new float[SCALE_BLOCK];

        for (int sb = 0; sb < SB_SIZE; sb++) {
            imdct(spec, sb * SS_SIZE, sampnow);

            for (int ss = 0; ss < SS_SIZE; ss++) {
                samp[ss][sb] = sampprv[ss][sb] + sampnow[ss];
            }

            for (int ss = 0; ss < SS_SIZE; ss++) {
                sampprv[ss][sb] = sampnow[SS_SIZE + ss];
            }
        }

        for (int sb = 1; sb < SB_SIZE; sb += 2) {
            for (int ss = 1; ss < SS_SIZE; ss += 2) {
                samp[ss][sb] = (float) (samp[ss][sb] * -1.0);
            }
        }
    }

    /**
     */
    private void imdct_init() {
        int n = SCALE_BLOCK;

        for (int m = 0; m < n; m++) {
            m_imdctwin[m] = (float) Math.sin((((2 * m) + 1) * Math.PI) / (2 * n));
        }

        for (int m = 0; m < n; m++) {
            for (int k = 0; k < (n / 2); k++) {
                m_imdctcos[k][m] = (float) Math.cos((((2 * k) + 1) * ((2 * m) +
                                                    1 + (n / 2f)) * Math.PI) / (2 * n));
                m_imdcttbl[k][m] = m_imdctwin[m] * m_imdctcos[k][m];
            }
        }
    }

    /**
     * IMDCT (inverse modified discrete cosine transform)
     */
    private void imdct(float[] in, int in_p, float[] out) {
        for (int m = 0; m < SCALE_BLOCK; m++) {
            out[m] = 0.0f;

            for (int k = 0; k < (SCALE_BLOCK / 2); k++) {
                out[m] += (in[in_p + k] * m_imdcttbl[k][m]);
            }
        }
    }

    /** */
    private final int[] m_dxoff = new int[CH_MAX];
    /** */
    private final float[][] m_dsbcostbl = new float[SB_SIZE][SCALE_RANGE];

    /* init table */
    {
        m_dxoff[0] = m_dxoff[1] = 0;

        for (int i = 0; i < SCALE_RANGE; i++) {
            for (int k = 0; k < SB_SIZE; k++) {
                m_dsbcostbl[k][i] = (float) Math.cos((((2 * k) + 1) * (i + 16) * Math.PI) / SCALE_RANGE);
            }
        }
    }

    /** coefficient for butterfly computation */
    private static final double[] m_dcn = {
        -0.6000, -0.5350, -0.3300, -0.1850,
        -0.0950, -0.0410, -0.0142, -0.0037
    };
    /** */
    private final float[] m_dca = new float[8];

    /** */
    private final float[] m_dcs = new float[8];

    /** */
    private void l3dalias_init() {
        for (int k = 0; k < 8; k++) {
            double sq = Math.sqrt(1.0 + (m_dcn[k] * m_dcn[k]));
            m_dca[k] = (float) (m_dcn[k] / sq);
            m_dcs[k] = (float) (1.0 / sq);
        }
    }

    /**
     * folded distortion reduction butterfly
     */
    private void l3dalias(float[] x) {
        int n = SB_SIZE - 1;
        int size = SS_SIZE;
        int x_p = 0;

        for (int k = 0; k < n; k++, x_p += size) {
            for (int i = 0; i < 8; i++) {
                float a = x[(x_p + size) - 1 - i];
                float b = x[x_p + size + i];
                x[(x_p + size) - 1 - i] = (a * m_dcs[i]) - (b * m_dca[i]);
                x[x_p + size + i] = (b * m_dcs[i]) + (a * m_dca[i]);
            }
        }
    }

    /**
     */
    private static final double[] m_dewin = {
            0.000000000, -0.000015259, -0.000015259, -0.000015259,
            -0.000015259, -0.000015259, -0.000015259, -0.000030518,
            -0.000030518, -0.000030518, -0.000030518, -0.000045776,
            -0.000045776, -0.000061035, -0.000061035, -0.000076294,
            -0.000076294, -0.000091553, -0.000106812, -0.000106812,
            -0.000122070, -0.000137329, -0.000152588, -0.000167847,
            -0.000198364, -0.000213623, -0.000244141, -0.000259399,
            -0.000289917, -0.000320435, -0.000366211, -0.000396729,
            -0.000442505, -0.000473022, -0.000534058, -0.000579834,
            -0.000625610, -0.000686646, -0.000747681, -0.000808716,
            -0.000885010, -0.000961304, -0.001037598, -0.001113892,
            -0.001205444, -0.001296997, -0.001388550, -0.001480103,
            -0.001586914, -0.001693726, -0.001785278, -0.001907349,
            -0.002014160, -0.002120972, -0.002243042, -0.002349854,
            -0.002456665, -0.002578735, -0.002685547, -0.002792358,
            -0.002899170, -0.002990723, -0.003082275, -0.003173828,
            -0.003250122, -0.003326416, -0.003387451, -0.003433228,
            -0.003463745, -0.003479004, -0.003479004, -0.003463745,
            -0.003417969, -0.003372192, -0.003280640, -0.003173828,
            -0.003051758, -0.002883911, -0.002700806, -0.002487183,
            -0.002227783, -0.001937866, -0.001617432, -0.001266479,
            -0.000869751, -0.000442505, 0.000030518, 0.000549316,
            0.001098633, 0.001693726, 0.002334595, 0.003005981,
            0.003723145, 0.004486084, 0.005294800, 0.006118774,
            0.007003784, 0.007919312, 0.008865356, 0.009841919,
            0.010848999, 0.011886597, 0.012939453, 0.014022827,
            0.015121460, 0.016235352, 0.017349243, 0.018463135,
            0.019577026, 0.020690918, 0.021789551, 0.022857666,
            0.023910522, 0.024932861, 0.025909424, 0.026840210,
            0.027725220, 0.028533936, 0.029281616, 0.029937744,
            0.030532837, 0.031005859, 0.031387329, 0.031661987,
            0.031814575, 0.031845093, 0.031738281, 0.031478882,
            0.031082153, 0.030517578, 0.029785156, 0.028884888,
            0.027801514, 0.026535034, 0.025085449, 0.023422241,
            0.021575928, 0.019531250, 0.017257690, 0.014801025,
            0.012115479, 0.009231567, 0.006134033, 0.002822876,
            -0.000686646, -0.004394531, -0.008316040, -0.012420654,
            -0.016708374, -0.021179199, -0.025817871, -0.030609131,
            -0.035552979, -0.040634155, -0.045837402, -0.051132202,
            -0.056533813, -0.061996460, -0.067520142, -0.073059082,
            -0.078628540, -0.084182739, -0.089706421, -0.095169067,
            -0.100540161, -0.105819702, -0.110946655, -0.115921021,
            -0.120697021, -0.125259399, -0.129562378, -0.133590698,
            -0.137298584, -0.140670776, -0.143676758, -0.146255493,
            -0.148422241, -0.150115967, -0.151306152, -0.151962280,
            -0.152069092, -0.151596069, -0.150497437, -0.148773193,
            -0.146362305, -0.143264771, -0.139450073, -0.134887695,
            -0.129577637, -0.123474121, -0.116577148, -0.108856201,
            -0.100311279, -0.090927124, -0.080688477, -0.069595337,
            -0.057617187, -0.044784546, -0.031082153, -0.016510010,
            -0.001068115, 0.015228271, 0.032379150, 0.050354004,
            0.069168091, 0.088775635, 0.109161377, 0.130310059,
            0.152206421, 0.174789429, 0.198059082, 0.221984863,
            0.246505737, 0.271591187, 0.297210693, 0.323318481,
            0.349868774, 0.376800537, 0.404083252, 0.431655884,
            0.459472656, 0.487472534, 0.515609741, 0.543823242,
            0.572036743, 0.600219727, 0.628295898, 0.656219482,
            0.683914185, 0.711318970, 0.738372803, 0.765029907,
            0.791213989, 0.816864014, 0.841949463, 0.866363525,
            0.890090942, 0.913055420, 0.935195923, 0.956481934,
            0.976852417, 0.996246338, 1.014617920, 1.031936646,
            1.048156738, 1.063217163, 1.077117920, 1.089782715,
            1.101211548, 1.111373901, 1.120223999, 1.127746582,
            1.133926392, 1.138763428, 1.142211914, 1.144287109,
            1.144989014, 1.144287109, 1.142211914, 1.138763428,
            1.133926392, 1.127746582, 1.120223999, 1.111373901,
            1.101211548, 1.089782715, 1.077117920, 1.063217163,
            1.048156738, 1.031936646, 1.014617920, 0.996246338,
            0.976852417, 0.956481934, 0.935195923, 0.913055420,
            0.890090942, 0.866363525, 0.841949463, 0.816864014,
            0.791213989, 0.765029907, 0.738372803, 0.711318970,
            0.683914185, 0.656219482, 0.628295898, 0.600219727,
            0.572036743, 0.543823242, 0.515609741, 0.487472534,
            0.459472656, 0.431655884, 0.404083252, 0.376800537,
            0.349868774, 0.323318481, 0.297210693, 0.271591187,
            0.246505737, 0.221984863, 0.198059082, 0.174789429,
            0.152206421, 0.130310059, 0.109161377, 0.088775635,
            0.069168091, 0.050354004, 0.032379150, 0.015228271,
            -0.001068115, -0.016510010, -0.031082153, -0.044784546,
            -0.057617187, -0.069595337, -0.080688477, -0.090927124,
            -0.100311279, -0.108856201, -0.116577148, -0.123474121,
            -0.129577637, -0.134887695, -0.139450073, -0.143264771,
            -0.146362305, -0.148773193, -0.150497437, -0.151596069,
            -0.152069092, -0.151962280, -0.151306152, -0.150115967,
            -0.148422241, -0.146255493, -0.143676758, -0.140670776,
            -0.137298584, -0.133590698, -0.129562378, -0.125259399,
            -0.120697021, -0.115921021, -0.110946655, -0.105819702,
            -0.100540161, -0.095169067, -0.089706421, -0.084182739,
            -0.078628540, -0.073059082, -0.067520142, -0.061996460,
            -0.056533813, -0.051132202, -0.045837402, -0.040634155,
            -0.035552979, -0.030609131, -0.025817871, -0.021179199,
            -0.016708374, -0.012420654, -0.008316040, -0.004394531,
            -0.000686646, 0.002822876, 0.006134033, 0.009231567,
            0.012115479, 0.014801025, 0.017257690, 0.019531250,
            0.021575928, 0.023422241, 0.025085449, 0.026535034,
            0.027801514, 0.028884888, 0.029785156, 0.030517578,
            0.031082153, 0.031478882, 0.031738281, 0.031845093,
            0.031814575, 0.031661987, 0.031387329, 0.031005859,
            0.030532837, 0.029937744, 0.029281616, 0.028533936,
            0.027725220, 0.026840210, 0.025909424, 0.024932861,
            0.023910522, 0.022857666, 0.021789551, 0.020690918,
            0.019577026, 0.018463135, 0.017349243, 0.016235352,
            0.015121460, 0.014022827, 0.012939453, 0.011886597,
            0.010848999, 0.009841919, 0.008865356, 0.007919312,
            0.007003784, 0.006118774, 0.005294800, 0.004486084,
            0.003723145, 0.003005981, 0.002334595, 0.001693726,
            0.001098633, 0.000549316, 0.000030518, -0.000442505,
            -0.000869751, -0.001266479, -0.001617432, -0.001937866,
            -0.002227783, -0.002487183, -0.002700806, -0.002883911,
            -0.003051758, -0.003173828, -0.003280640, -0.003372192,
            -0.003417969, -0.003463745, -0.003479004, -0.003479004,
            -0.003463745, -0.003433228, -0.003387451, -0.003326416,
            -0.003250122, -0.003173828, -0.003082275, -0.002990723,
            -0.002899170, -0.002792358, -0.002685547, -0.002578735,
            -0.002456665, -0.002349854, -0.002243042, -0.002120972,
            -0.002014160, -0.001907349, -0.001785278, -0.001693726,
            -0.001586914, -0.001480103, -0.001388550, -0.001296997,
            -0.001205444, -0.001113892, -0.001037598, -0.000961304,
            -0.000885010, -0.000808716, -0.000747681, -0.000686646,
            -0.000625610, -0.000579834, -0.000534058, -0.000473022,
            -0.000442505, -0.000396729, -0.000366211, -0.000320435,
            -0.000289917, -0.000259399, -0.000244141, -0.000213623,
            -0.000198364, -0.000167847, -0.000152588, -0.000137329,
            -0.000122070, -0.000106812, -0.000106812, -0.000091553,
            -0.000076294, -0.000076294, -0.000061035, -0.000061035,
            -0.000045776, -0.000045776, -0.000030518, -0.000030518,
            -0.000030518, -0.000030518, -0.000015259, -0.000015259,
            -0.000015259, -0.000015259, -0.000015259, -0.000015259
    };

    /** */
    private final float[][] m_dxbuf = new float[CH_MAX][HAN_SIZE * 2];

    /**
     * sub band synthesis filter
     */
    private void l3dsubband(float[] sample, int sample_p, float[] s, int ch) {
        float coef = -1.0f;
        float[] w = new float[512];

        // frequency band signal calculation (IDCT,32×64)
        for (int i = 0; i < SCALE_RANGE; i++) {
            float sum = 0.0f;

            for (int k = 0; k < SB_SIZE; k++) {
                sum += (s[k] * m_dsbcostbl[k][i]);
            }

            m_dxbuf[ch][m_dxoff[ch] + i] = sum;
        }

        // filter synthesis
        for (int i = 0; i < SB_SIZE; i++) {
            for (int j = 0; j < 8; j++) {
                int l = i + (j * 64);
                int l2 = (m_dxoff[ch] + i + (j * 128)) & ((HAN_SIZE * 2) - 1);
                int m = i + (j * 64) + 32;
                int m2 = (m_dxoff[ch] + i + (j * 128) + 96) &
                         ((HAN_SIZE * 2) - 1);
                w[l] = (float) (coef * m_dewin[l] * m_dxbuf[ch][l2]);
                w[m] = (float) (coef * m_dewin[m] * m_dxbuf[ch][m2]);
                coef = (float) (coef * -1.0);
            }
        }

        // calculation of period addition signal
        for (int i = 0; i < SB_SIZE; i++) {
            float sum = 0.0f;

            for (int j = 0; j < 16; j++) {
                sum += w[(j * SB_SIZE) + i];
            }

            sample[sample_p + i] = sum;
        }

        m_dxoff[ch] = (m_dxoff[ch] - SCALE_RANGE) & ((HAN_SIZE * 2) - 1);
    }
}
