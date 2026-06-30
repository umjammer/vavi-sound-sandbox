/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.dispatch;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import org.rococoa.ID;
import org.rococoa.ObjCBlock;
import org.rococoa.ObjCBlocks.BlockLiteral;


/**
 * Dispatch Framework.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-02-12 nsano initial version <br>
 */
public interface DispatchLibrary extends Library {

    DispatchLibrary library = Native.load("dispatch", DispatchLibrary.class);

//    NativeLibrary NATIVE_LIBRARY = NativeLibrary.getInstance("CoreFoundation");

    ID /* dispatch_queue_main_t */ dispatch_get_main_queue();

    ID /* dispatch_queue_global_t */ dispatch_get_global_queue(Pointer /* intptr_t */ identifier, Pointer /* uintptr_t */ flags);

    void dispatch_main();

    void dispatch_async(ID /* dispatch_queue_t */ queue, BlockLiteral dispatch_block_tblock);

    interface DispatchQueue extends ObjCBlock {
        void callback();
    }
}
