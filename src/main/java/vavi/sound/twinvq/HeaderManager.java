/*
 * Copyright 1996-2000 (c) NTT Cyber Space Laboratories
 *
 * Modified on 2000.09.06 by N. Iwakami
 */

package vavi.sound.twinvq;

import java.util.Map;


/**
 * HeaderManager
 */
class HeaderManager {
    /** 通常チャンクのチャンクバンク */
    private Map<String, Chunk> primaryChunkBank;

    /** 補助チャンクのチャンクバンク */
    private Map<String, Chunk> secondaryChunkBank;

    /** TWIN チャンクのID、通常のIDと違い "TWIN"+<バージョン識別子>で構成される。 */
    private String chunkID;

    /** チャンクバンクからIDでチャンクを引き出す */
    Chunk getChunk(Map<String, Chunk> chunkBank, String id) {

        // チャンクのあるなしを問い合わせる。
        if (chunkBank.containsKey(id)) {
            // あれば
            // そのチャンクを戻す。
            return chunkBank.get(id);
        }

        // チャンクがなければ処理を放棄する。
        throw new FailGetChunkException();
    }

    /**
     * チャンクを入力して、サブチャンクを拾い出しチャンクバンクに預ける
     * @param chunkBank In/Out チャンクバンク
     * @param inputChunk 入力チャンク
     * Chunk型のチャンクからサブチャンクを取り出しチャンクバンクに登録する
     */
    private void PickUpSubChunks(Map<String, Chunk> chunkBank, ChunkChunk inputChunk) {
        // 準備
        // チャンク ID のサイズ（４文字）
        final int idSize = 4;

        // チャンクを解析する前にまき戻しを行う
        inputChunk.rewind();

        Chunk subChunk;
        try {
            // チャンクからサブチャンクを取り出す
            while ((subChunk = inputChunk.GetNextChunk(idSize)) != null) {
                String id = subChunk.getID();
                // 取り出したサブチャンクをチャンクバンクに登録
                chunkBank.put(id, subChunk);
            }
        } catch (ChunkChunk.FailGetChunkException e) {
            throw new WrongChunkFormatException();
        }

    }

    /**
     * ヘッダマネージャの初期化をする。Create() からのみ呼ばれる。
     * 初期化する。コンストラクタの代わりに使う
     */
    void init(ChunkChunk twinChunk) {
        try {
            // 基本チャンクを基本チャンクバンクに収める。
            PickUpSubChunks(primaryChunkBank, twinChunk);

            // 補助チャンクがあったら補助チャンクバンクに収める。
            ChunkChunk scndChunk = (ChunkChunk) getPrimaryChunk("SCND");
            PickUpSubChunks(secondaryChunkBank, scndChunk);
        } catch (ChunkChunk.FailGetChunkException e) {
            // Debug.pprintln("Fail!!");
        } catch (FailGetChunkException e) {
            // Debug.pprintln("Fail getting SCND chnunk");
        }
    }

    /**
     * コンストラクタ。ユーザは呼べない。代わりに Create() を使う。
     * 初期化の際にエラーが出る可能性があるためこのような仕様にした。
     */
    private HeaderManager() {
    }

    /** チャンクの書式が正しくない */
    static class WrongChunkFormatException extends RuntimeException {
    }

    /** 通常チャンクを引き出す */
    public Chunk getPrimaryChunk(String id) {
        return getChunk(primaryChunkBank, id);
    }

    /** 補助チャンクを引き出す */
    public Chunk getSecondaryChunk(String id) {
        return getChunk(secondaryChunkBank, id);
    }

    /** */
    public final String getID() {
        return chunkID;
    }

    /**
     * ヘッダマネージャを生成する。
     * チャンクマネージャを作り出す。コンストラクタの代わり
     *
     * @return 生成したヘッダマネージャへのポインタ、生成に失敗した場合は null
     */
    static HeaderManager create(ChunkChunk twinChunk) {
        try {
            // チャンクマネージャを生成する。
            HeaderManager theManager = null;
            theManager = new HeaderManager();
            theManager.init(twinChunk);

            // TWINチャンクのヘッダを取得する
            theManager.chunkID = twinChunk.getID();
            if (theManager.chunkID.isEmpty()) {
                return null;
            }

            return theManager;
        } catch (WrongChunkFormatException e) {
            return null;
        }
    }

    /** チャンクの取得に失敗した */
    static class FailGetChunkException extends RuntimeException {
    }
}

// ヘッダマネージャからのデータ読み出しの支援クラス

/**
 * Unified string information, 文字列チャンクの総合情報、
 * ヘッダマネージャから取得することができる
 */
class UniStringInfo {
    public enum CharCode {
        unknown_code(-1),
        ISO_8859_1(0),
        Unicode(1),
        S_JIS(2),
        JIS(3),
        EUC(4);
        final int value;

        CharCode(int value) {
            this.value = value;
        }
    }

    /** チャンク ID */
    private String id;

    /** 基本文字列 */
    private String primary;

    /** 補助文字列 */
    private String secondary;

    /** 基本文字列の文字コード */
    private int primaryCharCode;

    /** 補助文字列の文字コード */
    private int secondaryCharCode;

    private void putPrimaryInfo(StringChunk theChunk) {
        // ID をチェック
        if (id.isEmpty()) {
            id = theChunk.getID();
        } else if (!id.equals(theChunk.getID())) {
            throw new IDException();
        }

        // データを書き込み
        primary = theChunk.getString();
    }

    private void putSecondaryInfo(StringChunk theChunk) {
        // ID をチェック
        if (id.isEmpty()) {
            id = theChunk.getID();
        } else if (!id.equals(theChunk.getID())) {
            throw new IDException();
        }

        // データを書き込み
        String secondary = theChunk.getString();
        // 文字コード情報があるかどうかチェック
        if (secondary.length() < 2) {
            throw new NoCharCodeException();
        }

        // 文字コードデータ
        primaryCharCode = secondary.charAt(0) - '0';
        secondaryCharCode = secondary.charAt(1) - '0';

        secondary = secondary.substring(2);
    }

    /** 初期化の際、基本チャンクと補助チャンクの ID が食い違っている */
    static class IDException extends RuntimeException {
    }

    /** 補助チャンクに文字コード情報がない */
    static class NoCharCodeException extends RuntimeException {
    }

    /** 基本文字列を返す */
    public final String getPrimaryInfo() {
        return primary;
    }

    /** 補助文字列を返す */
    public final String getSecondaryInfo() {
        return secondary;
    }

    /** 基本文字列の文字コードを返す */
    public final int getPrimaryCharCode() {
        return primaryCharCode;
    }

    /** 補助文字列の文字コードを返す */
    public final int getSecondaryCharCode() {
        return secondaryCharCode;
    }

    /**
     * コンストラクタ、必要な情報を全て与える
     *
     * @param secondary deault = ""
     * @param primCode default = unknown_code
     * @param scndCode default = unknown_code
     */
    UniStringInfo(String id, String primary, String secondary, int primCode, int scndCode) {
        this.id = id;
        this.primary = primary;
        this.secondary = secondary;
        this.primaryCharCode = primCode;
        this.secondaryCharCode = scndCode;
    }

    /** コンストラクタ、ヘッダマネージャから読み出す */
    UniStringInfo(String id, HeaderManager theManager) {
        // ID を設定する
        this.id = id;
        primaryCharCode = -1;
        secondaryCharCode = -1;

        int flag = 0;
        // 基本チャンク情報をコピーする
        try {
            StringChunk primChunk = new StringChunk(theManager.getPrimaryChunk(id));
            putPrimaryInfo(primChunk);
        } catch (HeaderManager.FailGetChunkException e1) {
            flag = 1;
            // throw new err_FailConstruction();
        }
        if (flag != 0) {
            throw new FailConstructionException();
        }

        // 補助チャンク情報をコピーする
        try {
            StringChunk scndChunk = new StringChunk(theManager.getSecondaryChunk(id));
            putSecondaryInfo(scndChunk);
        } catch (HeaderManager.FailGetChunkException e) {
        } catch (NoCharCodeException e) {
            throw new FailConstructionException();
        }
    }

    /** コンストラクトの失敗 */
    static class FailConstructionException extends RuntimeException {
    }
}

/* */
