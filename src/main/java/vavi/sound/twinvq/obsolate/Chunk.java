/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq.obsolate;

import java.util.List;


/**
 * Provides the basic structure of chunks. Input and output raw data.
 */
class Chunk {

    private final String id;

    private List<Byte> data;

    private int pos;

    /**
     * Gets size byte integer from current position.
     * @param size default: long integer
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
     * Gets a vector of size bytes from the current position.
     * @param size default: all the rest
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
     * Gets length string from current position.
     * @param length default: all the rest
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
     * Writes size byte integer from current position.
     * 
     * @param size default: long integer
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

    /** Returns current position to the beginning. */
    protected void rewindChunk() {
        pos = 0;
    }

    /** Erases data. */
    protected void deleteChunk() {
        data.clear();
        rewindChunk();
    }

    /** Get current position. */
    protected final int getCurrentPosition() {
        return pos;
    }

    /** exception */
    protected static class FailPutException extends RuntimeException {
    }

    /** constructor */
    public Chunk(Chunk parent) {
        id = parent.id;
        pos = 0;
    }

    /** constructor */
    public Chunk(String ID) {
        id = ID;
        pos = 0;
    }

    /** Gets ID. */
    public final String getID() {
        return id;
    }

    /** Gets chunk size. */
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

    /** Adds data, character array type. */
    public int putData(int size, byte[] inputData) {
        for (int ii = 0; ii < size; ii++) {
            data.add(inputData[ii]);
        }

        return 0;
    }

    /** Adds data, vector type. */
    public int putData(byte[] inputData) {
        for (byte inputDatum : inputData) {
            data.add(inputDatum);
        }
        return 0;
    }

    /** Adds data, string type. */
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
 * Chunk that stores only strings, general-purpose chunk type.
 */
class StringChunk extends Chunk {
    public String getString() {
        rewindChunk();
        return getRndString(0);
    }

    public StringChunk(Chunk parent) {
        super(parent);
    }

    public StringChunk(String ID, String data/* ="" */) {
        super(ID);
        if (!data.isEmpty())
            putData(data);
    }

    public String toString() {
        return "String";
    }
}

/**
 * Chunk type template that stores only one integer, general purpose chunk type.
 */
class IntChunk extends Chunk {
    int m_dataSize;

    // Gets integer data.
    public final long getInt() {
        rewindChunk();
        return getNInt(m_dataSize);
    }

    public void putInt(long data) {
        deleteChunk();
        putNInt((int) data, m_dataSize);
    }

    public IntChunk(Chunk parent) {
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
 * Chunk that stores chunks, general-purpose chunk type.
 */
class ChunkChunk extends Chunk {
    /** Returns next chunk. */
    public Chunk GetNextChunk(int idSize/* = 4 */) {
        String id;
        if (!(id = this.getRndString(idSize)).isEmpty()) {
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

    /** Rewinds. */
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

    /** Seeds an object. */
    public ChunkChunk(Chunk parent) {
        super(parent);
    }

    /**
     * Creates an empty chunk given just the ID.
     */
    public ChunkChunk(String id) {
        super(id);
    }

    /** Failed to get chunk. */
    public static class FailGetChunkException extends RuntimeException {
    }

    /** Chunk write failed. */
    public static class FailPutChunkException extends Exception {
    }

    public String toString() {
        return "Chunk";
    }
}

/**
 * CommChunk
 */
class CommChunk extends Chunk {

    private final String version;

    private final int channelMode;

    private final int bitRate;

    private final int samplingRate;

    private final int securityLevel;

    /**
     * 
     * @param parent
     * @param version ="TWIN97012000"
     */
    public CommChunk(Chunk parent, String version) {
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

    /** Gets channel mode. */
    public int getChannelMode() {
        return channelMode;
    }

    /** Gets bitrate. */
    public int getBitRate() {
        return bitRate;
    }

    /** Gets sampling frequency. */
    public int getSamplingRate() {
        return samplingRate;
    }

    /** Gets additional information. */
    public int getSecurityLevel() {
        return securityLevel;
    }

    /** */
    public String getVersion() {
        return version;
    }

    /** Construction failed. */
    static class FailConstructionException extends Exception {
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

    public YearChunk(Chunk parent) {
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

    EncdChunk(Chunk parent) {
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
// Declaration of a subchunk type that uses a generic chunk type
// ///////////////////////////////////////////////////////
// TWIN Chunk
// typedef CChunkChunk CTwinChunk; // TWIN
//
// // Normal Chunk
// // COMM is not a normal chunk
// typedef CStringChunk CNameChunk; // NAME
// typedef CStringChunk CComtChunk; // COMT
// typedef CStringChunk CAuthChunk; // AUTH
// typedef CStringChunk CCpyrChunk; // (c)
// typedef CStringChunk CFileChunk; // FILE
// typedef CIntChunk<unsigned long> CDsizChunk; // DSIZ
// typedef CChunk CExtrChunk; // EXTR
//
// // Extended Chunk/Normal
// typedef CStringChunk CAlbmChunk; // ALBM
// // YEAR is not a normal chunk
// // ENCD is not a normal chunk
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
// // Extended Chunk/Auxiliary
// typedef CChunkChunk CScndChunk; // SCND
//
// // Reserved Chunk
// typedef CChunk C_Id3Chunk; // _ID3
// typedef CChunk C_YmhChunk; // _YMH
// typedef CChunk C_NttChunk; // _NTT
