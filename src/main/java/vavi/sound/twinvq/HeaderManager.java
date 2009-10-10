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
    /** �ʏ�`�����N�̃`�����N�o���N */
    private Map<String, Chunk> primaryChunkBank;

    /** �⏕�`�����N�̃`�����N�o���N */
    private Map<String, Chunk> secondaryChunkBank;

    /** TWIN �`�����N��ID�A�ʏ��ID�ƈႢ "TWIN"+<�o�[�W�������ʎq>�ō\�������B */
    private String chunkID;

    /** �`�����N�o���N����ID�Ń`�����N�������o�� */
    Chunk getChunk(Map<String, Chunk> chunkBank, String id) {

        // �`�����N�̂���Ȃ���₢���킹��B
        if (chunkBank.containsKey(id)) {
            // �����
            // ���̃`�����N��߂��B
            return chunkBank.get(id);
        }

        // �`�����N���Ȃ���Ώ������������B
        throw new FailGetChunkException();
    }

    /**
     * �`�����N����͂��āA�T�u�`�����N���E���o���`�����N�o���N�ɗa����
     * @param chunkBank In/Out �`�����N�o���N
     * @param inputChunk ���̓`�����N
     * Chunk�^�̃`�����N����T�u�`�����N�����o���`�����N�o���N�ɓo�^����
     */
    private void PickUpSubChunks(Map<String, Chunk> chunkBank, ChunkChunk inputChunk) {
        // ����
        // �`�����N ID �̃T�C�Y�i�S�����j
        final int idSize = 4;

        // �`�����N����͂���O�ɂ܂��߂����s��
        inputChunk.rewind();

        Chunk subChunk;
        try {
            // �`�����N����T�u�`�����N�����o��
            while ((subChunk = inputChunk.GetNextChunk(idSize)) != null) {
                String id = subChunk.getID();
                // ���o�����T�u�`�����N���`�����N�o���N�ɓo�^
                chunkBank.put(id, subChunk);
            }
        } catch (ChunkChunk.FailGetChunkException e) {
            throw new WrongChunkFormatException();
        }

    }

    /**
     * �w�b�_�}�l�[�W���̏�����������BCreate() ����̂݌Ă΂��B
     * ����������B�R���X�g���N�^�̑���Ɏg��
     */
    void init(ChunkChunk twinChunk) {
        try {
            // ��{�`�����N����{�`�����N�o���N�Ɏ��߂�B
            PickUpSubChunks(primaryChunkBank, twinChunk);

            // �⏕�`�����N����������⏕�`�����N�o���N�Ɏ��߂�B
            ChunkChunk scndChunk = (ChunkChunk) getPrimaryChunk("SCND");
            PickUpSubChunks(secondaryChunkBank, scndChunk);
        } catch (ChunkChunk.FailGetChunkException e) {
            // Debug.pprintln("Fail!!");
        } catch (FailGetChunkException e) {
            // Debug.pprintln("Fail getting SCND chnunk");
        }
    }

    /**
     * �R���X�g���N�^�B���[�U�͌ĂׂȂ��B����� Create() ���g���B
     * �������̍ۂɃG���[���o��\�������邽�߂��̂悤�Ȏd�l�ɂ����B
     */
    private HeaderManager() {
    }

    /** �`�����N�̏������������Ȃ� */
    class WrongChunkFormatException extends RuntimeException {
    }

    /** �ʏ�`�����N�������o�� */
    public Chunk getPrimaryChunk(String id) {
        return getChunk(primaryChunkBank, id);
    }

    /** �⏕�`�����N�������o�� */
    public Chunk getSecondaryChunk(String id) {
        return getChunk(secondaryChunkBank, id);
    }

    /** */
    public final String getID() {
        return chunkID;
    }

    /**
     * �w�b�_�}�l�[�W���𐶐�����B
     * �`�����N�}�l�[�W�������o���B�R���X�g���N�^�̑���
     * 
     * @return ���������w�b�_�}�l�[�W���ւ̃|�C���^�A�����Ɏ��s�����ꍇ�� null
     */
    static HeaderManager create(ChunkChunk twinChunk) {
        try {
            // �`�����N�}�l�[�W���𐶐�����B
            HeaderManager theManager = null;
            theManager = new HeaderManager();
            theManager.init(twinChunk);

            // TWIN�`�����N�̃w�b�_���擾����
            theManager.chunkID = twinChunk.getID();
            if (theManager.chunkID == "") {
                return null;
            }

            return theManager;
        } catch (WrongChunkFormatException e) {
            return null;
        }
    }

    /** �`�����N�̎擾�Ɏ��s���� */
    class FailGetChunkException extends RuntimeException {
    }
}

// �w�b�_�}�l�[�W������̃f�[�^�ǂݏo���̎x���N���X

/**
 * Unified string information, ������`�����N�̑������A
 * �w�b�_�}�l�[�W������擾���邱�Ƃ��ł���
 */
class UniStringInfo {
    public enum CharCode {
        unknown_code(-1),
        ISO_8859_1(0),
        Unicode(1),
        S_JIS(2),
        JIS(3),
        EUC(4);
        int value;

        CharCode(int value) {
            this.value = value;
        }
    }

    /** �`�����N ID */
    private String id;

    /** ��{������ */
    private String primary;

    /** �⏕������ */
    private String secondary;

    /** ��{������̕����R�[�h */
    private int primaryCharCode;

    /** �⏕������̕����R�[�h */
    private int secondaryCharCode;

    private void putPrimaryInfo(StringChunk theChunk) {
        // ID ���`�F�b�N
        if (id == "") {
            id = theChunk.getID();
        } else if (id != theChunk.getID()) {
            throw new IDException();
        }

        // �f�[�^����������
        primary = theChunk.getString();
    }

    private void putSecondaryInfo(StringChunk theChunk) {
        // ID ���`�F�b�N
        if (id == "") {
            id = theChunk.getID();
        } else if (id != theChunk.getID()) {
            throw new IDException();
        }

        // �f�[�^����������
        String secondary = theChunk.getString();
        // �����R�[�h��񂪂��邩�ǂ����`�F�b�N
        if (secondary.length() < 2) {
            throw new NoCharCodeException();
        }

        // �����R�[�h�f�[�^
        primaryCharCode = secondary.charAt(0) - '0';
        secondaryCharCode = secondary.charAt(1) - '0';

        secondary = secondary.substring(2, secondary.length());
    }

    /** �������̍ہA��{�`�����N�ƕ⏕�`�����N�� ID ���H������Ă��� */
    class IDException extends RuntimeException {
    }

    /** �⏕�`�����N�ɕ����R�[�h��񂪂Ȃ� */
    class NoCharCodeException extends RuntimeException {
    }

    /** ��{�������Ԃ� */
    public final String getPrimaryInfo() {
        return primary;
    }

    /** �⏕�������Ԃ� */
    public final String getSecondaryInfo() {
        return secondary;
    }

    /** ��{������̕����R�[�h��Ԃ� */
    public final int getPrimaryCharCode() {
        return primaryCharCode;
    }

    /** �⏕������̕����R�[�h��Ԃ� */
    public final int getSecondaryCharCode() {
        return secondaryCharCode;
    }

    /**
     * �R���X�g���N�^�A�K�v�ȏ���S�ė^����
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

    /** �R���X�g���N�^�A�w�b�_�}�l�[�W������ǂݏo�� */
    UniStringInfo(String id, HeaderManager theManager) {
        // ID ��ݒ肷��
        this.id = id;
        primaryCharCode = -1;
        secondaryCharCode = -1;

        int flag = 0;
        // ��{�`�����N�����R�s�[����
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

        // �⏕�`�����N�����R�s�[����
        try {
            StringChunk scndChunk = new StringChunk(theManager.getSecondaryChunk(id));
            putSecondaryInfo(scndChunk);
        } catch (HeaderManager.FailGetChunkException e) {
        } catch (NoCharCodeException e) {
            throw new FailConstructionException();
        }
    }

    /** �R���X�g���N�g�̎��s */
    class FailConstructionException extends RuntimeException {
    }
}

/* */
