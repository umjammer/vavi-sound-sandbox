/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.packetcast;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import javax.media.Controller;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaException;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;
import javax.swing.Timer;

import vavi.util.Debug;


/**
 * A sample program to cut an input file given the start and end points.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060127 nsano initial version <br>
 */
public class PacketCaster {

    /**
     * Main program
     */
    public static void main(String[] args) throws Exception {
        new PacketCaster(args);
    }

    /** */
    int interval;

    /** */
    PacketCaster(String[] args) throws Exception {
        String inputURL = new File(args[0]).toURI().toString();
//        String inputURL = args[0];

        // Generate the input and output media locators.
        MediaLocator iml = new MediaLocator(inputURL);

        interval = Integer.parseInt(args[1]);

        Timer timer = new Timer(interval, new ActionListener() {
            final UrlMaker urlMaker = new MyUrlMaker();
            long playingTime = 0;
            /** Capture for 10 seconds */
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    // Transcode with the specified parameters.
                    long start = playingTime * 1000 * 1000;
Debug.println("start: " + start);
                    playingTime += interval;
                    long end = playingTime * 1000 * 1000;
Debug.println("end: " + end);
                    String outputURL = urlMaker.getUrl();
                    MediaLocator oml = new MediaLocator(outputURL);
                    doIt(iml, oml, new long[] { start }, new long[] { end });
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        });
        timer.setInitialDelay(0);
        timer.start();
        Thread.sleep(10000);
    }

    /**
     * Given a source media locator, destination media locator and a start and
     * end point, this program cuts the pieces out.
     * @throws IOException
     * @throws MediaException
     */
    public void doIt(MediaLocator inML, MediaLocator outML, long[] start, long[] end) throws MediaException, IOException {

        Processor p;

System.err.println("- Create processor for: " + inML);
        p = Manager.createProcessor(inML);

System.err.println("- Configure the processor for: " + inML);
        if (!StateWaiter.waitForState(p, Processor.Configured)) {
            throw new IllegalStateException("Failed to configure the processor.");
        }

System.err.println("- Realize the processor for: " + inML);
        if (!StateWaiter.waitForState(p, Controller.Realized)) {
            throw new IllegalStateException("Failed to realize the processor.");
        }

        SuperCutDataSource ds = new SuperCutDataSource(p, inML, start, end);

        // Create the processor to generate the final output.
        p = Manager.createProcessor(ds);

        p.addControllerListener(controllerListener);

        // Put the Processor into configured state.
        if (!StateWaiter.waitForState(p, Processor.Configured)) {
            throw new IllegalStateException("Failed to configure the processor.");
        }

        // Set the output content descriptor on the final processor.
        ContentDescriptor cd = new FileTypeDescriptor(FileTypeDescriptor.WAVE);
System.err.println("- Set output content descriptor to: " + cd);
        if ((p.setContentDescriptor(cd)) == null) {
            throw new IllegalStateException("Failed to set the output content descriptor on the processor.");
        }

        // We are done with programming the processor. Let's just
        // realize and prefetch it.
        if (!StateWaiter.waitForState(p, Controller.Prefetched)) {
            throw new IllegalStateException("Failed to realize the processor.");
        }

        // Now, we'll need to create a DataSink.
        DataSink dsink = Manager.createDataSink(p.getDataOutput(), outML);
        dsink.open();

        dsink.addDataSinkListener(dataSinkListener);
        fileDone = false;

System.err.println("- Start cutting...");

        // OK, we can now start the actual concatenation.
        p.start();
        dsink.start();

        // Wait for EndOfStream event.
        waitForFileDone();

        // Cleanup.
        dsink.close();
        p.removeControllerListener(controllerListener);

System.err.println("  ...done cutting.");
    }

    /**
     * Controller Listener.
     */
    ControllerListener controllerListener = evt -> {

        if (evt instanceof ControllerErrorEvent) {
            System.err.println("Failed to cut the file.");
            System.exit(-1);
        } else if (evt instanceof EndOfMediaEvent) {
            evt.getSourceController().close();
        }
    };

    final Object waitFileSync = new Object();

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
        System.err.println();
        return fileSuccess;
    }

    /**
     * Event handler for the file writer.
     */
    DataSinkListener dataSinkListener = evt -> {

        if (evt instanceof EndOfStreamEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                waitFileSync.notifyAll();
System.err.print("O");
            }
        } else if (evt instanceof DataSinkErrorEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                fileSuccess = false;
                waitFileSync.notifyAll();
System.err.print("X");
            }
        }
    };

    /** Get the file corresponding to the current second {@link #interval} [mesc] */
    class MyUrlMaker implements UrlMaker {
        /** */
        Calendar calendar = Calendar.getInstance();
        /** */
        MyUrlMaker() {
            calendar.roll(Calendar.SECOND, 10);
        }
        /** Time currently in use */
        int currentNo = 0;

        /** */
        @Override
        public String getUrl() {
            //
            if (currentNo == 0) {
                currentNo = (calendar.get(Calendar.SECOND)) / (interval / 1000) + 1;
            } else {
                currentNo++;
                if (currentNo > 6) {
                    currentNo = 1;
                }
            }
            String url = "file://" + System.getProperty("user.dir") + "/tmp/0000" + currentNo + ".wav";
//System.err.println("url: " + url);
            return url;
        }
    }

    /** */
    interface UrlMaker {
        /** */
        String getUrl();
    }
}
