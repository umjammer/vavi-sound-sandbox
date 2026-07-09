/*
 * Copyright 2002 Michael Kohn (mike@naken.cc)
 */

package vavi.sound.pcm.equalizing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

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
public class Normalizer {

    /**
     * Peak-normalizes a block of signed PCM samples (the same scaling the
     * WAV-file {@link #normalize(String, String)} path applies to the data
     * chunk). The buffer is scanned for the deepest peak and every sample is
     * multiplied so that the peak reaches full scale.
     *
     * @param pcm  interleaved little-endian PCM; modified in place
     * @param bits 8 or 16 bits per sample
     * @return {@code pcm} (for convenience)
     */
    public static byte[] normalize(byte[] pcm, int bits) {
        int deepest = 0;
        if (bits == 16) {
            for (int i = 0; i + 1 < pcm.length; i += 2) {
                short r = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xff));
                deepest = Math.max(deepest, Math.abs(r));
            }
        } else if (bits == 8) {
            for (byte b : pcm) {
                deepest = Math.max(deepest, Math.abs((b & 0xff) - 128));
            }
        } else {
            throw new IllegalArgumentException("unsupported bits: " + bits);
        }

        if (deepest == 0) {
            return pcm; // silence, nothing to scale
        }

        double ratio = (bits == 16 ? 32767.0 : 127.0) / deepest;

        if (bits == 16) {
            for (int i = 0; i + 1 < pcm.length; i += 2) {
                short r = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xff));
                int scaled = (int) (r * ratio);
                scaled = Math.clamp(scaled, -32768, 32767);
                pcm[i] = (byte) (scaled & 0xff);
                pcm[i + 1] = (byte) ((scaled >> 8) & 0xff);
            }
        } else {
            for (int i = 0; i < pcm.length; i++) {
                int s = (pcm[i] & 0xff) - 128;
                int scaled = Math.clamp((int) (s * ratio), -128, 127);
                pcm[i] = (byte) (scaled + 128);
            }
        }
        return pcm;
    }

    /** */
    static int parse_header(InputStream in, OutputStream out) throws IOException {
        int length;
        byte[] riff_type = new byte[4];

        LittleEndianDataInputStream leis = new LittleEndianDataInputStream(in); 
        length = leis.readInt();
        int read = 0;
        while (read < 4) {
            int r = leis.read(riff_type, read, 4 - read);
            if (r == -1) break;
            read += r;
        }

        if (out == null) {
            System.out.print("RIFF Header\n");
            System.out.print("----------------------------\n");
            System.out.printf("          Length: %d\n", length);
            System.out.printf("            Type: %s\n", new String(riff_type));
            System.out.print("----------------------------\n");
        }

        // Write RIFF Header

        if (out != null) {
            LittleEndianDataOutputStream leos = new LittleEndianDataOutputStream(out); 
            leos.writeBytes("RIFF");
            leos.writeInt(length);
            leos.writeBytes("WAVE");
        }

        return 0;
    }

    /** */
    static int parse_fmt(InputStream in, WAVE.fmt fmt_chunk, OutputStream out) throws IOException {
        int length;

        LittleEndianDataInputStream leis = new LittleEndianDataInputStream(in); 
        length = leis.readInt();
        fmt_chunk.setFormatId(leis.readShort());
        fmt_chunk.setNumberChannels(leis.readShort());
        fmt_chunk.setSamplingRate(leis.readInt());
        fmt_chunk.setBytesPerSecond(leis.readInt());
        fmt_chunk.setBlockSize(leis.readShort());
        fmt_chunk.setSamplingBits(leis.readShort());

        if (length > 16) {
            long skipped = 0;
            while (skipped < length - 16) {
                long s = in.skip(length - 16 - skipped);
                if (s <= 0) {
                    int b = in.read();
                    if (b == -1) break;
                    skipped++;
                } else {
                    skipped += s;
                }
            }
        }

        if (length % 2 != 0) {
            in.read();
        }

        if (out == null) {
            System.out.print("FMT Chunk\n");
            System.out.print("----------------------------\n");
            System.out.printf("          Length: %d\n", length);
            System.out.print("     Format Type: ");
            if (fmt_chunk.getFormatId() == 1) {
                System.out.print("PCM\n");
            } else {
                System.out.printf("unknown (%d)\n", fmt_chunk.getFormatId());
            }

            System.out.printf(" Channel Numbers: %d\n", fmt_chunk.getNumberChannels());
            System.out.printf("     Sample Rate: %d\n", fmt_chunk.getSamplingRate());
            System.out.printf("Bytes Per Second: %d\n", fmt_chunk.getBytesPerSecond());
            System.out.print("Bytes Per Sample: ");
            if (fmt_chunk.getBlockSize() == 1) {
                System.out.printf("8 bit mono (%d)\n", fmt_chunk.getBlockSize());
            } else if (fmt_chunk.getBlockSize() == 2) {
                System.out.printf("8 bit stereo or 16 bit mono (%d)\n", fmt_chunk.getBlockSize());
            } else if (fmt_chunk.getBlockSize() == 4) {
                System.out.printf("16 bit stereo (%d)\n", fmt_chunk.getBlockSize());
            } else {
                System.out.printf("unknown (%d)\n", fmt_chunk.getBlockSize());
            }

            System.out.printf(" Bits Per Sample: %d\n", fmt_chunk.getSamplingBits());
            System.out.print("----------------------------\n");
        }

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
    static int parse_data(InputStream in, WAVE.fmt fmt_chunk, OutputStream out, double ratio) throws IOException {
        int length;
        int deepest = 0;
        int t;

        LittleEndianDataInputStream leis = new LittleEndianDataInputStream(in); 
        length = leis.readInt();

        if (out == null) {
            System.out.print("DATA chunk\n");
            System.out.print("----------------------------\n");
            System.out.printf("          Length: %d\n", length);
            System.out.print("Scanning for biggest/smallest peak\n");

            if (fmt_chunk.getSamplingBits() == 16) {
                for (t = 0; t < length / 2; t++) {
                    int b1 = in.read();
                    int b2 = in.read();
                    if (b1 == -1 || b2 == -1) {
                        break;
                    }
                    short r = (short) ((b2 << 8) | (b1 & 0xff));
                    int absVal = Math.abs(r);
                    if (absVal > deepest) {
                        deepest = absVal;
                    }
                }
            } else if (fmt_chunk.getSamplingBits() == 8) {
                for (t = 0; t < length; t++) {
                    int h = in.read();
                    if (h == -1) {
                        break;
                    }
                    int s = h - 128;
                    int absVal = Math.abs(s);
                    if (absVal > deepest) {
                        deepest = absVal;
                    }
                }
            } else {
                long skipped = 0;
                while (skipped < length) {
                    long s = in.skip(length - skipped);
                    if (s <= 0) {
                        int b = in.read();
                        if (b == -1) break;
                        skipped++;
                    } else {
                        skipped += s;
                    }
                }
            }

            if (length % 2 != 0) {
                in.read();
            }

            System.out.printf("Deepest: %d\n", deepest);
            return deepest;
        }

        // Write Data Chunk

        LittleEndianDataOutputStream leos = new LittleEndianDataOutputStream(out); 
        leos.writeBytes("data");
        leos.writeInt(length);

        if (fmt_chunk.getSamplingBits() == 16) {
            for (t = 0; t < length / 2; t++) {
                int b1 = in.read();
                int b2 = in.read();
                if (b1 == -1 || b2 == -1) {
                    break;
                }
                short r = (short) ((b2 << 8) | (b1 & 0xff));
                int scaled = (int) (r * ratio);
                if (scaled > 32767) scaled = 32767;
                else if (scaled < -32768) scaled = -32768;
                leos.writeShort((short) scaled);
            }
        } else if (fmt_chunk.getSamplingBits() == 8) {
            for (t = 0; t < length; t++) {
                int h = in.read();
                if (h == -1) {
                    break;
                }
                int s = h - 128;
                int scaled = (int) (s * ratio);
                if (scaled > 127) scaled = 127;
                else if (scaled < -128) scaled = -128;
                leos.write(scaled + 128);
            }
        } else {
            byte[] buf = new byte[4096];
            int rem = length;
            while (rem > 0) {
                int r = in.read(buf, 0, Math.min(buf.length, rem));
                if (r == -1) break;
                leos.write(buf, 0, r);
                rem -= r;
            }
        }

        if (length % 2 != 0) {
            int pad = in.read();
            if (pad != -1) {
                leos.write(pad);
            } else {
                leos.write(0);
            }
        }

        return 0;
    }

    /** */
    static int normalize(String inname, String outname) throws IOException {
        InputStream in;
        byte[] chunk_name = new byte[4];
        WAVE.fmt fmt_chunk = new WAVE.fmt();

        try {
            in = new BufferedInputStream(Files.newInputStream(Paths.get(inname)));
        } catch (IOException e) {
            System.out.printf("Couldn't open file for reading: %s\n", inname);
            return -1;
        }

        int deepest = 0;
        while (true) {
            if (in.read(chunk_name, 0, 4) < 4) {
                break;
            }

            String type = new String(chunk_name);
            switch (type) {
            case "RIFF" -> parse_header(in, null);
            case "fmt " -> parse_fmt(in, fmt_chunk, null);
            case "data" -> deepest = parse_data(in, fmt_chunk, null, 1.0);
            default -> {
                LittleEndianDataInputStream leis = new LittleEndianDataInputStream(in);
                int length = leis.readInt();
                long skipped = 0;
                while (skipped < length) {
                    long s = in.skip(length - skipped);
                    if (s <= 0) {
                        int b = in.read();
                        if (b == -1) break;
                        skipped++;
                    } else {
                        skipped += s;
                    }
                }
                if (length % 2 != 0) {
                    in.read();
                }
            }
            }
        }
        in.close();

        if (outname == null) {
            return 0;
        }

        double ratio;
        if (deepest == 0) {
            ratio = 1.0;
        } else {
            if (fmt_chunk.getSamplingBits() == 16) {
                ratio = 32767.0 / deepest;
            } else {
                ratio = 127.0 / deepest;
            }
        }

        System.out.print("Creating new wave\n");
        System.out.printf("Ratio: %.5f\n", ratio);

        InputStream in2;
        OutputStream out2;
        try {
            in2 = new BufferedInputStream(Files.newInputStream(Paths.get(inname)));
        } catch (IOException e) {
            System.out.printf("Couldn't open file for reading: %s\n", inname);
            return -1;
        }
        try {
            out2 = new BufferedOutputStream(Files.newOutputStream(Paths.get(outname)));
        } catch (IOException e) {
            in2.close();
            System.out.printf("Could not open file for writing: %s\n", outname);
            return -1;
        }

        while (true) {
            if (in2.read(chunk_name, 0, 4) < 4) {
                break;
            }

            String type = new String(chunk_name);
            switch (type) {
            case "RIFF" -> parse_header(in2, out2);
            case "fmt " -> parse_fmt(in2, fmt_chunk, out2);
            case "data" -> parse_data(in2, fmt_chunk, out2, ratio);
            default -> {
                LittleEndianDataInputStream leis = new LittleEndianDataInputStream(in2);
                int length = leis.readInt();
                LittleEndianDataOutputStream leos = new LittleEndianDataOutputStream(out2);
                leos.writeBytes(type);
                leos.writeInt(length);
                byte[] buf = new byte[4096];
                int rem = length;
                while (rem > 0) {
                    int r = in2.read(buf, 0, Math.min(buf.length, rem));
                    if (r == -1) break;
                    leos.write(buf, 0, r);
                    rem -= r;
                }
                if (length % 2 != 0) {
                    int pad = in2.read();
                    if (pad != -1) {
                        leos.write(pad);
                    } else {
                        leos.write(0);
                    }
                }
            }
            }
        }

        out2.close();
        in2.close();

        return 0;
    }
}
