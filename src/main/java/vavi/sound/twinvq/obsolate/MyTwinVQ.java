/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq.obsolate;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import static java.lang.System.getLogger;


/**
 * MyTwinVQ.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-07-28 nsano initial version <br>
 */
public class MyTwinVQ implements TwinVQ {

    private static final Logger logger = getLogger(MyTwinVQ.class.getName());

    private static final int id = 97012000;

    private HeaderInfo setupInfo;

    @Override
    public int TvqEncInitialize(HeaderInfo setupInfo, EncSpecificInfo encInfo, Index index, int dispErrorMessageBox) {
logger.log(Level.TRACE, "TvqEncInitialize: ");
        return 0;
    }

    @Override
    public void TvqEncTerminate(Index index) {
logger.log(Level.TRACE, "TvqEncTerminate: ");
    }

    @Override
    public void TvqEncGetVectorInfo(int[][] bits0, int[][] bits1) {
logger.log(Level.TRACE, "TvqEncGetVectorInfo: ");
    }

    @Override
    public void TvqEncResetFrameCounter() {
logger.log(Level.TRACE, "TvqEncResetFrameCounter: ");
    }

    @Override
    public void TvqEncodeFrame(float[] sig_in, Index index) {
logger.log(Level.TRACE, "TvqEncodeFrame: ");
    }

    @Override
    public void TvqEncUpdateVectorInfo(int varbits, int ndiv, int[] bits0, int[] bits1) {
logger.log(Level.TRACE, "TvqEncUpdateVectorInfo: ");
    }

    @Override
    public void TvqEncSetFrameCounter(int position) {
logger.log(Level.TRACE, "TvqEncSetFrameCounter: ");
    }

    @Override
    public int TvqEncGetFrameSize() {
logger.log(Level.TRACE, "TvqEncGetFrameSize: ");
        return 0;
    }

    @Override
    public int TvqEncGetNumChannels() {
logger.log(Level.TRACE, "TvqEncGetNumChannels: ");
        return 0;
    }

    @Override
    public int TvqEncGetNumFixedBitsPerFrame() {
logger.log(Level.TRACE, "TvqEncGetNumFixedBitsPerFrame: ");
        return 0;
    }

    @Override
    public void TvqEncGetSetupInfo(HeaderInfo setupInfo) {
logger.log(Level.TRACE, "TvqEncGetSetupInfo: ");
    }

    @Override
    public float TvqEncGetSamplingRate() {
logger.log(Level.TRACE, "TvqEncGetSamplingRate: ");
        return 0;
    }

    @Override
    public int TvqEncGetBitRate() {
logger.log(Level.TRACE, "TvqEncGetBitRate: ");
        return 0;
    }

    @Override
    public void TvqEncGetConfInfo(ConfInfo cf) {
logger.log(Level.TRACE, "TvqEncGetConfInfo: ");
    }

    @Override
    public int TvqEncGetNumFrames() {
logger.log(Level.TRACE, "TvqEncGetNumFrames: ");
        return 0;
    }

    @Override
    public int TvqGetVersionID(int versionNum, String versionString) {
logger.log(Level.TRACE, "TvqGetVersionID: ");
        return 0;
    }

    @Override
    public int TvqEncCheckVersion(String strTvqID) {
logger.log(Level.TRACE, "TvqEncCheckVersion: ");
        return 0;
    }

    @Override
    public int TvqEncGetModuleVersion(String versionString) {
logger.log(Level.TRACE, "TvqEncGetModuleVersion: ");
        return 0;
    }

    @Override
    public int TvqInitialize(HeaderInfo setupInfo, Index index, int dispErrorMessageBox) {
logger.log(Level.TRACE, "TvqInitialize: ");
logger.log(Level.TRACE, "setupInfo: " + setupInfo);
logger.log(Level.TRACE, "index: " + index);
        this.setupInfo = setupInfo;
        return 0;
    }

    @Override
    public void TvqTerminate(Index index) {
logger.log(Level.TRACE, "TvqTerminate: ");
    }

    @Override
    public void TvqGetVectorInfo(int[][] bits0, int[][] bits1) {
logger.log(Level.TRACE, "TvqGetVectorInfo: ");
    }

    @Override
    public void TvqResetFrameCounter() {
logger.log(Level.TRACE, "TvqResetFrameCounter: ");
    }

    @Override
    public void TvqDecodeFrame(Index indexp, float[] out) {
logger.log(Level.TRACE, "TvqDecodeFrame: ");
    }

    @Override
    public int TvqWtypeToBtype(int w_type, int[] btype) {
logger.log(Level.TRACE, "TvqWtypeToBtype: ");
        return 0;
    }

    @Override
    public void TvqUpdateVectorInfo(int varbits, int[] ndiv, int[] bits0, int[] bits1) {
logger.log(Level.TRACE, "TvqUpdateVectorInfo: ");
    }

    @Override
    public void TvqSetFrameCounter(int position) {
logger.log(Level.TRACE, "TvqSetFrameCounter: ");
    }

    @Override
    public int TvqCheckVersion(String versionID) {
logger.log(Level.TRACE, "TvqCheckVersion: " + versionID);
        return versionID.startsWith("TWIN") ? id : TVQ_UNKNOWN_VERSION;
    }

    @Override
    public void TvqGetSetupInfo(HeaderInfo setupInfo) {
logger.log(Level.TRACE, "TvqGetSetupInfo: ");
    }

    @Override
    public void TvqGetConfInfo(ConfInfo cf) {
logger.log(Level.TRACE, "TvqGetConfInfo: ");
    }

    @Override
    public int TvqGetFrameSize() {
logger.log(Level.TRACE, "TvqGetFrameSize: ");
        return 0;
    }

    @Override
    public int TvqGetNumChannels() {
        int numChannels = setupInfo.channelMode == 1 ? 2 : 1;
logger.log(Level.TRACE, "TvqGetNumChannels: " + numChannels);
        return numChannels;
    }

    @Override
    public int TvqGetBitRate() {
logger.log(Level.TRACE, "TvqGetBitRate: ");
        return 0;
    }

    @Override
    public float TvqGetSamplingRate() {
logger.log(Level.TRACE, "TvqGetSamplingRate: ");
        return 0;
    }

    @Override
    public int TvqGetNumFixedBitsPerFrame() {
        int numChannels = setupInfo.channelMode == 1 ? 2 : 1;
logger.log(Level.TRACE, "TvqGetNumFixedBitsPerFrame: ");
        return 16 * numChannels;
    }

    @Override
    public int TvqGetNumFrames() {
logger.log(Level.TRACE, "TvqGetNumFrames: ");
        return 0;
    }

    @Override
    public int TvqGetModuleVersion(byte[] versionString) {
logger.log(Level.TRACE, "TvqGetModuleVersion: ");
        return 0;
    }

    @Override
    public void TvqFbCountUsedBits(int nbit) {
logger.log(Level.TRACE, "TvqFbCountUsedBits: ");
    }

    @Override
    public float TvqGetFbCurrentBitrate() {
logger.log(Level.TRACE, "TvqGetFbCurrentBitrate: ");
        return 0;
    }

    @Override
    public int TvqGetFbTotalBits() {
logger.log(Level.TRACE, "TvqGetFbTotalBits: ");
        return 0;
    }
}
