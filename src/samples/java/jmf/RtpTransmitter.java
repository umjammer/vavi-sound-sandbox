package jmf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;


/**
 * RtpTransmitter RTP packet transmitter class.
 *
 * @author fukugawa
 */
public class RtpTransmitter {
    // Socket
    private final DatagramSocket socket;

    // destination IP address
    private final String destIP;

    // destination UDP port number
    private final String destPort;

    // sending thread
    private TransmitThread transmitThread;

    /**
     * Generates an RTP packet transmitter that processes the audio data input from the microphone
     * into RTP packets and sends them to the specified destination IP address and
     * specified destination UDP port number using the specified socket.
     * Default media type uses G.711 u-law(0)
     *
     * @param socket socket to use
     * @param destIP destination IP address
     * @param destPort destination UDP port number
     */
    public RtpTransmitter(DatagramSocket socket, String destIP, String destPort) {
        this.socket = socket;
        this.destIP = destIP;
        this.destPort = destPort;
    }

    /**
     * Starts capturing from the microphone and start sending RTP packets to the other party.
     * The thread in this method returns immediately without blocking.
     * Capturing and sending will stop when the stop() method is called.
     */
    public void start() {
        this.transmitThread = new TransmitThread(this.socket, this.destIP, this.destPort);
        this.transmitThread.start();
    }

    /**
     * Stops capturing from the microphone and finishes sending RTP packets to the other party
     */
    public void stop() {
        this.transmitThread.transmitStop();
    }

    /**
     * Change the media type used.
     * *This method has not been implemented yet.
     */
    public void setMediaType(int mediaType) {

    }
}

/**
 * Capture -> Send thread
 */
class TransmitThread extends Thread {

    private final String destIP;

    private final String destPort;

    private final DatagramSocket socket;

    private boolean isStop;

    // constructor
    public TransmitThread(DatagramSocket socket, String destIP, String destPort) {
        this.socket = socket;
        this.destIP = destIP;
        this.destPort = destPort;
        this.isStop = false;

    }

    // Microphone capture -> Send Start thread
    @Override
    public void run() {
        try {
            byte[] voicePacket = new byte[160];
            byte[] rtpPacket = new byte[172];
            InetSocketAddress address = new InetSocketAddress(this.destIP, Integer.parseInt(this.destPort));
            DatagramPacket packet = null;

            AudioFormat linearFormat = new AudioFormat(8000, 16, 1, true, false);
            AudioFormat ulawFormat = new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 1, 1, 8000, false);

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, linearFormat);
            TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(linearFormat);
            targetDataLine.start();

            AudioInputStream linearStream = new AudioInputStream(targetDataLine);
            // convert from linear PCM 16bit 8000Hz to G.711 u-law
            AudioInputStream ulawStream = AudioSystem.getAudioInputStream(ulawFormat, linearStream);

            while (!isStop) {
                try {
                    // get G.711 u-law 20ms minutes
                    ulawStream.read(voicePacket, 0, voicePacket.length);
                    // add RTP header
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    new RtpHeader().writeTo(baos);
                    System.arraycopy(voicePacket, 0, baos.toByteArray(), 0, 12);
                    System.arraycopy(voicePacket, 0, rtpPacket, 12, 160);
                    packet = new DatagramPacket(rtpPacket, rtpPacket.length, address);
                    // send to other party
                    this.socket.send(packet);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
            targetDataLine.stop();
            targetDataLine.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Microphone capture -> Start transmission Stop thread
    public void transmitStop() {
        this.isStop = true;
    }

    static class RtpHeader {
        // Init RTP Headerstop
        RtpHeader() {
            Random r = new Random();
            this.sequenceNum = 0;
            this.timeStamp = 0;
            this.syncSourceId = r.nextInt();
            this.marker = -128;
        }

        // sequence number
        private short sequenceNum;
        // time stamp
        private int timeStamp;
        // sync source ID
        private final int syncSourceId;
        // marker bit
        private byte marker;

        // version number 10000000
        final byte version = -128;
        // padding
        final byte padding = 0;
        // expansion bit
        final byte extention = 0;
        // contribution count
        final byte contribute = 0;
        // payload type
        final byte payload = 0;

        void writeTo(OutputStream os) throws IOException {
            // RTP header
            byte[] rtpHeader = new byte[12];

            // generating RTP headers
            rtpHeader[0] = (byte) (version | padding | extention | contribute);
            rtpHeader[1] = (byte) (marker | payload);
            rtpHeader[2] = (byte) (this.sequenceNum >> 8);
            rtpHeader[3] = (byte) (this.sequenceNum >> 0);
            rtpHeader[4] = (byte) (this.timeStamp >> 24);
            rtpHeader[5] = (byte) (this.timeStamp >> 16);
            rtpHeader[6] = (byte) (this.timeStamp >> 8);
            rtpHeader[7] = (byte) (this.timeStamp >> 0);
            rtpHeader[8] = (byte) (this.syncSourceId >> 24);
            rtpHeader[9] = (byte) (this.syncSourceId >> 16);
            rtpHeader[10] = (byte) (this.syncSourceId >> 8);
            rtpHeader[11] = (byte) (this.syncSourceId >> 0);

            // sequence number, timestamp, marker bit migration
            this.sequenceNum++;
            this.timeStamp += 160;
            if (this.marker == -128) {
                this.marker = 0;
            }

            // RTP header + audio data = RTP packet
            ByteArrayOutputStream baos = new ByteArrayOutputStream(172);
            baos.write(rtpHeader, 0, 12);
            baos.writeTo(os);
        }
    }
}
