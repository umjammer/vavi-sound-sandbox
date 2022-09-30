/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm;

import java.io.DataInputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.tritonus.sampled.file.WaveAudioFileReader;
import org.tritonus.sampled.file.WaveTool;
import vavi.sound.sampled.adpcm.ccitt.CcittEncoding;
import vavi.sound.sampled.adpcm.dvi.DviEncoding;
import vavi.sound.sampled.adpcm.ima.ImaEncoding;
import vavi.sound.sampled.adpcm.ms.MsEncoding;
import vavi.sound.sampled.adpcm.oki.OkiEncoding;
import vavi.sound.sampled.adpcm.yamaha.YamahaEncoding;


/**
 * AdpcmWaveAudioFileReader.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-21 nsano initial version <br>
 */
public class AdpcmWaveAudioFileReader extends WaveAudioFileReader {

    protected AudioFormat readFormatChunk(DataInputStream dis,
                                          long chunkLength) throws UnsupportedAudioFileException, IOException {

        int read = WaveTool.MIN_FMT_CHUNK_LENGTH;

        if (chunkLength < WaveTool.MIN_FMT_CHUNK_LENGTH) {
            throw new UnsupportedAudioFileException("corrupt WAVE file: format chunk is too small");
        }

        short formatCode = readLittleEndianShort(dis);
        short channelCount = readLittleEndianShort(dis);
        if (channelCount <= 0) {
            throw new UnsupportedAudioFileException("corrupt WAVE file: number of channels must be positive");
        }

        int sampleRate = readLittleEndianInt(dis);
        if (sampleRate <= 0) {
            throw new UnsupportedAudioFileException("corrupt WAVE file: sample rate must be positive");
        }

        int avgBytesPerSecond = readLittleEndianInt(dis);
        int blockAlign = readLittleEndianShort(dis);

        AudioFormat.Encoding encoding;
        int sampleSizeInBits = 0;
        int frameSize = 0;
        float frameRate = sampleRate;

        int cbSize;
        switch (formatCode) {
        case 2: // MS ADPCM
            // TODO
            encoding = MsEncoding.MS;
            break;

        case 10: // OKI ADPCM
            // TODO
            encoding = OkiEncoding.OKI;
            break;

        case 11: // Intel's DVI ADPCM
            // TODO
            encoding = DviEncoding.DVI;
            break;

        case 14: // G.723 ADPCM
            // TODO
            encoding = CcittEncoding.G721;
            break;

        case WaveTool.WAVE_FORMAT_IMA_ADPCM:
            if (chunkLength < WaveTool.MIN_FMT_CHUNK_LENGTH + 2) {
                throw new UnsupportedAudioFileException("corrupt WAVE file: extra GSM bytes are missing");
            }
            sampleSizeInBits = readLittleEndianShort(dis);
            cbSize = readLittleEndianShort(dis);
            if (cbSize < 2) {
                throw new UnsupportedAudioFileException("corrupt WAVE file: extra IMA ADPCM bytes are corrupt");
            }
            int samplesPerBlock = readLittleEndianShort(dis) & 0xFFFF; // unsigned
            sampleSizeInBits = AudioSystem.NOT_SPECIFIED;
            encoding = ImaEncoding.IMA;
            frameSize = blockAlign;
            frameRate = ((float) sampleRate) / ((float) samplesPerBlock);
            read += 6;
            break;

        case 20: // YAMAHA ADPCM
            // TODO
            encoding = YamahaEncoding.YAMAHA;
            break;

        default:
            throw new UnsupportedAudioFileException("unsupported WAVE file: unknown format code " + formatCode);
        }
        // if frameSize isn't set, calculate it (the default)
        if (frameSize == 0) {
            frameSize = calculateFrameSize(sampleSizeInBits, channelCount);
        }

        // go to next chunk
        advanceChunk(dis, chunkLength, read);
        return new AudioFormat(
                encoding,
                sampleRate,
                sampleSizeInBits,
                channelCount,
                frameSize,
                frameRate,
                false);
    }
}
