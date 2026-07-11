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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import vavi.sound.sf.SFZ;


/**
 * SfzSoundbankReader. reads an SFZ sound font text definition (.sfz) as a
 * {@link Soundbank} playable by Gervill.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-09 nsano initial version <br>
 * @see SfzSoundbank
 */
public class SfzSoundbankReader extends SoundbankReader {

    private static final int SNIFF_LENGTH = 1024;

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
        if (!isSfz(head)) {
            stream.reset();
            return null;
        }
        byte[] rest = stream.readAllBytes();
        byte[] data = new byte[head.length + rest.length];
        System.arraycopy(head, 0, data, 0, head.length);
        System.arraycopy(rest, 0, data, head.length, rest.length);

        Path tempFile = Files.createTempFile("sfz-", ".sfz");
        try {
            Files.write(tempFile, data);
            SFZ sfz = SFZ.read(tempFile);
            return SfzSoundbank.getSoundbank(sfz, null);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException {
        byte[] head;
        try (InputStream is = Files.newInputStream(file.toPath())) {
            head = is.readNBytes(SNIFF_LENGTH);
        }
        if (!isSfz(head)) {
            return null;
        }
        SFZ sfz = SFZ.read(file.toPath());
        return SfzSoundbank.getSoundbank(sfz, file.toPath().getParent());
    }

    private static boolean isSfz(byte[] head) {
        if (head.length == 0) {
            return false;
        }
        String text = new String(head, StandardCharsets.UTF_8).toLowerCase();
        return text.contains("<region>") || text.contains("<group>") || text.contains("<control>") || text.contains("<global>") || text.contains("#include");
    }
}
