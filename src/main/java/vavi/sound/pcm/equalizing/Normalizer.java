/*
 * Copyright 2002 Michael Kohn (mike@naken.cc)
 */

package vavi.sound.pcm.equalizing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import vavi.io.LittleEndianDataInputStream;
import vavi.io.LittleEndianDataOutputStream;
import vavi.util.win32.WAVE;


/**
 * Normalizer.
 *
 * @author <a href="mike@naken.cc">Michael Kohn</a> (nsano)
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060207 nsano port to Java <br>
 * @see "http://www.mikekohn.net/stuff/audio.php"
 */
class Normalizer {

    /** */
    int parse_header(InputStream in, OutputStream out) throws IOException {
        int length;
        byte[] riff_type = new byte[4];

        LittleEndianDataInputStream leis = new LittleEndianDataInputStream(in); 
        length = leis.readInt();
        leis.read(riff_type, 0, 4);

        System.out.printf("RIFF Header\n");
        System.out.printf("----------------------------\n");
        System.out.printf("          Length: %d\n", length);
        System.out.printf("            Type: %s\n", new String(riff_type));
        System.out.printf("----------------------------\n");

        // Write RIFF Header

        if (out != null) {
            LittleEndianDataOutputStream leos = new LittleEndianDataOutputStream(out); 
            leos.writeBytes("RIFF");
            leos.writeInt(4);
            leos.writeBytes("WAVE");
        }

        return 0;
    }

    /** */
    int parse_fmt(InputStream in, WAVE.fmt fmt_chunk, OutputStream out) throws IOException {
        int length;

        LittleEndianDataInputStream leis = new LittleEndianDataInputStream(in); 
        length = leis.readInt();
        fmt_chunk.setFormatId(leis.readShort());
        fmt_chunk.setNumberChannels(leis.readShort());
        fmt_chunk.setSamplingRate(leis.readInt());
        fmt_chunk.setBytesPerSecond(leis.readInt());
        fmt_chunk.setBlockSize(leis.readShort());
        fmt_chunk.setSamplingBits(leis.readShort());

        System.out.printf("FMT Chunk\n");
        System.out.printf("----------------------------\n");
        System.out.printf("          Length: %d\n", length);
        System.out.printf("     Format Type: ");
        if (fmt_chunk.getFormatId() == 0) {
            System.out.printf("Mono\n");
        } else if (fmt_chunk.getFormatId() == 1) {
            System.out.printf("Stereo\n");
        } else {
            System.out.printf("unknown\n");
        }

        System.out.printf(" Channel Numbers: %d\n", fmt_chunk.getNumberChannels());
        System.out.printf("     Sample Rate: %d\n", fmt_chunk.getSamplingRate());
        System.out.printf("Bytes Per Second: %d\n", fmt_chunk.getBytesPerSecond());
        System.out.printf("Bytes Per Sample: ");
        if (fmt_chunk.getBlockSize()== 1) {
            System.out.printf("8 bit mono (%d)\n", fmt_chunk.getBlockSize());
        } else if (fmt_chunk.getBlockSize() == 2) {
            System.out.printf("8 bit stereo or 16 bit mono (%d)\n", fmt_chunk.getBlockSize());
        } else if (fmt_chunk.getBlockSize() == 4) {
            System.out.printf("16 bit stereo (%d)\n", fmt_chunk.getBlockSize());
        }

        System.out.printf(" Bits Per Sample: %d\n", fmt_chunk.getSamplingBits());
        System.out.printf("----------------------------\n");

        // Write FMT Chunk

        if (out != null) {
            LittleEndianDataOutputStream leos = new LittleEndianDataOutputStream(out); 
            leos.writeBytes("fmt ");
            leos.writeInt(16);
            leos.writeShort(fmt_chunk.getFormatId());
            leos.writeShort(fmt_chunk.getNumberChannels());
            leos.writeInt(fmt_chunk.getSamplingRate());
            leos.writeInt(fmt_chunk.getBytesPerSecond());
            leos.writeShort(fmt_chunk.getBlockSize());
            leos.writeShort(fmt_chunk.getSamplingBits());
        }

        return 0;
    }

    /** */
    int parse_data(InputStream in, WAVE.fmt fmt_chunk, OutputStream out) throws IOException {
        int length;
        int deepest;
        int t;
        int h;
        int r;
        double ratio;

        deepest = 0;

        LittleEndianDataInputStream leis = new LittleEndianDataInputStream(in); 
        length = leis.readInt();
        in.mark(length); // TODO OutOfMemoryError

        System.out.printf("DATA chunk\n");
        System.out.printf("----------------------------\n");
        System.out.printf("          Length: %d\n", length);

        System.out.printf("Scanning for biggest/smallest peak\n");

        if (fmt_chunk.getSamplingBits() == 16) {
            for (t = 0; t < length / 2; t++) {
                r = in.read() + (in.read() << 8);
                r = Math.abs(r);
                if (r > deepest) {
                    deepest = r;
                }
            }
        } else if (fmt_chunk.getSamplingBits() == 8) {
            for (t = 0; t < length; t++) {
                h = in.read();
                h = Math.abs(h);
                if (h > deepest) {
                    deepest = h;
                }
            }
        }

        System.out.printf("Deepest: %d\n", deepest);

        if (out == null) {
            return 0;
        }

        System.out.printf("Creating new wave\n");

        in.reset();

        // Write Data Chunk

        LittleEndianDataOutputStream leos = new LittleEndianDataOutputStream(out); 
        leos.writeBytes("data");
        leos.writeInt(length);

        ratio = 32768 / (float) deepest;
        System.out.printf("Ratio: %.5f\n", ratio);

        if (fmt_chunk.getSamplingBits() == 16) {
            for (t = 0; t < length / 2; t++) {
                r = in.read() + (in.read() << 8);
                r = (int) (r * ratio);
                leos.writeShort(r);
            }
        } else if (fmt_chunk.getSamplingBits() == 8) {
            for (t = 0; t < length; t++) {
                h = in.read();
                h = (int) (h * ratio);
                out.write(h);
            }
        }

        return 0;
    }

    /** */
    int normalize(String inname, String outname) throws IOException {
        InputStream in;
        OutputStream out;
        byte[] chunk_name = new byte[4];
        WAVE.fmt fmt_chunk = new WAVE.fmt();

        try {
            in = new BufferedInputStream(new FileInputStream(inname));
        } catch (IOException e) {
            System.out.printf("Couldn't open file for reading: %s\n", inname);
            return -1;
        }

        if (outname != null) {
            try {
                out = new BufferedOutputStream(new FileOutputStream(outname));
            } catch (IOException e) {
                in.close();
                System.out.printf("Could not open file for writing: %s\n", outname);
                return 0;
            }
        } else {
            out = null;
        }

        while (true) {
            if (in.read(chunk_name, 0, 4) < 4) {
                break;
            }

            String type = new String(chunk_name);
            if (type.equals("RIFF")) {
                parse_header(in, out);
            } else if (type.equals("fmt ")) {
                parse_fmt(in, fmt_chunk, out);
            } else if (type.equals("data")) {
                parse_data(in, fmt_chunk, out);
            } else {
                System.out.printf("Unknown chunk: '%s'\n", type);
                return -1;
            }
        }

        if (out != null) {
            out.close();
        }
        in.close();

        return 0;
    }
}

/* */
