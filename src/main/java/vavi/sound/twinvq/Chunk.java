/*
 * Copyright 1996-2000 (c) NTT Cyber Space Laboratories
 *
 * Modified on 2000.09.06 by N. Iwakami
 */

package vavi.sound.twinvq;

import java.util.List;


/**
 * �`�����N�̊�{�\����񋟂���B���̃f�[�^����o�͂���
 */
class Chunk {

    private String id;

    private List<Byte> data;

    private int pos;

    /**
     * ���݈ʒu���� size �o�C�g�������擾
     * @param size �f�t�H���g�Flong����
     */
    protected long getNInt(int size/* =sizeof(long) */) {
        final int mask = 0xff;
        int ibyte;
        long retval;

        retval = 0;
        for (ibyte = 0; ibyte < size; ibyte++) {
            retval <<= 8;
            retval |= data.get(pos++) & mask;
        }

        return retval;
    }

    /**
     * ���݈ʒu���� size �o�C�g���̃x�N�g�����擾
     * @param size �f�t�H���g�F�c��S��
     */
    protected byte[] getVector(int size/* = 0 */) {

        if (size == 0) {
            size = data.size() - pos;
        }

        byte[] retval = new byte[size];

        if (pos + size > data.size()) {
            return retval;
        }

        for (int ii = 0; ii < size; ii++) {
            retval[ii] = data.get(pos++);
        }
        return retval;
    }

    /**
     * ���݈ʒu���� length �����̕�������擾
     * @param length �f�t�H���g�F�c��S��
     */
    protected String getRndString(int length/* = 0 */) {
        if (length == 0) {
            length = getSize();
        }

        if (pos + length > data.size()) {
            return "";
        }

        StringBuilder theString = new StringBuilder();
        for (int ii = 0; ii < length; ii++) {
            theString.append(data.get(pos++));
        }

        return theString.toString();
    }

    /**
     * ���݈ʒu���� size �o�C�g��������������
     * 
     * @param size �f�t�H���g�Flong����
     */
    protected void putNInt(int inputData, int size/* =sizeof(long) */) {
        final int mask = 0xff;
        int ibyte;
        byte data_tmp;

        if (size > 4) {
            throw new FailPutException();
        }

        for (ibyte = 0; ibyte < size; ibyte++) {
            data_tmp = (byte) ((inputData >> (8 * (size - ibyte - 1))) & mask);
            data.add(data_tmp);
        }
    }

    /** ���݈ʒu��擪�ɖ߂� */
    protected void rewindChunk() {
        pos = 0;
    }

    /** �f�[�^���������� */
    protected void deleteChunk() {
        data.clear();
        rewindChunk();
    }

    /** ���݈ʒu���擾���� */
    protected final int getCurrentPosition() {
        return pos;
    }

    /** ��O */
    protected class FailPutException extends RuntimeException {
    }

    /** �R���X�g���N�^ */
    public Chunk(Chunk parent) {
        id = parent.id;
        pos = 0;
    }

    /** �R���X�g���N�^ */
    public Chunk(String ID) {
        id = ID;
        pos = 0;
    }

    /** ID �̎擾 */
    public final String getID() {
        return id;
    }

    /** �`�����N�T�C�Y�̎擾 */
    public final int getSize() {
        return data.size();
    }

    public final byte[] getData() {
        byte[] retval = new byte[data.size()];
        for (int ii = 0; ii < retval.length; ii++) {
            retval[ii] = data.get(ii);
        }
        return retval;
    }

    /** �f�[�^��ǉ��A�L�����N�^�z��^ */
    public int putData(int size, byte[] inputData) {
        for (int ii = 0; ii < size; ii++) {
            data.add(inputData[ii]);
        }

        return 0;
    }

    /** �f�[�^��ǉ��A�x�N�g���^ */
    public int putData(byte[] inputData) {
        for (int ii = 0; ii < inputData.length; ii++) {
            data.add(inputData[ii]);
        }
        return 0;
    }

    /** �f�[�^��ǉ��A������^ */
    public int putData(String theString) {

        for (byte it : theString.getBytes()) {
            data.add(it);
        }

        return 0;
    }

    public String toString() {
        return "Raw";
    }
}

/**
 * ������݂̂��i�[����`�����N�A�ėp�`�����N�^
 */
class StringChunk extends Chunk {
    public String getString() {
        rewindChunk();
        return getRndString(0);
    }

    public StringChunk(final Chunk parent) {
        super(parent);
    }

    public StringChunk(String ID, String data/* ="" */) {
        super(ID);
        if (data != "")
            putData(data);
    }

    public String toString() {
        return "String";
    }
}

/**
 * �������P�����i�[����`�����N�^�̃e���v���[�g�A�ėp�`�����N�^
 */
class IntChunk extends Chunk {
    int m_dataSize;

    // �����f�[�^���擾����
    public final long getInt() {
        rewindChunk();
        return getNInt(m_dataSize);
    }

    public void putInt(long data) {
        deleteChunk();
        putNInt((int) data, m_dataSize);
    }

    public IntChunk(final Chunk parent) {
        super(parent);
        m_dataSize = 8;
    }

    public IntChunk(String id, long data) {
        super(id);
        m_dataSize = 8;
        putNInt((int) data, m_dataSize);
    }

    public String toString() {
        return String.format("Integer, size=%d", m_dataSize);
    }
}

/**
 * �`�����N���i�[����`�����N�A�ėp�`�����N�^
 */
class ChunkChunk extends Chunk {
    /** ���̃`�����N��Ԃ� */
    public Chunk GetNextChunk(int idSize/* = 4 */) {
        String id;
        if ((id = this.getRndString(idSize)) != "") {
            Chunk subChunk = new Chunk(id);

            int size = (int) this.getNInt(8);
            if (size > 0) {
                byte[] theData;
                theData = this.getVector(size);
                if (theData.length == 0)
                    throw new FailGetChunkException();
                subChunk.putData(theData);
            }
            return subChunk;
        }

        return null;
    }

    /** �����߂� */
    public void rewind() {
        rewindChunk();
    }

    public void putChunk(Chunk src) {
        String id = src.getID();
        putData(id);

        putNInt(src.getSize(), 8);

        byte[] data = src.getData();
        putData(data);
    }

    /** �I�u�W�F�N�g����ɂ��� */
    public ChunkChunk(final Chunk parent) {
        super(parent);
    }

    /**
     * ID �����^����̃`�����N���쐬����
     */
    public ChunkChunk(String id) {
        super(id);
    }

    /** �`�����N�擾�Ɏ��s */
    public class FailGetChunkException extends RuntimeException {
    }

    /** �`�����N�������݂Ɏ��s */
    public class FailPutChunkException extends Exception {
    }

    public String toString() {
        return "Chunk";
    }
}

/**
 * CommChunk
 */
class CommChunk extends Chunk {

    private String version;

    private int channelMode;

    private int bitRate;

    private int samplingRate;

    private int securityLevel;

    /**
     * 
     * @param parent
     * @param version ="TWIN97012000"
     */
    public CommChunk(final Chunk parent, String version) {
        super(parent);
        this.version = version;

        this.rewindChunk();
        this.channelMode = (int) this.getNInt(8);
        this.bitRate = (int) this.getNInt(8);
        this.samplingRate = (int) this.getNInt(8);
        this.securityLevel = (int) this.getNInt(8);
    }

    public CommChunk(int channelMode, int bitRate, int samplingRate, int securityLevel, String version/* ="TWIN97012000" */) {
        super("COMM");
        this.version = version;

        this.channelMode = channelMode;
        this.bitRate = bitRate;
        this.samplingRate = samplingRate;
        this.securityLevel = securityLevel;

        this.rewindChunk();
        this.putNInt(channelMode, 8);
        this.putNInt(bitRate, 8);
        this.putNInt(samplingRate, 8);
        this.putNInt(securityLevel, 8);

    }

    /** �`���l�����[�h���擾 */
    public int getChannelMode() {
        return channelMode;
    }

    /** �r�b�g���[�g���擾 */
    public int getBitRate() {
        return bitRate;
    }

    /** �T���v�����O���g�����擾 */
    public int getSamplingRate() {
        return samplingRate;
    }

    /** �ǉ������擾 */
    public int getSecurityLevel() {
        return securityLevel;
    }

    /** */
    public String getVersion() {
        return version;
    }

    /** �R���X�g���N�V�����Ɏ��s */
    class FailConstructionException extends Exception {
    }

    public String toString() {
        return "COMM";
    }
}

/**
 * YearChunk
 */
class YearChunk extends Chunk {
    int year;

    int month;

    public final int getYear() {
        return year;
    }

    public final int getMonth() {
        return month;
    }

    public YearChunk(String id, short year, char month) {
        super(id);
        this.year = year;
        this.month = month;
        putNInt(year, 2);
        putNInt(month, 1);
    }

    public YearChunk(final Chunk parent) {
        super(parent);
        rewindChunk();
        year = (int) getNInt(2);
        month = (int) getNInt(1);
    }

    public String toString() {
        return "YEAR";
    }
}

/**
 * EncdChunk
 */
class EncdChunk extends Chunk {
    int year;

    int month;

    int day;

    int hour;

    int minute;

    int timeZone;

    public final int getYear() {
        return year;
    }

    final int getMonth() {
        return month;
    }

    final int getDay() {
        return day;
    }

    final int getHour() {
        return hour;
    }

    final int getMinute() {
        return minute;
    }

    final int getTimeZone() {
        return timeZone;
    }

    EncdChunk(String id, int year, int month, int day, int hour, int minute, int timeZone) {
        super(id);
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.timeZone = timeZone;

        putNInt(year, 2);
        putNInt(month, 1);
        putNInt(day, 1);
        putNInt(hour, 1);
        putNInt(minute, 1);
        putNInt(timeZone, 1);
    }

    EncdChunk(final Chunk parent) {
        super(parent);
        rewindChunk();
        year = (int) getNInt(2);
        month = (int) getNInt(1);
        day = (int) getNInt(1);
        hour = (int) getNInt(1);
        minute = (int) getNInt(1);
        timeZone = (int) getNInt(1);
    }

    public String toString() {
        return "ENCD";
    }
}

// ///////////////////////////////////////////////////////
// �ėp�`�����N�^�𗘗p����T�u�`�����N�^�̐錾
// ///////////////////////////////////////////////////////
// TWIN �`�����N
// typedef CChunkChunk CTwinChunk; // TWIN
//
// // �W���`�����N
// // COMM �͔ėp�`�����N�ł͂Ȃ�
// typedef CStringChunk CNameChunk; // NAME
// typedef CStringChunk CComtChunk; // COMT
// typedef CStringChunk CAuthChunk; // AUTH
// typedef CStringChunk CCpyrChunk; // (c)
// typedef CStringChunk CFileChunk; // FILE
// typedef CIntChunk<unsigned long> CDsizChunk; // DSIZ
// typedef CChunk CExtrChunk; // EXTR
//
// // �g���`�����N�E�ʏ�
// typedef CStringChunk CAlbmChunk; // ALBM
// // YEAR �͔ėp�`�����N�ł͂Ȃ�
// // ENCD �͔ėp�`�����N�ł͂Ȃ�
// typedef CIntChunk<short> CTracChunk; // TRAC
// typedef CStringChunk CLyrcChunk; // LYRC
// typedef CChunk CGuidChunk; // GUID
// typedef CStringChunk CIsrcChunk; // ISRC
// typedef CStringChunk CWordChunk; // WORD
// typedef CStringChunk CMuscChunk; // MUSC
// typedef CStringChunk CArngChunk; // ARNG
// typedef CStringChunk CProdChunk; // PROD
// typedef CStringChunk CRemxChunk; // REMX
// typedef CStringChunk CCdctChunk; // CDCT
// typedef CStringChunk CSingChunk; // SING
// typedef CStringChunk CBandChunk; // BAND
// typedef CStringChunk CPrsnChunk; // PRSN
// typedef CStringChunk CLablChunk; // LABL
// typedef CStringChunk CNoteChunk; // NOTE
//
// // �g���`�����N�E�⏕
// typedef CChunkChunk CScndChunk; // SCND
//
// // �\��`�����N
// typedef CChunk C_Id3Chunk; // _ID3
// typedef CChunk C_YmhChunk; // _YMH
// typedef CChunk C_NttChunk; // _NTT
