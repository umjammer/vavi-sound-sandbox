package vavi.apps.packetcast;
/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Processor;


/**
 * Utility class to block until a certain state had reached.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2006/03/27 nsano initial version <br>
 */
class StateWaiter {

    Processor p;

    boolean error = false;

    StateWaiter(Processor p) {
        this.p = p;
        p.addControllerListener(new ControllerListener() {
            public void controllerUpdate(ControllerEvent ce) {
                if (ce instanceof ControllerErrorEvent) {
                    error = true;
                }
                synchronized (this) {
                    notifyAll();
                }
            }
        });
    }

    synchronized boolean waitForState(int state) {

        switch (state) {
        case Processor.Configured:
            p.configure();
            break;
        case Processor.Realized:
            p.realize();
            break;
        case Processor.Prefetched:
            p.prefetch();
            break;
        case Processor.Started:
            p.start();
            break;
        }

        while (p.getState() < state && !error) {
            try {
                wait(1000);
            } catch (Exception e) {
            }
        }
        // p.removeControllerListener(this);
        return !(error);
    }

    /**
     * Block until the given processor has transitioned to the given state.
     * Return false if the transition failed.
     */
    public static boolean waitForState(Processor p, int state) {
        return (new StateWaiter(p)).waitForState(state);
    }

    Object waitFileSync = new Object();

    boolean fileDone = false;

    boolean fileSuccess = true;

    /**
     * Block until file writing is done.
     */
    boolean waitForFileDone() {
System.err.print("  ");
        synchronized (waitFileSync) {
            try {
                while (!fileDone) {
                    waitFileSync.wait(1000);
                    System.err.print(".");
                }
            } catch (Exception e) {
            }
        }
System.err.println("");
        return fileSuccess;
    }
}

/* */
