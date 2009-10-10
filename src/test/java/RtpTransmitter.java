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
 * RtpTransmitter RTP�p�P�b�g�̑��M�@�N���X
 * 
 * @author fukugawa
 */
public class RtpTransmitter {
    // Socket
    private DatagramSocket socket;

    // ����IP�A�h���X
    private String destIP;

    // ����UDP�|�[�g�ԍ�
    private String destPort;

    // ���M�X���b�h
    private TransmitThread transmitThread;

    /**
     * �}�C�N������͂��ꂽ�����f�[�^���ARTP�p�P�b�g�ɉ��H���A �w�肵���\�P�b�g���g�p���āA�w�肵������IP�A�h���X�A�w�肵������UDP�|�[�g�ԍ�
     * �֑��M����RTP�p�P�b�g���M�@�𐶐����܂� �f�B�t�H���g�̃��f�B�A�^�C�v�́AG.711 u-law(0)���g�p���܂�
     * 
     * @param socket �g�p����Socket
     * @param destIP ����IP�A�h���X
     * @param destPort ����UDP�|�[�g�ԍ�
     */
    public RtpTransmitter(DatagramSocket socket, String destIP, String destPort) {
        this.socket = socket;
        this.destIP = destIP;
        this.destPort = destPort;
    }

    /**
     * �}�C�N����L���v�`���[���J�n���A�����RTP�p�P�b�g�𑗐M���n�߂܂� ���̃��\�b�h�̃X���b�h�̓u���b�N�����ɂ����ɐ����Ԃ��܂�
     * �L���v�`���[�E���M��stop()���\�b�h���Ă΂��ƒ�~���܂�
     */
    public void start() {
        this.transmitThread = new TransmitThread(this.socket, this.destIP, this.destPort);
        this.transmitThread.start();
    }

    /**
     * �}�C�N����̃L���v�`���[���~���A����ւ�RTP�p�P�b�g�̑��M���I���܂�
     */
    public void stop() {
        this.transmitThread.transmitStop();
    }

    /**
     * �g�p���郁�f�B�A�^�C�v��ύX���܂� �����̃��\�b�h�͂܂���������Ă��܂���
     */
    public void setMediaType(int mediaType) {

    }
}

/**
 * �L���v�`���[ -> ���M�X���b�h
 */
class TransmitThread extends Thread {

    private String destIP;

    private String destPort;

    private DatagramSocket socket;

    private boolean isStop;

    // �R���X�g���N�^
    public TransmitThread(DatagramSocket socket, String destIP, String destPort) {
        this.socket = socket;
        this.destIP = destIP;
        this.destPort = destPort;
        this.isStop = false;

    }

    // �}�C�N�L���v�`���[ -> ���M �X���b�h�J�n
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
            // ���j�APCM 16bit 8000Hz ���� G.711 u-law�֕ϊ�
            AudioInputStream ulawStream = AudioSystem.getAudioInputStream(ulawFormat, linearStream);

            while (!isStop) {
                try {
                    // G.711 u-law 20ms�����擾����
                    ulawStream.read(voicePacket, 0, voicePacket.length);
                    // RTP�w�b�_�[��t����
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    new RtpHeader().writeTo(baos);
                    System.arraycopy(voicePacket, 0, baos.toByteArray(), 0, 12);
                    System.arraycopy(voicePacket, 0, rtpPacket, 12, 160);
                    packet = new DatagramPacket(rtpPacket, rtpPacket.length, address);
                    // ����֑��M
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

    // �}�C�N�L���v�`���[ -> ���M�J�n �X���b�h��~
    public void transmitStop() {
        this.isStop = true;
    }

    class RtpHeader {
        // Init RTP Headerstop
        RtpHeader() {
            Random r = new Random();
            this.sequenceNum = 0;
            this.timeStamp = 0;
            this.syncSourceId = r.nextInt();
            this.marker = -128;
        }

        // �V�[�P���X�ԍ�
        private short sequenceNum;
        // �^�C���X�^���v
        private int timeStamp;
        // �����\�[�XID
        private int syncSourceId;
        // �}�[�J�[�r�b�g
        private byte marker;

        // �o�[�W�����ԍ�10000000
        byte version = -128;
        // �p�f�B���O
        byte padding = 0;
        // �g���r�b�g
        byte extention = 0;
        // �R���g���r���[�g�J�E���g
        byte contribute = 0;
        // �y�C���[�h�^�C�v
        byte payload = 0;

        void writeTo(OutputStream os) throws IOException {
            // RTP�w�b�_
            byte[] rtpHeader = new byte[12];
            
            // RTP�w�b�_�[�̐���
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

            // �V�[�P���X�ԍ��A�^�C���X�^���v�A�}�[�J�[�r�b�g�ڍs
            this.sequenceNum++;
            this.timeStamp += 160;
            if (this.marker == -128) {
                this.marker = 0;
            }

            // RTP�w�b�_�[�{�����f�[�^ = RTP�p�P�b�g
            ByteArrayOutputStream baos = new ByteArrayOutputStream(172);
            baos.write(rtpHeader, 0, 12);
            baos.writeTo(os);
        }
    }
}

/* */
