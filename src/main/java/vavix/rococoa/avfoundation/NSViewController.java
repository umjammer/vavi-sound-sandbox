
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSObject;


public abstract class NSViewController extends NSObject {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("NSViewController", _Class.class);

    public interface _Class extends ObjCClass {
        NSViewController alloc();
    }

    public abstract void loadView();
}