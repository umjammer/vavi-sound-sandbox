/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.ituneslibrary;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSNumber;
import org.rococoa.cocoa.foundation.NSObject;
import org.rococoa.cocoa.foundation.NSString;

import vavix.rococoa.foundation.NSMutableDictionary;
import vavix.rococoa.foundation.NSSet;


/**
 * ITLibMediaEntity.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/02/17 umjammer initial version <br>
 */
public abstract class ITLibMediaEntity extends NSObject {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("ITLibMediaEntity", _Class.class);

    interface _Class extends ObjCClass {
        ITLibMediaEntity alloc();
    }

    public abstract NSMutableDictionary fields();

    public abstract NSNumber persistentID();

    /**
     * @return nullable
     */
    public abstract NSObject valueForKey(NSString property);

    /**
     * @param properties NullAllowed
     * @param handler ITLibMediaEntityEnumerateValuesHandler
     */
    public abstract void enumerateValuesForProperties(NSSet properties, com.sun.jna.Pointer handler);

    /**
     * @param properties NullAllowed
     * @param handler ITLibMediaEntityEnumerateValuesHandler
     */
    public abstract void enumerateValuesExceptForProperties(NSSet properties, com.sun.jna.Pointer handler);
}

