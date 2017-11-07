/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.packetcast;

import java.awt.Component;
import java.awt.Image;

import javax.media.Buffer;
import javax.media.ConfigureCompleteEvent;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Duration;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.MediaTimeSetEvent;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.SizeChangeEvent;
import javax.media.Time;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.util.BufferToImage;


/**
 * MovieProcessor.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2006/04/12 nsano initial version <br>
 */
class MovieProcessor implements ControllerListener {

    /** */
    private Player player;
    /** */
    private FramePositioningControl fpc;
    /** */
    private FrameGrabbingControl fgc;
    /** */
    private Object waitSync = new Object();
    /** */
    private boolean stateTransitionOK = true;
    /** */
    private int totalFrames = FramePositioningControl.FRAME_UNKNOWN;
    /** */
    private int currentFrame = 0;
    /** */
    private BufferToImage frameConverter;

    /** */
    public MovieProcessor(String url) {

        MediaLocator ml;
System.err.println("url: " + url);
        if ((ml = new MediaLocator(url)) == null) {
            throw new IllegalArgumentException("Cannot build media locator from: " + ml);
        }

        DataSource ds = null;
        // Create a DataSource given the media locator.
System.err.println("creating JMF data source");
        try {
            ds = Manager.createDataSource(ml);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create DataSource from: " + ml);
        }

System.err.println("opening: " + ds.getContentType());
        try {
            player = Manager.createPlayer(ds);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create a player from the given DataSource:\n \n" + e.getMessage());
        }

        player.addControllerListener(this);
        player.realize();
        if (!waitForState(Controller.Realized)) {
            throw new IllegalStateException("Failed to realize the JMF player.");
        }

        // Try to retrieve a FramePositioningControl from the player.
        fpc = (FramePositioningControl) player.getControl("javax.media.control.FramePositioningControl");
        if (fpc == null) {
            throw new IllegalStateException("The player does not support FramePositioningControl.");
        }

        // Try to retrieve a FrameGrabbingControl from the player.
        fgc = (FrameGrabbingControl) player.getControl("javax.media.control.FrameGrabbingControl");
        if (fgc == null) {
            throw new IllegalStateException("The player does not support FrameGrabbingControl.");
        }

        Time duration = player.getDuration();
        if (duration != Duration.DURATION_UNKNOWN) {
System.err.println("Movie duration: " + duration.getSeconds());
            // totalFrames = fpc.mapTimeToFrame(duration)+1;
            totalFrames = (int) (duration.getSeconds() * 25.0);
System.err.println("Movie duration: " + totalFrames + " frames.");
            if (totalFrames == FramePositioningControl.FRAME_UNKNOWN) {
System.err.println("The FramePositioningControl does not support mapTimeToFrame.");
            }
        } else {
System.err.println("Movie duration: unknown");
        }

        // Prefetch the player.
        player.prefetch();
        if (!waitForState(Controller.Prefetched)) {
            throw new IllegalStateException("Failed to prefetch the player.");
        }

        Buffer frame = fgc.grabFrame();
        VideoFormat format = (VideoFormat) frame.getFormat();
        frameConverter = new BufferToImage(format);

        Image image = frameConverter.createImage(frame);
System.err.println("image at frame 0: " + image);
        currentFrame = 0;
    }

    // ----

    /** */
    public Image getImageFrame1(int step) {
        int newpos = currentFrame + step;
        if (newpos < 0) {
            newpos = 0;
        }
        if (newpos > totalFrames - 1) {
            newpos = totalFrames - 1;
        }

        fpc.seek(newpos);
        currentFrame = newpos;

        return getCurrentImage();
    }

    /** */
    public Image getImageFrame(int step) {
        int newpos = currentFrame + step;

        if (newpos < 0) {
            step = -currentFrame;
            newpos = 0;
        }
        if (newpos > totalFrames - 1) {
            step = (totalFrames - 1) - currentFrame;
            newpos = totalFrames - 1;
        }

        int actualStep;
        actualStep = fpc.skip(step);
        if (actualStep != step) {
System.err.println("Could not skip as desired!");
        }

        currentFrame = newpos;

        return getCurrentImage();
    }

    /** */
    public Image getCurrentImage() {
        Image image = frameConverter.createImage(fgc.grabFrame());
        if (image == null) {
System.err.println("frameConverter.createImage FAILED!");
        }

        return image;
    }

    /** */
    public int getFrame(int step) {
        int newpos = currentFrame + step;
        int retval = 0;

        if (newpos < 0) {
            newpos = 0;
            retval = -1;
        }
        if (newpos > totalFrames - 1) {
            newpos = totalFrames - 1;
            retval = -1;
        }

        int actualStep;
        actualStep = fpc.skip(step);
        if (actualStep != step) {
System.err.println("Could not skip as desired!");
        }

        currentFrame = newpos;

        return retval;
    }

    /** */
    public int getPosition(int mode) {
        if (mode == 0) {
            return totalFrames - 1;
        } else {
            return currentFrame;
        }
    }

    /** */
    public void close() {
        player.close();
    }

    /** Block until the player has transitioned to the given state. */
    boolean waitForState(int state) {
        synchronized (waitSync) {
            try {
                while (player.getState() < state && stateTransitionOK) {
                    waitSync.wait();
                }
            } catch (Exception e) {
            }
        }
        return stateTransitionOK;
    }

    /** Controller Listener */
    public void controllerUpdate(ControllerEvent evt) {

        if (evt instanceof ConfigureCompleteEvent || evt instanceof RealizeCompleteEvent || evt instanceof PrefetchCompleteEvent) {
            synchronized (waitSync) {
                stateTransitionOK = true;
                waitSync.notifyAll();
            }
        } else if (evt instanceof ResourceUnavailableEvent) {
            synchronized (waitSync) {
                stateTransitionOK = false;
                waitSync.notifyAll();
            }
        } else if (evt instanceof EndOfMediaEvent) {
            player.setMediaTime(new Time(0));
        } else if (evt instanceof SizeChangeEvent) {
        } else if (evt instanceof MediaTimeSetEvent) {
            synchronized (waitSync) {
                waitSync.notifyAll();
            }
        }
    }

    /** */
    public Component getVC() {
        return player.getVisualComponent();
    }

    /** */
    public void kill() {
        player.stop();
        player.close();
    }
}

/* */
