/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.ituneslibrary;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSData;
import org.rococoa.cocoa.foundation.NSImage;
import org.rococoa.cocoa.foundation.NSObject;


/**
 * @interface ITLibArtwork : NSObject
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/02/17 umjammer initial version <br>
 */
public abstract class ITLibArtwork extends NSObject {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("ITLibArtwork", _Class.class);

    interface _Class extends ObjCClass {
        ITLibArtwork alloc();
    }

    // NullAllowed
    public abstract NSImage image();

    // NullAllowed
    public abstract NSData imageData();

    /**
     * @return ITLibArtworkFormat
     */
    public abstract int imageDataFormat();

    public BufferedImage getImage() throws IOException {
        NSData data = imageData();
        byte[] bytes = new byte[data.length()];
        data.getBytes(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return ImageIO.read(bais);
    }
}
