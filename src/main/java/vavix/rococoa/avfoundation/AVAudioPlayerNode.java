/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import org.rococoa.ObjCBlock;
import org.rococoa.ObjCBlocks.BlockLiteral;
import org.rococoa.ObjCClass;


/**
 * @interface AVAudioPlayerNode : AVAudioNode
 * <p>
 * the "push" entry point of an {@link AVAudioEngine} graph. buffers are handed over by
 * {@link #scheduleBuffer(AVAudioPCMBuffer, BlockLiteral)} and the completion handler tells
 * us when the player is done with one, which is the only back pressure signal we need to
 * let core audio drive the clock.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-08 nsano initial version <br>
 */
public abstract class AVAudioPlayerNode extends AVAudioNode {

    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioPlayerNode", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioPlayerNode alloc();
    }

    public abstract AVAudioPlayerNode init();

    public static AVAudioPlayerNode newInstance() {
        return CLASS.alloc().init();
    }

    /**
     * {@code void (^)(void)}, called once the player has consumed the buffer.
     * it is invoked on an internal thread, never on the render thread.
     */
    public interface CompletionHandler extends ObjCBlock {
        void complete(BlockLiteral block);
    }

    /** @param completionHandler {@link CompletionHandler} */
    public abstract void scheduleBuffer_completionHandler(AVAudioPCMBuffer buffer, BlockLiteral completionHandler);

    /** @param completionHandler {@link CompletionHandler} */
    public void scheduleBuffer(AVAudioPCMBuffer buffer, BlockLiteral completionHandler) {
        scheduleBuffer_completionHandler(buffer, completionHandler);
    }

    /** the engine must be running, otherwise this is a no-op. */
    public abstract void play();

    public abstract void pause();

    /** unschedules every pending buffer, their completion handlers are called. */
    public abstract void stop();

    public abstract boolean isPlaying();

    /**
     * preallocates the resources needed to render {@code frameCount} frames,
     * to avoid allocation on the first render.
     */
    public abstract void prepareWithFrameCount(int frameCount);
}
