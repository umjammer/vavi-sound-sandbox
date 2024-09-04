/*
 * (c)Copyright 1996-2000 NTT Cyber Space Laboratories
 *                Released on 2000.05.22 by N. Iwakami
 */

package vavi.sound.twinvq.obsolate;

import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * definitions related to the bitstream file operating tools
 *
 * @version 1.1 1999/02/24 added function bseek() <br/>
 *          1.2 1999/06/18 added a member "readable" to structure BFILE to improve bseek reliability <br/>
 *          0.0 1994/04/21 <br/>
 *          1.0 1999/01/08 <br/>
 *          1.1 1999/02/24 added function bseek() <br/>
 *          1.2 1999/06/18 bugfix about skipping frames <br/>
 *          1.3 1999/06/24 bugfix about skipping frames <br/>
 * @author N.Iwakami
 */
class BFile {

    static final int BYTE_BIT = 8;

    /** Bit buffer size (bytes) */
    static final int BBUFSIZ = 1024;

    /** Bit buffer length (bits) */
    static final int BBUFLEN = (BBUFSIZ * BYTE_BIT);

    /** Number of bit files */
    static final int N_BFILE = 50;

    static final int CHAR_BITS = 8;

    static final int BSEEK_CUR = 0; // SEEK_CUR
    static final int BSEEK_SET = 1; // SEEK_SET
    static final int BSEEK_END = 2; // SEEK_END

    /** current point in the bit buffer */
    long ptr;

    /** bit buffer size */
    int nbuf;

    /* NI, 1999/6/18 */
    /** set to 1 if the bit buffer is readable */
    int readable;

    /** the bit buffer */
    final byte[] buf = new byte[BBUFSIZ];

    /** R/W mode */
    String mode;

    /** the file pointer */
    RandomAccessFile io;

    /** b-file pointer ID used in tealloc() */
    int _file_id;

    int[] _bfile_flag = new int[N_BFILE];

    BFile[] _bfp_mem = new BFile[N_BFILE];

    /**
     * @param name Data file name
     * @param mode Access mode
     */
    BFile(String name, String mode) throws IOException {

        // Open file
        this.io = new RandomAccessFile(name, mode);
        // Clear pointer
        this.ptr = 0;
        // Reset the readable flag
        this.readable = 0;
        // Set mode
        this.mode = mode;
    }

    void bclose() throws IOException {
        long iadr;

        // If mode is "w" output data remaining in the buffer
        if (this.mode.equals("w")) {
            iadr = (this.ptr + BYTE_BIT - 1) / BYTE_BIT;
            this.io.write(this.buf, 0, (int) iadr);
        }
        // Close file
        this.io.close();
    }

    long bseek(long offset, int origin) throws IOException {
        long fs_ret;

        if (!this.mode.equals("w")) {
            System.err.print("bseek(): No seek support in write mode.");
            return 2;
        }

        switch (origin) {
        case BSEEK_CUR: {
            long offsetTest = offset; // this.ptr
            // if seeking is within the buffer
            if (0 <= offsetTest && offsetTest < this.nbuf) {
                this.ptr = offsetTest;
            } else {
                if (offsetTest >= this.nbuf) { // forward seek
                    long nSeekStep = offsetTest / BBUFLEN;
                    if (this.readable == 1) {
                        nSeekStep -= 1;
                    }
                    fs_ret = this.io.getFilePointer();
                    this.io.seek(fs_ret + nSeekStep * BBUFSIZ);
                    fs_ret += nSeekStep * BBUFSIZ;
                    if (fs_ret != 0)
                        return fs_ret;

                    this.readable = 0;
                    this.ptr = offsetTest % BBUFLEN;
                } else { // backward seek
                    long nSeekStep = (offsetTest - (BBUFLEN - 1)) / BBUFLEN;
                    if (this.readable == 1) {
                        nSeekStep -= 1;
                    }
                    fs_ret = this.io.getFilePointer();
                    this.io.seek(nSeekStep * BBUFSIZ);
                    fs_ret += nSeekStep * BBUFSIZ;
                    if (fs_ret != 0)
                        return fs_ret;

                    this.readable = 0;
                    this.ptr = (offsetTest - nSeekStep * BBUFLEN) % BBUFLEN;
                }
            }
        }
            break;
        case BSEEK_SET: // top of the file
            if (offset < 0) {
                return 1;
            }
            this.io.seek((offset / BBUFLEN) * BBUFSIZ);
            fs_ret = (offset / BBUFLEN) * BBUFSIZ;
            if (fs_ret != 0) {
                return fs_ret;
            }
            this.readable = 0;
            this.nbuf *= 8;
            this.ptr = offset % BBUFLEN;
            break;
        case BSEEK_END:
            if (offset > 0) {
                return 1;
            }
            this.io.seek(((offset - BBUFLEN + 1) / BBUFLEN - 1) * BBUFSIZ);
            fs_ret = -1; // TODO SEEK_END
            if (fs_ret != 0)
                return fs_ret;
            this.readable = 0;
            this.nbuf *= 8;
            this.ptr = (offset + BBUFLEN) % BBUFLEN;
            break;
        default:
            System.err.printf("bseek(): %d: Invalid origin ID.", origin);
            return 2;
        }
        return 0;
    }

    /**
     * @param data Output: Output data array
     * @param size Length of each data
     * @param nbits Number of bits to write
     */
    int read(byte[] data, int size, int nbits) throws IOException {
        // Variables
        int ibits;
        long iptr, ibufbit;
        int icl, idata, ibufadr;
        byte mask, tmpdat;
        int retval;

        // Main operation
        retval = 0;
        mask = 0x1;
        for (ibits = 0; ibits < nbits; ibits++) {
            if (this.readable == 0) { // when the file data buffer is empty
                this.nbuf = this.io.read(this.buf, 0, BBUFSIZ);
                this.nbuf *= 8;
                this.readable = 1;
            }
            // current file data buffer pointer
            iptr = this.ptr;
            // If data file is empty then return
            if (iptr >= this.nbuf) {
                return retval;
            }
            // current file data buffer address
            ibufadr = (int) (iptr / BYTE_BIT);
            // current file data buffer bit
            ibufbit = iptr % BYTE_BIT;
            // tmpdat = stream.buf[ibufadr] >> (BYTE_BIT-ibufbit-1);
            tmpdat = this.buf[ibufadr];
            tmpdat = (byte) (tmpdat >>> (BYTE_BIT - ibufbit - 1));
            // current data bit

            // output data address
            idata = ibits * size;
            // set output data
            data[idata] = (byte) (tmpdat & mask);
            for (icl = 1; icl < size; icl++) {
                // clear the rest output data buffer
                data[idata + icl] = 0;
            }
            // update data buffer pointer
            this.ptr += 1;
            if (this.ptr == BBUFLEN) {
                this.ptr = 0;
                this.readable = 0;
            }
            ++retval;
        }
        return retval;
    }

    static final int BITS_INT = 4 * 8;

    /**
     * @param data input data
     * @param nbits number of bits
     */
    int getBStream(int[] data, int offset, int nbits) throws IOException {
        byte[] tmpbit = new byte[BITS_INT];

        if (nbits > BITS_INT) {
            throw new IllegalArgumentException(String.format("getBStream: %d: %d Error.", nbits, BITS_INT));
        }
        int retval = read(tmpbit, 1, nbits);
        for (int ibit = retval; ibit < nbits; ibit++) {
            tmpbit[ibit] = 0;
        }
        int mask = 0x1 << (nbits - 1);
        int work = 0;
        for (int ibit = 0; ibit < nbits; ibit++) {
            work += mask * tmpbit[ibit];
            mask >>>= 1;
        }
        data[offset] = work;
        return retval;
    }
}
