/*
 * (c)Copyright 1996-2000 NTT Cyber Space Laboratories
 *                Modified on 2000.09.06 by N. Iwakami
 */

package vavi.sound.twinvq.obsolate;

import java.util.Map;


/**
 * HeaderManager
 */
class HeaderManager {
    /** Chunk bank of normal chunks */
    private Map<String, Chunk> primaryChunkBank;

    /** Auxiliary chunk chunk bank */
    private Map<String, Chunk> secondaryChunkBank;

    /** TWIN chunk ID, unlike a normal ID, consists of "TWIN" + "version identifier". */
    private String chunkID;

    /** Extracts chunks from chunk bank by ID. */
    static Chunk getChunk(Map<String, Chunk> chunkBank, String id) {

        // Query whether chunks are present or not.
        if (chunkBank.containsKey(id)) {
            // If it exists, return that chunk.
            return chunkBank.get(id);
        }

        // If there are no chunks, the process is abandoned.
        throw new FailGetChunkException();
    }

    /**
     * Input a chunk, pick up sub-chunks and deposit them in the chunk bank
     * @param chunkBank In/Out chunk bank
     * @param inputChunk input chunk
     * Extract sub-chunks from Chunk-type chunks and register them in the chunk bank.
     */
    private static void PickUpSubChunks(Map<String, Chunk> chunkBank, ChunkChunk inputChunk) {
        // preparation
        // chunk ID size (4 characters)
        final int idSize = 4;

        // rewinds before parsing chunks
        inputChunk.rewind();

        Chunk subChunk;
        try {
            // extract subchunk from chunk
            while ((subChunk = inputChunk.GetNextChunk(idSize)) != null) {
                String id = subChunk.getID();
                // register the retrieved subchunk to the chunk bank
                chunkBank.put(id, subChunk);
            }
        } catch (ChunkChunk.FailGetChunkException e) {
            throw new WrongChunkFormatException();
        }

    }

    /**
     * Initializes the header manager. Called only from #create().
     * Uses instead of constructor for initializing.
     */
    void init(ChunkChunk twinChunk) {
        try {
            // put the basic chunks into the basic chunk bank.
            PickUpSubChunks(primaryChunkBank, twinChunk);

            // if there are auxiliary chunks, put them in the auxiliary chunk bank.
            ChunkChunk scndChunk = (ChunkChunk) getPrimaryChunk("SCND");
            PickUpSubChunks(secondaryChunkBank, scndChunk);
        } catch (ChunkChunk.FailGetChunkException e) {
            // Debug.pprintln("Fail!!");
        } catch (FailGetChunkException e) {
            // Debug.pprintln("Fail getting SCND chnunk");
        }
    }

    /**
     * Constructor. User cannot call. Use Create() instead.
     * This specification was made because there is a possibility that an error may occur during initialization.
     */
    private HeaderManager() {
    }

    /** Chunk format is incorrect */
    static class WrongChunkFormatException extends RuntimeException {
    }

    /** Pulls out regular chunks. */
    public Chunk getPrimaryChunk(String id) {
        return getChunk(primaryChunkBank, id);
    }

    /** Pulls out auxiliary chunks. */
    public Chunk getSecondaryChunk(String id) {
        return getChunk(secondaryChunkBank, id);
    }

    /** */
    public final String getID() {
        return chunkID;
    }

    /**
     * Generates header manager.
     * Create a chunk manager. instead of constructor.
     *
     * @return pointer to the generated header manager, or null if generation fails
     */
    static HeaderManager create(ChunkChunk twinChunk) {
        try {
            // generate a chunk manager.
            HeaderManager theManager = null;
            theManager = new HeaderManager();
            theManager.init(twinChunk);

            // get TWIN chunk header
            theManager.chunkID = twinChunk.getID();
            if (theManager.chunkID.isEmpty()) {
                return null;
            }

            return theManager;
        } catch (WrongChunkFormatException e) {
            return null;
        }
    }

    /** Failed to get chunk */
    static class FailGetChunkException extends RuntimeException {
    }
}

// support class for reading data from header manager

/**
 * Unified string information, General information about string chunks,
 * can be obtained from header manager
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

    /** chunk ID */
    private String id;

    /** basic string */
    private String primary;

    /** auxiliary string */
    private String secondary;

    /** basic string character code */
    private int primaryCharCode;

    /** character code of auxiliary string */
    private int secondaryCharCode;

    private void putPrimaryInfo(StringChunk theChunk) {
        // check ID
        if (id.isEmpty()) {
            id = theChunk.getID();
        } else if (!id.equals(theChunk.getID())) {
            throw new IDException();
        }

        // write data
        primary = theChunk.getString();
    }

    private void putSecondaryInfo(StringChunk theChunk) {
        // check ID
        if (id.isEmpty()) {
            id = theChunk.getID();
        } else if (!id.equals(theChunk.getID())) {
            throw new IDException();
        }

        // write data
        String secondary = theChunk.getString();
        // check if there is character code information
        if (secondary.length() < 2) {
            throw new NoCharCodeException();
        }

        // character code data
        primaryCharCode = secondary.charAt(0) - '0';
        secondaryCharCode = secondary.charAt(1) - '0';

        secondary = secondary.substring(2);
    }

    /** During initialization, the IDs of the basic chunk and auxiliary chunk are different. */
    static class IDException extends RuntimeException {
    }

    /** There is no character code information in the auxiliary chunk */
    static class NoCharCodeException extends RuntimeException {
    }

    /** Returns basic string. */
    public final String getPrimaryInfo() {
        return primary;
    }

    /** Returns auxiliary string. */
    public final String getSecondaryInfo() {
        return secondary;
    }

    /** Returns the character code of the basic string. */
    public final int getPrimaryCharCode() {
        return primaryCharCode;
    }

    /** Returns the character code of the auxiliary string. */
    public final int getSecondaryCharCode() {
        return secondaryCharCode;
    }

    /**
     * Constructor, give all necessary information.
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

    /** Constructor, read from header manager. */
    UniStringInfo(String id, HeaderManager theManager) {
        // set ID
        this.id = id;
        primaryCharCode = -1;
        secondaryCharCode = -1;

        int flag = 0;
        // copy basic chunk information
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

        // copy auxiliary chunk information
        try {
            StringChunk scndChunk = new StringChunk(theManager.getSecondaryChunk(id));
            putSecondaryInfo(scndChunk);
        } catch (HeaderManager.FailGetChunkException e) {
        } catch (NoCharCodeException e) {
            throw new FailConstructionException();
        }
    }

    /** Construction failure */
    static class FailConstructionException extends RuntimeException {
    }
}
