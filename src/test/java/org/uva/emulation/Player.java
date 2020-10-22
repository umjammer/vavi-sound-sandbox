/*
 * http://opl3.cozendey.com/
 */

package org.uva.emulation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import static org.uva.emulation.Player.FileType.DRO1;
import static org.uva.emulation.Player.FileType.DRO2;
import static org.uva.emulation.Player.FileType.MID;


public class Player {
    private static final Logger LOGGER = Logger.getLogger(Player.class.getName());

    private static final double frequency = 49700.0D;
    private SourceDataLine sourceDataLine;
    private Opl3Player player;
    public byte[][] musicBuffer;
    private int musicBufferLength;
    private int entireMusicLength;
    private Player.BufferStatus bufferStatus;
    private Player.FileType fileType;
    private int bufferedArrayIndex;
    private int playingArrayIndex = 0;
    private int playingSample = 0;
    private int bufferedOverallPosition;
    private boolean isThreadEnding = false;
    private boolean isWaitingBuffer;
    private int buffersWait;
    private MusicFile musicFile;

    public static void main(String[] args) {
        if (args.length < 1 || args[0] == null || args[0].isEmpty()) {
            throw new IllegalArgumentException("Missing file path for reading");
        }
        Player player = new Player(args[0]);
        player.playFirst();
//        player.playAgain();
    }

    public Player(String filePath) {
        musicFile = loadFile(new File(filePath));

        player = loadPlayer(musicFile);
    }

    public void playFirst() {
        LOGGER.info("Enter in playing first method");

        reset();
        setMusicBufferLength();
        startSourceDataLine();
        playBuffering();
//        playBuffered();
        stopSourceDataLine();
    }

    public MusicFile loadFile(File file) {
        LOGGER.info("Loading and define audio type for file=" + file);

        if (!file.exists()) {
            throw new IllegalArgumentException("Unable to find the specified file");
        }
        try {
            byte[] buffer = Files.readAllBytes(Paths.get(file.toURI()));

            switch (file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase()) {
            case "laa":
            case "cmf":
                fileType = MID;
                break;
            case "dro":
                if (buffer[10] == 1) {
                    fileType = DRO1;
                } else if (buffer[8] == 2) {
                    fileType = DRO2;
                }
                break;
            default:
                throw new IllegalStateException("Unrecognized extension file found");
            }
            return new MusicFile(file, fileType, buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Opl3Player loadPlayer(MusicFile musicFile) {
        Opl3Player cPlayer;

        switch (musicFile.getFileType()) {
        case MID:
            cPlayer = new MidPlayer();
            break;
        case DRO1:
            cPlayer = new DroPlayer(true);
            break;
        case DRO2:
            cPlayer = new Dro2Player(true);
            break;
        default:
            throw new RuntimeException("Unable to find corresponding player");
        }
        try {
            LOGGER.info("Found compatible player, loading file type=" + musicFile.getFileType());
            cPlayer.load(musicFile.getFileBuffer());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return cPlayer;
    }

    private void setMusicBufferLength() {

        musicBufferLength = 0;

        while (player.update()) {
            double sec = 1.0 / player.getRefresh();

            int len = 4 * (int) (frequency * sec);
            entireMusicLength += len;
            ++musicBufferLength;
        }

        for (int i = 0; i < 30; ++i) {
            int len = 4 * (int) (frequency * 0.1);
            entireMusicLength += len;
            ++musicBufferLength;
        }

        LOGGER.info("Exit in music buffer length method: " + entireMusicLength);
    }

    private void startSourceDataLine() {
        LOGGER.info("Enter in start source data line method: " + entireMusicLength);

        AudioFormat audioFormat = new AudioFormat((float) frequency, 16, 2, true, false);
        try {
            sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
            sourceDataLine.addLineListener(event -> {
                LOGGER.info("line: " + event.getType());
            });
            sourceDataLine.open(audioFormat);

            sourceDataLine.start();
            LOGGER.info("Starting source date line: " + sourceDataLine.getClass().getName() + ", " + sourceDataLine.isOpen() + ", " + sourceDataLine.isActive() + ", " + sourceDataLine.isRunning());
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    private void stopSourceDataLine() {
        LOGGER.info("Enter in stop source data line method");

        sourceDataLine.drain();
        sourceDataLine.stop();
        sourceDataLine.close();
    }

    private byte[] readBufferChunk(double sec) {
//        LOGGER.info("Enter in read buffer chunk method");

        int len = 4 * (int) (frequency * sec);

        byte[] buf = player.read(len);

//        ++bufferedArrayIndex;

        bufferedOverallPosition += len;
        return buf;
    }

    private synchronized void playBuffering() {
        LOGGER.info("Enter in play buffering method");

        bufferedArrayIndex = 0;
        bufferedOverallPosition = 0;
        bufferStatus = Player.BufferStatus.BUFFERING;
        isWaitingBuffer = false;
        buffersWait = 1;
//        musicBuffer = new byte[musicBufferLength][];
//        System.gc();
        player = loadPlayer(musicFile);

//        while (!sourceDataLine.isActive()) {
//            LOGGER.info("line is not running");
//        }

        while (true) {
            double sec;
            if (player.update()) {
                sec = 1.0 / player.getRefresh();

                if (!isThreadEnding && bufferStatus != Player.BufferStatus.RENDERBUFFER) {
                    byte[] buf = readBufferChunk(sec);
                    sourceDataLine.write(buf, 0, buf.length);
//                    writeAvailable();
                    continue;
                }
                return;
            }
            sec = 0.1;
            int wait = 0;

            while (true) {
                if (wait < 30) {
                    if (!isThreadEnding && bufferStatus != Player.BufferStatus.RENDERBUFFER) {
                        byte[] buf = readBufferChunk(sec);
                        sourceDataLine.write(buf, 0, buf.length);
//                        writeAvailable();
                        ++wait;
                        continue;
                    }
                    return;
                }
                if (playingArrayIndex < bufferedArrayIndex) {
                    ++playingArrayIndex;
                }
                if (bufferStatus == Player.BufferStatus.BUFFERING) {
                    bufferStatus = Player.BufferStatus.BUFFERED;
                }
                isWaitingBuffer = false;
                return;
            }
        }
    }

    private void writeAvailable() {
//        LOGGER.info("Enter in write available method");

        if (isWaitingBuffer) {
            if (bufferedArrayIndex - playingArrayIndex < buffersWait && musicBuffer[playingArrayIndex - 1] != null) {
                return;
            }
            isWaitingBuffer = false;
            buffersWait *= 2;
        }
        int length = sourceDataLine.available();
//LOGGER.info("write available: " + length);

        if (bufferedOverallPosition >= length || length >= entireMusicLength) {
            int i;

            for (i = 0; i < length && playingArrayIndex < bufferedArrayIndex; ++playingSample) {
                if (playingSample >= musicBuffer[playingArrayIndex].length) {
                    playingSample = 0;
                    ++playingArrayIndex;
                    if (playingArrayIndex >= bufferedArrayIndex) {
                        break;
                    }
                }
                ++i;
            }
            if (i < length) {
                isWaitingBuffer = true;
                if (buffersWait > musicBuffer.length - bufferedArrayIndex) {
                    buffersWait = musicBuffer.length - bufferedArrayIndex;
                }
                sourceDataLine.flush();
//LOGGER.info("flush: " + sourceDataLine.available());
            }
        }
    }

    private void playBuffered() {
        LOGGER.info("Enter play buffered method: " + playingArrayIndex + ", " + bufferedArrayIndex);

        int maxLen = 198800;

        while (playingArrayIndex < bufferedArrayIndex) {
            while (playingSample < musicBuffer[playingArrayIndex].length) {
                if (isThreadEnding || bufferStatus == Player.BufferStatus.RENDERBUFFER) {
                    return;
                }
                int len = Math.min(musicBuffer[playingArrayIndex].length - playingSample, maxLen);
//LOGGER.info("@@@ hereZ: " + playingArrayIndex + ", " + playingSample + ", " + len + " / " + sourceDataLine.available() +  "\n" + StringUtil.getDump(musicBuffer[playingArrayIndex], 128));
                sourceDataLine.write(musicBuffer[playingArrayIndex], playingSample, len);
                playingSample += len;
            }
            playingSample = 0;
            ++playingArrayIndex;
        }
        if (bufferStatus == Player.BufferStatus.PARTIALBUFFER && buffersWait <= 1024) {
            bufferStatus = Player.BufferStatus.RENDERBUFFER;
        }
    }

    private void playAgain() {
        LOGGER.info("Enter in playing again method");

        reset();
        startSourceDataLine();
        playBuffered();
        stopSourceDataLine();
    }

    private void reset() {
        LOGGER.info("Reset indexes");

        playingArrayIndex = 0;
        playingSample = 0;
        entireMusicLength = 0;
    }

    public enum FileType {
        MID,
        DRO1,
        DRO2
    }

    public enum BufferStatus {
        RENDERBUFFER,
        BUFFERING,
        BUFFERED,
        PARTIALBUFFER
    }

    private static class MusicFile {
        private final File path;
        private final FileType fileType;
        private final byte[] fileBuffer;
        public MusicFile(File path, FileType fileType, byte[] fileBuffer) {
            this.path = path;
            this.fileType = fileType;
            this.fileBuffer = fileBuffer;
        }
        public File getPath() {
            return path;
        }
        public FileType getFileType() {
            return fileType;
        }
        public byte[] getFileBuffer() {
            return fileBuffer;
        }
    }
}