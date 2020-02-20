/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.ituneslibrary;

import java.util.Set;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSNumber;
import org.rococoa.cocoa.foundation.NSObject;
import org.rococoa.cocoa.foundation.NSString;


/**
 * ITLibMediaEntity.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/02/17 umjammer initial version <br>
 */
public abstract class ITLibMediaEntity extends NSObject {

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("ITLibMediaEntity", _Class.class);

    interface _Class extends ObjCClass {
        ITLibMediaEntity alloc();
    }

    public abstract NSNumber persistentID();

	//NullAllowed
    public abstract NSObject valueFor(String property);

	//[Export ("enumerateValuesForProperties:usingBlock:")]
    public abstract void enumerateValues (/*NullAllowed*/ Set<NSString> properties, /*ITLibMediaEntityEnumerateValuesHandler*/int handler);

	//[Export ("enumerateValuesExceptForProperties:usingBlock:")]
    public abstract void enumerateValuesExcept (/*NullAllowed*/ Set<NSString> properties, /*ITLibMediaEntityEnumerateValuesHandler*/int handler);
}

