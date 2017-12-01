/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package jmf;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.media.ConfigureCompleteEvent;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.MediaTimeSetEvent;
import javax.media.NoDataSinkException;
import javax.media.NoProcessorException;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.StopAtTimeEvent;
import javax.media.Time;
import javax.media.control.TrackControl;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import vavi.util.Debug;


/**
 * A sample program to transcode an input source to an output location with
 * different data formats.
 */
public class Transcoder {

    /**
     *
     * @param inML
     * @param outML
     * @param fmts
     * @param start start time
     * @param end end time
     */
    public void doIt(MediaLocator inML, MediaLocator outML, Format fmts[], int start, int end) throws NoProcessorException, IOException, NoDataSinkException {

        Processor processor = Manager.createProcessor(inML);
        processor.addControllerListener(controllerListener);
        // Put the Processor into configured state.
        processor.configure();
        if (!waitForState(processor, Processor.Configured)) {
            throw new IllegalStateException("Failed to configure the processor.");
        }

        // Set the output content descriptor based on the media locator.
        setContentDescriptor(processor, outML);

        // OpasTest the tracks to the given output formats.
        setTrackFormats(processor, fmts);

        // We are done with programming the processor. Let's just
        // realize the it.
        processor.realize();
        if (!waitForState(processor, Controller.Realized)) {
            throw new IllegalStateException("Failed to realize the processor.");
        }

        // Now, we'll need to create a DataSink.
        DataSink dataSink = createDataSink(processor, outML);
        if (dataSink == null) {
            throw new IllegalStateException("Failed to create a DataSink for the given output MediaLocator: " + outML);
        }

        dataSink.addDataSinkListener(dataSinkListener);
        fileDone = false;

        // Set the start time if there's one set.
        if (start > 0) {
            processor.setMediaTime(new Time((double) start));
        }

        // Set the stop time if there's one set.
        if (end > 0) {
            processor.setStopTime(new Time((double) end));
        }

System.err.println("start transcoding...");

        // OK, we can now start the actual transcoding.
        processor.start();
        dataSink.start();

        // Wait for EndOfStream event.
        waitForFileDone();

        // Cleanup.
        dataSink.close();
        processor.removeControllerListener(controllerListener);

Debug.println("...done transcoding.");
    }

    /**
     * Set the content descriptor based on the given output MediaLocator.
     */
    private void setContentDescriptor(Processor processor, MediaLocator outML) {

        ContentDescriptor cd = fileExtToCD(outML.getRemainder());

        // If the output file maps to a content type,
        // we'll try to set it on the processor.

        if (cd != null) {

Debug.println("- set content descriptor to: " + cd);

            if ((processor.setContentDescriptor(cd)) == null) {

                // The processor does not support the output content
                // type. But we can set the content type to RAW and
                // see if any DataSink supports it.

                processor.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW));
            }
        }
    }

    /**
     * Set the target transcode format on the processor.
     */
    private void setTrackFormats(Processor p, Format fmts[]) {

        if (fmts.length == 0) {
            return;
        }

        TrackControl tcs[] = p.getTrackControls();
        if (tcs == null) {
            throw new IllegalStateException("The Processor cannot transcode the tracks to the given formats");
        }

        for (int i = 0; i < fmts.length; i++) {

Debug.println("- set track format to: " + fmts[i]);

            if (!setEachTrackFormat(p, tcs, fmts[i])) {
                throw new IllegalStateException("Cannot transcode any track to: " + fmts[i]);
            }
        }
    }

    /**
     * Convert a file name to a content type. The extension is parsed to
     * determine the content type.
     */
    private ContentDescriptor fileExtToCD(String name) {

        String ext;
        int p;

        // Extract the file extension.
        if ((p = name.lastIndexOf('.')) < 0) {
            return null;
        }

        ext = (name.substring(p + 1)).toLowerCase();

        String type;
        // Use the MimeManager to get the mime type from the file extension.
        if (ext.equals("mp3")) {
            type = FileTypeDescriptor.MPEG_AUDIO;
        } else {
            if ((type = getMimeType(ext)) == null) {
                return null;
            }
            type = ContentDescriptor.mimeTypeToPackageName(type);
        }

        return new FileTypeDescriptor(type);
    }

    String getMimeType(String name) {
        try {
            Class<?> clazz = Class.forName("com.sun.media.MimeManager");
            Method method = clazz.getMethod("getMimeType", String.class);
            return String.class.cast(method.invoke(null, name));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * We'll loop through the tracks and try to find a track that can be
     * converted to the given format.
     */
    private boolean setEachTrackFormat(Processor p, TrackControl tcs[], Format fmt) {

        Format supported[];
        Format f;

// for (int i = 0; i < tcs.length; i++) {
//  supported = tcs[i].getSupportedFormats();
//  for (int j = 0; j < supported.length; j++) {
//   System.err.println("audio format: " + supported[j]);
//  }
// }

        for (int i = 0; i < tcs.length; i++) {

            supported = tcs[i].getSupportedFormats();

            if (supported == null) {
                continue;
            }

            for (int j = 0; j < supported.length; j++) {

                if (fmt.matches(supported[j]) && (f = fmt.intersects(supported[j])) != null && tcs[i].setFormat(f) != null) {

                    // Success.
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Create the DataSink.
     * @throws IOException
     * @throws NoDataSinkException
     */
    private DataSink createDataSink(Processor p, MediaLocator outML) throws IOException, NoDataSinkException {

        DataSource ds;

        if ((ds = p.getDataOutput()) == null) {
            throw new IllegalStateException("Something is really wrong: the processor does not have an output DataSource");
        }

System.err.println("- create DataSink for: " + outML);
        DataSink dsink = Manager.createDataSink(ds, outML);
        dsink.open();

        return dsink;
    }

    /** */
    private Object waitSync = new Object();

    /** */
    private boolean stateTransitionOK = true;

    /**
     * Block until the processor has transitioned to the given state. Return
     * false if the transition failed.
     */
    private boolean waitForState(Processor p, int state) {
        synchronized (waitSync) {
            try {
                while (p.getState() < state && stateTransitionOK) {
                    waitSync.wait();
                }
            } catch (Exception e) {
            }
        }
        return stateTransitionOK;
    }

    /**
     * Controller Listener.
     */
    private ControllerListener controllerListener = new ControllerListener() {
        public void controllerUpdate(ControllerEvent event) {

            if (event instanceof ConfigureCompleteEvent ||
                event instanceof RealizeCompleteEvent ||
                event instanceof PrefetchCompleteEvent) {
                synchronized (waitSync) {
                    stateTransitionOK = true;
                    waitSync.notifyAll();
                }
            } else if (event instanceof ResourceUnavailableEvent) {
                synchronized (waitSync) {
                    stateTransitionOK = false;
                    waitSync.notifyAll();
                }
            } else if (event instanceof EndOfMediaEvent) {
                event.getSourceController().close();
            } else if (event instanceof MediaTimeSetEvent) {
System.err.println("- mediaTime set: " + ((MediaTimeSetEvent) event).getMediaTime().getSeconds());
            } else if (event instanceof StopAtTimeEvent) {
System.err.println("- stop at time: " + ((StopAtTimeEvent) event).getMediaTime().getSeconds());
                event.getSourceController().close();
            }
        }
    };

    /** */
    private Object waitFileSync = new Object();

    /** */
    private boolean fileDone = false;

    /** */
    private boolean fileSuccess = true;

    /**
     * Block until file writing is done.
     */
    private boolean waitForFileDone() {
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

    /**
     * Event handler for the file writer.
     */
    private DataSinkListener dataSinkListener = new DataSinkListener() {
        public void dataSinkUpdate(DataSinkEvent event) {

            if (event instanceof EndOfStreamEvent) {
                synchronized (waitFileSync) {
                    fileDone = true;
                    waitFileSync.notifyAll();
                }
            } else if (event instanceof DataSinkErrorEvent) {
                synchronized (waitFileSync) {
                    fileDone = true;
                    fileSuccess = false;
                    waitFileSync.notifyAll();
                }
            }
        }
    };

    /**
     * @param args [options]
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) throws Exception {

        String encoding = null;
        int channels = 1;
        int samplingRate = 8000;

        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("samplingRate")
                          .hasArg()
                          .withDescription("sampling rate in Hz")
                          .create("r"));
        options.addOption(OptionBuilder.withArgName("channels")
                          .hasArg()
                          .withDescription("number of channels")
                          .create("c"));
        options.addOption(OptionBuilder.withArgName("encoding")
                          .hasArg()
                          .withDescription("encoding")
                          .create("e"));

        CommandLineParser parser = new BasicParser();
        CommandLine cl = parser.parse(options, args);

        String inputURL = cl.getArgs()[0];
        String outputURL = cl.getArgs()[1];

        if (cl.hasOption("r")) {
            samplingRate = Integer.parseInt(cl.getOptionValue("r"));
Debug.println("samplingRate: " + samplingRate);
        }
        if (cl.hasOption("c")) {
            channels = Integer.parseInt(cl.getOptionValue("c"));
Debug.println("channels: " + channels);
        }
        if (cl.hasOption("e")) {
            encoding = cl.getOptionValue("e");
Debug.println("encoding: " + encoding);
        }

        Format fmt = new AudioFormat(encoding,                      // encoding
                                     samplingRate,                  // sampleRate
                                     AudioFormat.NOT_SPECIFIED,     // sampleSizeInBits
                                     channels,                      // channels
                                     AudioFormat.NOT_SPECIFIED,     // endian
                                     AudioFormat.NOT_SPECIFIED);    // signed

        // Generate the input and output media locators.
        MediaLocator iml, oml;

        if ((iml = createMediaLocator(inputURL)) == null) {
Debug.println("Cannot build media locator from: " + inputURL);
            System.exit(0);
        }

        if ((oml = createMediaLocator(outputURL)) == null) {
Debug.println("Cannot build media locator from: " + outputURL);
            System.exit(0);
        }

        // Trancode with the specified parameters.
        Transcoder transcode = new Transcoder();

        transcode.doIt(iml, oml, new Format[] { fmt }, 0, 0);
Debug.println("Transcoding failed");

        System.exit(0);
    }

    /**
     * Create a media locator from the given string.
     */
    static MediaLocator createMediaLocator(String url) {

        MediaLocator ml;

        if (url.indexOf(":") > 0 && (ml = new MediaLocator(url)) != null) {
            return ml;
        }

        if (url.startsWith(File.separator)) {
            if ((ml = new MediaLocator("file:" + url)) != null) {
                return ml;
            }
        } else {
            String file = "file:" + System.getProperty("user.dir") + File.separator + url;
            if ((ml = new MediaLocator(file)) != null) {
                return ml;
            }
        }

        return null;
    }
}

/* */
