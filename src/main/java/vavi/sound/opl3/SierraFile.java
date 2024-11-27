/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.opl3;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import vavi.sound.midi.opl3.Opl3Soundbank;
import vavi.sound.midi.opl3.Opl3Soundbank.Opl3Instrument;
import vavi.sound.midi.opl3.Opl3Synthesizer.Context;
import vavi.sound.opl3.MidPlayer.MidiTypeFile;

import static java.lang.System.getLogger;


/**
 * SierraFile (SCI).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/25 umjammer initial version <br>
 */
class SierraFile extends MidiTypeFile {

    private static final Logger logger = getLogger(SierraFile.class.getName());

    /** for patch file location */
    protected URI uri;

    @Override
    int markSize() {
        return 3;
    }

    @Override
    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        return dis.readUnsignedByte() == 0x84 &&
                dis.readUnsignedByte() == 0 &&
                dis.readUnsignedByte() != 0xf0; // not advanced sierra
    }

    /** convert byte buffer to int buffer for an instrument */
    private static int[] fromSierra(int[] buf) {
        int[] x = new int[11];
        x[0] = buf[9] * 0x80 + buf[10] * 0x40 + buf[5] * 0x20 + buf[11] * 0x10 + buf[1];
        x[1] = buf[22] * 0x80 + buf[23] * 0x40 + buf[18] * 0x20 + buf[24] * 0x10 + buf[14];
        x[2] = (buf[0] << 6) + buf[8];
        x[3] = (buf[13] << 6) + buf[21];
        x[4] = (buf[3] << 4) + buf[6];
        x[5] = (buf[16] << 4) + buf[19];
        x[6] = (buf[4] << 4) + buf[7];
        x[7] = (buf[17] << 4) + buf[20];
        x[8] = buf[26];
        x[9] = buf[27];
        x[10] = (buf[2] << 1) + (1 - (buf[12] & 1));
        return x;
    }

    /** load patch (.003) file */
    protected void loadSierraIns(URI uri) throws IOException {

        URL url;
        if (uri.getPath().contains("..") || uri.getScheme() == null || "file".equals(uri.getScheme())) {
            url = Path.of(uri.getPath()).toAbsolutePath().toUri().toURL();
        } else {
            url = uri.toURL();
        }

        String filename = URLDecoder.decode(url.toExternalForm(), StandardCharsets.UTF_8);
        filename = filename.substring(filename.lastIndexOf('/') + 1);

        String path = url.getFile();
        String patchFileName = path.substring(0, path.lastIndexOf('/')) + '/' + filename.substring(0, 3) + "patch.003";
        URL patchFileURL;
        try {
            patchFileURL = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), patchFileName, url.getQuery(), url.getRef()).toURL();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

logger.log(Level.DEBUG, "patch: " + patchFileURL);
        DataInputStream dis = new DataInputStream(patchFileURL.openStream());
        dis.skipBytes(2);
        stins = 0;

        for (int j = 0; j < 2; ++j) {
            for (int k = 0; k < 48; ++k) {
                int p = j * 48 + k;
logger.log(Level.TRACE, "%2d: ".formatted(p));

                int[] buf = new int[28];

                for (int i = 0; i < 28; ++i) {
                    buf[i] = dis.readUnsignedByte();
                }

                smyinsbank[p] = Opl3Soundbank.newInstrument(0, p, "sierra." + p, fromSierra(buf));
logger.log(Level.TRACE, "instrument[" + p + "]: " + smyinsbank[p]);

                ++stins;
            }

            dis.skipBytes(2);
        }

// TODO fill null for avoiding npe
for (int i = 96; i < 128; i++) {
 smyinsbank[i] = smyinsbank[0];
}
        dis.close();
    }

    protected final Opl3Instrument[] smyinsbank = new Opl3Instrument[128];
    // sierra instruments
    protected int stins;

    private final boolean[] ons = new boolean[16];
    private final int[] inums = new int[16];

    @Override
    void rewind(int subSong, MidPlayer player) throws IOException {
        this.uri = (URI) player.getProperties().get("uri");
logger.log(Level.DEBUG, "uri: " + uri);
        if (uri != null) loadSierraIns(uri);

        player.tins = stins;
        player.takeBE(3);
        player.deltas = 32;
        player.tracks[0].on = true;
        player.tracks[0].tend = player.flen; // music until the end of the file

        for (int c = 0; c < 16; ++c) {
            this.ons[c] = player.takeBE(1) != 0;
            this.inums[c] = player.takeBE(1);
        }

        player.tracks[0].spos = player.pos;
    }

    protected Context context;

    @Override
    public void init(Context context) {
        this.context = context;

        if (smyinsbank[0] != null) {
            context.instruments(smyinsbank);
        }

        for (int c = 0; c < 16; ++c) {
            context.channels()[c].nshift = -13;
            context.voiceStatus()[c].active = this.ons[c];
            context.channels()[c].inum = this.inums[c];

            context.channels()[c].setIns(context.instruments()[context.channels()[c].inum]);
        }

        context.adlib().style = Adlib.SIERRA_STYLE | Adlib.MIDI_STYLE;
    }

    @Override
    public int nativeVelocity(int channel, int velocity) {
//        if ((adlib.style & Adlib.MIDI_STYLE) != 0) {
        int nv = (context.voiceStatus()[channel].volume * velocity) / 128;

        if (nv > 127) {
            nv = 127;
        }

        nv = Adlib.my_midi_fm_vol_table[nv];
        return nv;
//        }
    }
}
