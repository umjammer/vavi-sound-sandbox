/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.ituneslibrary;

import java.util.ArrayList;
import java.util.List;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSArray;
import org.rococoa.cocoa.foundation.NSError;
import org.rococoa.cocoa.foundation.NSObject;
import org.rococoa.cocoa.foundation.NSURL;


/**
 * ITLibrary.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/02/17 umjammer initial version <br>
 * @see "https://github.com/xamarin/xamarin-macios/blob/b8f9dccdcc09f43a44d591fd0eaf9ba954ea2752/src/ituneslibrary.cs"
 */
public abstract class ITLibrary extends NSObject {

    static {
        ITunesLibrary.instance.toString();
        CLASS = org.rococoa.Rococoa.createClass("ITLibrary", _Class.class);
    }

    @SuppressWarnings("hiding")
    private static final _Class CLASS;

    public interface _Class extends ObjCClass {
        ITLibrary libraryWithAPIVersion_error(String requestedAPIVersion, NSError error);

        /**
         * @param options ITLibInitOptions
         * @return NullAllowed
         */
        ITLibrary libraryWithAPIVersion_options_error(String requestedAPIVersion, int options, NSError error);

        ITLibrary initWithAPIVersion_error(String requestedAPIVersion, NSError error);

        /**
         * @param options ITLibInitOptions
         */
        ITLibrary initWithAPIVersion_options_error(String requestedAPIVersion, int options, NSError error);
    }

    public abstract String applicationVersion();

    /**
     * @return ITLibExportFeature
     */
    public abstract int features();

    public abstract int apiMajorVersion();

    public abstract int apiMinorVersion();

    // NullAllowed
    public abstract NSURL mediaFolderLocation();

    // NullAllowed
    public abstract NSURL musicFolderLocation();

    public abstract boolean shouldShowContentRating();

    public abstract NSArray allMediaItems();

    public abstract NSArray allPlaylists();

    public List<ITLibMediaItem> getMediaItems() {
        List<ITLibMediaItem> result = new ArrayList<>();
        NSArray tracks = allMediaItems();
        for (int i = 0; i < tracks.count(); i++) {
            result.add(org.rococoa.Rococoa.cast(tracks.objectAtIndex(i), ITLibMediaItem.class));
        }
        return result;
    }

    public List<ITLibPlaylist> getPlaylists() {
        List<ITLibPlaylist> result = new ArrayList<>();
        NSArray playlists = allMediaItems();
        for (int i = 0; i < playlists.count(); i++) {
            result.add(org.rococoa.Rococoa.cast(playlists.objectAtIndex(i), ITLibPlaylist.class));
        }
        return result;
    }

    // return: NullAllowed
    public abstract ITLibArtwork artworkForMediaFile(NSURL mediaFileUrl);

    public abstract boolean reloadData();

    public abstract void unloadData();

    public static ITLibrary libraryWithAPIVersion(String requestedAPIVersion) {
        NSError error = NSError.CLASS.alloc();
        ITLibrary library = CLASS.libraryWithAPIVersion_error(requestedAPIVersion, error);
        if (library == null) {
            throw new IllegalStateException();
        }
        return library;
    }

    /**
     * @param options ITLibInitOptions
     */
    public static ITLibrary libraryWithAPIVersion(String requestedAPIVersion, int options) {
        NSError error = NSError.CLASS.alloc();
        ITLibrary library = CLASS.libraryWithAPIVersion_options_error(requestedAPIVersion, options, error);
        if (library == null) {
            throw new IllegalStateException();
        }
        return library;
    }

    public static ITLibrary initWithAPIVersion(String requestedAPIVersion) {
        NSError error = NSError.CLASS.alloc();
        ITLibrary library = CLASS.initWithAPIVersion_error(requestedAPIVersion, error);
        if (library == null) {
            throw new IllegalStateException();
        }
        return library;
    }

    /**
     * @param options ITLibInitOptions
     */
    public static ITLibrary initWithAPIVersion(String requestedAPIVersion, int options) {
        NSError error = NSError.CLASS.alloc();
        ITLibrary library = CLASS.initWithAPIVersion_options_error(requestedAPIVersion, options, error);
        if (library == null) {
            throw new IllegalStateException();
        }
        return library;
    }
}
