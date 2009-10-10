/*
 * Copyright 1996-2000 (c) NTT Cyber Space Laboratories
 *
 * Modified on 2000.09.06 by N. Iwakami
 */

package vavi.sound.twinvq;

import java.util.List;


/**
 * チャンクの基本構造を提供する。生のデータを入出力する
 */
class Chunk {

    private String id;

    private List<Byte> data;

    private int pos;

    /**
     * 現在位置から size バイト整数を取得
     * @param size デフォルト：long整数
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
     * 現在位置から size バイト分のベクトルを取得
     * @param size デフォルト：残り全部
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
     * 現在位置から length だけの文字列を取得
     * @param length デフォルト：残り全部
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
     * 現在位置から size バイト整数を書き込み
     * 
     * @param size デフォルト：long整数
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

    /** 現在位置を先頭に戻す */
    protected void rewindChunk() {
        pos = 0;
    }

    /** データを消去する */
    protected void deleteChunk() {
        data.clear();
        rewindChunk();
    }

    /** 現在位置を取得する */
    protected final int getCurrentPosition() {
        return pos;
    }

    /** 例外 */
    protected class FailPutException extends RuntimeException {
    }

    /** コンストラクタ */
    public Chunk(Chunk parent) {
        id = parent.id;
        pos = 0;
    }

    /** コンストラクタ */
    public Chunk(String ID) {
        id = ID;
        pos = 0;
    }

    /** ID の取得 */
    public final String getID() {
        return id;
    }

    /** チャンクサイズの取得 */
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

    /** データを追加、キャラクタ配列型 */
    public int putData(int size, byte[] inputData) {
        for (int ii = 0; ii < size; ii++) {
            data.add(inputData[ii]);
        }

        return 0;
    }

    /** データを追加、ベクトル型 */
    public int putData(byte[] inputData) {
        for (int ii = 0; ii < inputData.length; ii++) {
            data.add(inputData[ii]);
        }
        return 0;
    }

    /** データを追加、文字列型 */
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
 * 文字列のみを格納するチャンク、汎用チャンク型
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
 * 整数を１つだけ格納するチャンク型のテンプレート、汎用チャンク型
 */
class IntChunk extends Chunk {
    int m_dataSize;

    // 整数データを取得する
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
 * チャンクを格納するチャンク、汎用チャンク型
 */
class ChunkChunk extends Chunk {
    /** 次のチャンクを返す */
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

    /** 巻き戻し */
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

    /** オブジェクトを種にする */
    public ChunkChunk(final Chunk parent) {
        super(parent);
    }

    /**
     * ID だけ与え空のチャンクを作成する
     */
    public ChunkChunk(String id) {
        super(id);
    }

    /** チャンク取得に失敗 */
    public class FailGetChunkException extends RuntimeException {
    }

    /** チャンク書き込みに失敗 */
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

    /** チャネルモードを取得 */
    public int getChannelMode() {
        return channelMode;
    }

    /** ビットレートを取得 */
    public int getBitRate() {
        return bitRate;
    }

    /** サンプリング周波数を取得 */
    public int getSamplingRate() {
        return samplingRate;
    }

    /** 追加情報を取得 */
    public int getSecurityLevel() {
        return securityLevel;
    }

    /** */
    public String getVersion() {
        return version;
    }

    /** コンストラクションに失敗 */
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
// 汎用チャンク型を利用するサブチャンク型の宣言
// ///////////////////////////////////////////////////////
// TWIN チャンク
// typedef CChunkChunk CTwinChunk; // TWIN
//
// // 標準チャンク
// // COMM は汎用チャンクではない
// typedef CStringChunk CNameChunk; // NAME
// typedef CStringChunk CComtChunk; // COMT
// typedef CStringChunk CAuthChunk; // AUTH
// typedef CStringChunk CCpyrChunk; // (c)
// typedef CStringChunk CFileChunk; // FILE
// typedef CIntChunk<unsigned long> CDsizChunk; // DSIZ
// typedef CChunk CExtrChunk; // EXTR
//
// // 拡張チャンク・通常
// typedef CStringChunk CAlbmChunk; // ALBM
// // YEAR は汎用チャンクではない
// // ENCD は汎用チャンクではない
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
// // 拡張チャンク・補助
// typedef CChunkChunk CScndChunk; // SCND
//
// // 予約チャンク
// typedef CChunk C_Id3Chunk; // _ID3
// typedef CChunk C_YmhChunk; // _YMH
// typedef CChunk C_NttChunk; // _NTT
