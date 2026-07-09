/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.exs;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.spi.SoundbankReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import vavi.sound.exs.EXS;


/**
 * ExsSoundbankReader. reads a Logic/GarageBand EXS24 sampler instrument
 * (.exs) as a {@link Soundbank} playable by gervill.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-04 nsano initial version <br>
 * @see ExsSoundbank
 */
public class ExsSoundbankReader extends SoundbankReader {

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
        stream.mark(20);
        byte[] head = stream.readNBytes(20);
        if (!isExs(head)) {
            stream.reset();
            return null;
        }
        byte[] rest = stream.readAllBytes();
        byte[] data = new byte[head.length + rest.length];
        System.arraycopy(head, 0, data, 0, head.length);
        System.arraycopy(rest, 0, data, head.length, rest.length);
        return ExsSoundbank.getSoundbank(EXS.read(data, "EXS"), null);
    }

    @Override
    public Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException {
        Path path = file.toPath();
        byte[] data = Files.readAllBytes(path);
        if (!isExs(data)) {
            return null;
        }
        EXS exs = EXS.read(data, path.getFileName().toString());
        return ExsSoundbank.getSoundbank(exs, path.toAbsolutePath().getParent());
    }

    /** checks the magic at offset 16, both byte orders */
    private static boolean isExs(byte[] data) {
        if (data.length < 20) {
            return false;
        }
        String magic = new String(data, 16, 4, java.nio.charset.StandardCharsets.US_ASCII);
        return switch (magic) {
            case "TBOS", "JBOS", "SOBT", "SOBJ" -> true;
            default -> false;
        };
    }
}
