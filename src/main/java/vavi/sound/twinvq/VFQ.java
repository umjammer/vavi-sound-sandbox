/*
 * VQF demuxer
 * Copyright (c) 2009 Vitor Sessak
 *
 * This file is part of Libav.
 *
 * Libav is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * Libav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Libav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package vavi.sound.twinvq;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import vavi.sound.twinvq.LibAV.AVFormatContext;
import vavi.sound.twinvq.LibAV.AVInputFormat;
import vavi.sound.twinvq.LibAV.AVPacket;
import vavi.sound.twinvq.LibAV.AVProbeData;
import vavi.sound.twinvq.LibAV.AVStream;
import vavi.util.ByteUtil;
import vavi.util.Debug;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.twinvq.LibAV.AVERROR_INVALIDDATA;
import static vavi.sound.twinvq.LibAV.AV_INPUT_BUFFER_PADDING_SIZE;
import static vavi.sound.twinvq.LibAV.MKTAG;


/**
 * VFQ.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-08-09 nsano initial version <br>
 * @see "https://github.com/libav/libav/blob/master/libavformat/vqf.c"
 */
class VFQ {

    private static final Logger logger = getLogger(VFQ.class.getName());

    static final String TAG_COMM = "COMM";
    static final String TAG_DSIZ = "DSIZ";
    static final String TAG_YEAR = "YEAR";
    static final String TAG_ENCD = "ENCD";
    static final String TAG_EXTR = "EXTR";
    static final String TAG__YMH = "_YMH";
    static final String TAG__NTT = "_NTT";
    static final String TAG__ID3 = "_ID3";

    static class VqfContext {

        int frame_bit_len;
        byte last_frame_bits;
        int remaining_bits;
    }

    static int vqf_probe(byte[] buf) {
        if (ByteUtil.readLeInt(buf) != MKTAG('T', 'W', 'I', 'N'))
            return 0;

        if (Arrays.equals(buf, 4, 8, "97012000".getBytes(), 0, 8))
            return 100;

        if (Arrays.equals(buf, 4, 8, "00052200".getBytes(), 0, 8))
            return 100;

        return 50;
    }

    static void add_metadata(AVFormatContext s, String tag, int tag_len, int remaining) throws IOException {
        int len = Math.min(tag_len, remaining);
        byte[] buf;

        if (len == Integer.MAX_VALUE)
            return;

        buf = new byte[len + 1];
        s.pb.readFully(buf, 0, len);
        buf[len] = 0;
        s.metadata.put(tag, buf);
    }

    static final Map<String, String> vqf_metadata_conv = new HashMap<>();

    static {
        vqf_metadata_conv.put("(c) ", "copyright");
        vqf_metadata_conv.put("ARNG", "arranger");
        vqf_metadata_conv.put("AUTH", "author");
        vqf_metadata_conv.put("BAND", "band");
        vqf_metadata_conv.put("CDCT", "conductor");
        vqf_metadata_conv.put("COMT", "comment");
        vqf_metadata_conv.put("FILE", "filename");
        vqf_metadata_conv.put("GENR", "genre");
        vqf_metadata_conv.put("LABL", "publisher");
        vqf_metadata_conv.put("MUSC", "composer");
        vqf_metadata_conv.put("NAME", "title");
        vqf_metadata_conv.put("NOTE", "note");
        vqf_metadata_conv.put("PROD", "producer");
        vqf_metadata_conv.put("PRSN", "personnel");
        vqf_metadata_conv.put("REMX", "remixer");
        vqf_metadata_conv.put("SING", "singer");
        vqf_metadata_conv.put("TRCK", "track");
        vqf_metadata_conv.put("WORD", "words");
    }

    static int vqf_read_header(AVFormatContext s) {
        try {
            VFQ.VqfContext c = s.priv_data;
            AVStream st = new AVStream(s, null);
            byte[] tag_data = new byte[4];
            String chunk_tag;
            int rate_flag = -1;
            int header_size;
            int read_bitrate = 0;
            int size;
            byte[] comm_chunk = new byte[12];

            s.pb.skipBytes(12);

            header_size = s.pb.readInt(); // BE

//            st.codecpar.codec_type = AVMEDIA_TYPE_AUDIO;
//            st.codecpar.codec_id = AV_CODEC_ID_TWINVQ;
            st.start_time = 0;

            do {
                int len;
                s.pb.readFully(tag_data); // LE
                chunk_tag = new String(tag_data);

                if (chunk_tag.equals("DATA"))
                    break;

                len = s.pb.readInt(); // BE

                if (len < 0) {
                    logger.log(Level.ERROR, "Malformed header\n");
                    return -1;
                }

                header_size -= 8;

Debug.println("chunk: " + chunk_tag + ", " + len);
                switch (chunk_tag) {
                    case TAG_COMM -> {
                        s.pb.readFully(comm_chunk, 0, 12);
                        st.codecpar.channels = ByteUtil.readBeInt(comm_chunk) + 1;
                        read_bitrate = ByteUtil.readBeInt(comm_chunk, 4);
                        rate_flag = ByteUtil.readBeInt(comm_chunk, 8);
                        s.pb.skipBytes(len - 12);

                        st.codecpar.bit_rate = read_bitrate * 1000;
                    }
                    case TAG_DSIZ -> { // size of compressed data
                        size = s.pb.readInt(); // BE

                        s.metadata.put("size", String.valueOf(size));
                    }
                    case TAG_YEAR, // recording date
                         TAG_ENCD, // compression date
                         TAG_EXTR, // reserved
                         TAG__YMH, // reserved
                         TAG__NTT, // reserved
                         TAG__ID3 -> // reserved for ID3 tags
                        s.pb.skipBytes(Math.min(len, header_size));
                    default ->
                        add_metadata(s, chunk_tag, len, header_size);
                }

                header_size -= len;

            } while (header_size >= 0);

            switch (rate_flag) {
                case -1:
                    logger.log(Level.ERROR, "COMM tag not found!\n");
                    return -1;
                case 44:
                    st.codecpar.sample_rate = 44100;
                    break;
                case 22:
                    st.codecpar.sample_rate = 22050;
                    break;
                case 11:
                    st.codecpar.sample_rate = 11025;
                    break;
                default:
                    if (rate_flag < 8 || rate_flag > 44) {
                        logger.log(Level.ERROR, "Invalid rate flag %d\n", rate_flag);
                        return AVERROR_INVALIDDATA;
                    }
                    st.codecpar.sample_rate = rate_flag * 1000;
                    break;
            }

            if (read_bitrate / st.codecpar.channels < 8 || read_bitrate / st.codecpar.channels > 48) {
                logger.log(Level.ERROR, "Invalid bitrate per channel %d\n", read_bitrate / st.codecpar.channels);
                return AVERROR_INVALIDDATA;
            }

            switch (((st.codecpar.sample_rate / 1000) << 8) + read_bitrate / st.codecpar.channels) {
                case (11 << 8) + 8:
                case (8 << 8) + 8:
                case (11 << 8) + 10:
                case (22 << 8) + 32:
                    size = 512;
                    break;
                case (16 << 8) + 16:
                case (22 << 8) + 20:
                case (22 << 8) + 24:
                    size = 1024;
                    break;
                case (44 << 8) + 40:
                case (44 << 8) + 48:
                    size = 2048;
                    break;
                default:
                    logger.log(Level.ERROR, "Mode not suported: %d Hz, %d kb/s.\n",
                            st.codecpar.sample_rate, st.codecpar.bit_rate);
                    return -1;
            }
            c.frame_bit_len = st.codecpar.bit_rate * size / st.codecpar.sample_rate;
//            avpriv_set_pts_info(st, 64, size, st.codecpar.sample_rate);

            // put first 12 bytes of COMM chunk in extradata
            st.codecpar.extradata = new byte[12 + AV_INPUT_BUFFER_PADDING_SIZE];
            st.codecpar.extradata_size = 12;
            System.arraycopy(comm_chunk, 0, st.codecpar.extradata, 0, 12);
Debug.println("extradata_size: " + st.codecpar.extradata_size + "\n" + StringUtil.getDump(st.codecpar.extradata));

//            ff_metadata_conv_ctx(s, null, vqf_metadata_conv);

            return 0;
        } catch (IOException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            return -1;
        }
    }

    static AVPacket vqf_read_packet(AVFormatContext s) {
        try {
            VqfContext c = s.priv_data;
            int ret;
            int size = (c.frame_bit_len - c.remaining_bits + 7) >> 3;
Debug.println("size: " + size + ", blen: " + c.frame_bit_len + ", brem: " + c.remaining_bits + ", lfbits: " + (c.last_frame_bits & 0xff));

            AVPacket pkt = new AVPacket(size + 2);

            pkt.pos = 0; // s.pb.position();
            pkt.stream_index = 0;
            pkt.duration = 1;

            pkt.data[0] = (byte) (8 - c.remaining_bits); // Number of bits to skip
            pkt.data[1] = c.last_frame_bits;
            ret = s.pb.read(pkt.data, 2, size);

            if (ret <= 0) {
                throw new IllegalStateException("read");
            }

            c.last_frame_bits = pkt.data[size + 1];
            c.remaining_bits = (size << 3) - c.frame_bit_len + c.remaining_bits;

            return pkt;
        } catch (IOException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new UncheckedIOException(e);
        }
    }

    static int vqf_read_seek(AVFormatContext s, int stream_index, long timestamp, int flags) {
//        VqfContext c = s.priv_data;
//        AVStream st;
//        int ret;
//        long pos;
//
//        st = s.streams[stream_index];
//        pos = av_rescale_rnd(timestamp * st.codecpar.bit_rate,
//                st.time_base.num,
//                st.time_base.den * (long) c.frame_bit_len,
//                (flags & AVSEEK_FLAG_BACKWARD) ? AV_ROUND_DOWN : AV_ROUND_UP);
//        pos *= c.frame_bit_len;
//
//        st.cur_dts = av_rescale(pos, st.time_base.den, st.codecpar.bit_rate * (long) st.time_base.num);
//
//        if ((ret = s.pb.position(((pos - 7) >> 3) + s.internal.data_offset)) < 0)
//            return ret;
//
//        c.remaining_bits = (int) (-7 - ((pos - 7) & 7));
        return 0;
    }

    static AVInputFormat ff_vqf_demuxer = new AVInputFormat() {{
        name = "vqf";
        long_name = "Nippon Telegraph and Telephone Corporation (NTT) TwinVQ";
        priv_data_size = -1; // sizeof(VqfContext)
        read_probe = VFQ::vqf_probe;
        read_header = VFQ::vqf_read_header;
        read_packet = VFQ::vqf_read_packet;
        read_seek = VFQ::vqf_read_seek;
        extensions = "vqf,vql,vqe";
    }};
}