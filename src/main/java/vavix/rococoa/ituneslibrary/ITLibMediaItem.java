/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.ituneslibrary;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSDate;
import org.rococoa.cocoa.foundation.NSURL;


/**
 * ITLibMediaItem.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/02/17 umjammer initial version <br>
 */
public abstract class ITLibMediaItem extends ITLibMediaEntity {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("ITLibMediaItem", _Class.class);

    interface _Class extends ObjCClass {
        ITLibMediaItem alloc();
    }

    public abstract String title();

    // NullAllowed
    public abstract String sortTitle();

    // NullAllowed
    public abstract ITLibArtist artist();

    public abstract String composer();

    // NullAllowed
    public abstract String sortComposer();

    public abstract int rating();

    public abstract boolean isRatingComputed();

    public abstract int startTime();

    public abstract int stopTime();

    public abstract ITLibAlbum album();

    public abstract String genre();

    // NullAllowed
    public abstract String kind();

    /**
     * @return ITLibMediaItemMediaKind
     */
    public abstract int mediaKind();

    public abstract long fileSize();

    public abstract int size();

    public abstract int totalTime();

    public abstract int trackNumber();

    // NullAllowed
    public abstract String category();

    // NullAllowed
    @Override
    public abstract String description();

    /**
     * @return ITLibMediaItemLyricsContentRating
     */
    public abstract int lyricsContentRating();

    // NullAllowed
    public abstract String contentRating();

    // NullAllowed
    public abstract NSDate modifiedDate();

    // NullAllowed
    public abstract NSDate addedDate();

    public abstract int bitrate();

    public abstract int sampleRate();

    public abstract int beatsPerMinute();

    public abstract int playCount();

    // NullAllowed
    public abstract NSDate lastPlayedDate();

    /**
     * @return ITLibMediaItemPlayStatus
     */
    public abstract int playStatus();

    // NullAllowed
    public abstract NSURL location();

    public abstract boolean hasArtworkAvailable();

    // NullAllowed
    public abstract ITLibArtwork artwork();

    // NullAllowed
    public abstract String comments();

    public abstract  boolean isPurchased();

    public abstract  boolean isCloud();

    public abstract boolean isDRMProtected();

    public abstract boolean isVideo();

    // NullAllowed
    public abstract ITLibMediaItemVideoInfo videoInfo();

    // NullAllowed
    public abstract NSDate releaseDate();

    public abstract int year();

    public abstract int fileType();

    public abstract int skipCount();

    // NullAllowed
    public abstract NSDate skipDate();

    // NullAllowed
    public abstract String voiceOverLanguage();

    public abstract int volumeAdjustment();

    public abstract int volumeNormalizationEnergy();

    public abstract boolean isUserDisabled();

    // NullAllowed
    public abstract String grouping();

    /**
     * @return ITLibMediaItemLocationType
     */
    public abstract int locationType();
}
