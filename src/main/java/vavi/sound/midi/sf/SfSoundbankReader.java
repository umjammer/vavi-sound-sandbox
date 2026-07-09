/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.sf;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.spi.SoundbankReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;

import vavi.sound.sf.SFont;


/**
 * SfSoundbankReader. reads MuseScore/Polyphone compressed soundfonts,
 * SF3 (Ogg Vorbis) and SF4 (FLAC), as a {@link Soundbank} playable by
 * gervill. plain SF2 files are left to the JDK's own reader.
 * <p>
 * decoders for the compressed samples are discovered via
 * {@code javax.sound.sampled} spi, e.g. tritonus-pvorbis for SF3 and
 * a flac provider for SF4.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-04 nsano initial version <br>
 * @see SfSoundbank
 */
public class SfSoundbankReader extends SoundbankReader {

    /** enough for RIFF + LIST/INFO headers + the leading ifil chunk */
    private static final int SNIFF_LENGTH = 64;

    @Override
    public Soundbank getSoundbank(URL url) throws InvalidMidiDataException, IOException {
        if ("file".equals(url.getProtocol())) {
            try {
                return getSoundbank(new File(url.toURI()));
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
        try (InputStream is = new BufferedInputStream(url.openStream())) {
            return getSoundbank(is);
        }
    }

    @Override
    public Soundbank getSoundbank(InputStream stream) throws InvalidMidiDataException, IOException {
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        stream.mark(SNIFF_LENGTH);
        byte[] head = stream.readNBytes(SNIFF_LENGTH);
        if (!isCompressedSoundFont(head)) {
            stream.reset();
            return null;
        }
        byte[] rest = stream.readAllBytes();
        byte[] data = new byte[head.length + rest.length];
        System.arraycopy(head, 0, data, 0, head.length);
        System.arraycopy(rest, 0, data, head.length, rest.length);

        SFont.SoundFont sf = new SFont.SoundFont(new ByteArrayChannel(data), "SF");
        sf.read();
        return SfSoundbank.toSoundbank(sf);
    }

    @Override
    public Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException {
        try (InputStream is = Files.newInputStream(file.toPath())) {
            byte[] head = is.readNBytes(SNIFF_LENGTH);
            if (!isCompressedSoundFont(head)) {
                return null;
            }
        }
        SFont.SoundFont sf = new SFont.SoundFont(file);
        sf.read();
        return SfSoundbank.toSoundbank(sf);
    }

    /** true for RIFF/sfbk with an ifil version major of 3 (vorbis) or 4 (flac) */
    private static boolean isCompressedSoundFont(byte[] head) {
        if (head.length < 40) {
            return false;
        }
        if (!matches(head, 0, "RIFF") || !matches(head, 8, "sfbk")) {
            return false;
        }
        // the spec requires ifil to be the first INFO sub-chunk, but scan to be safe
        for (int i = 12; i < head.length - 12; i++) {
            if (matches(head, i, "ifil")) {
                int major = (head[i + 8] & 0xff) | ((head[i + 9] & 0xff) << 8);
                return major == 3 || major == 4;
            }
        }
        return false;
    }

    private static boolean matches(byte[] data, int offset, String fourcc) {
        for (int i = 0; i < 4; i++) {
            if (data[offset + i] != (byte) fourcc.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /** read-only in-memory channel */
    private static class ByteArrayChannel implements SeekableByteChannel {

        private final byte[] data;
        private int position;
        private boolean open = true;

        ByteArrayChannel(byte[] data) {
            this.data = data;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            if (!open) throw new ClosedChannelException();
            if (position >= data.length) {
                return -1;
            }
            int n = Math.min(dst.remaining(), data.length - position);
            dst.put(data, position, n);
            position += n;
            return n;
        }

        @Override
        public int write(ByteBuffer src) {
            throw new UnsupportedOperationException("read only");
        }

        @Override
        public long position() {
            return position;
        }

        @Override
        public SeekableByteChannel position(long newPosition) {
            position = (int) newPosition;
            return this;
        }

        @Override
        public long size() {
            return data.length;
        }

        @Override
        public SeekableByteChannel truncate(long size) {
            throw new UnsupportedOperationException("read only");
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }
    }
}
