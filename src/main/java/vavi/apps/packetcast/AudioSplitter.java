/*
 * @(#)Split.java    1.4 01/03/13
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

package vavi.apps.packetcast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.media.Controller;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.control.TrackControl;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;


/**
 * A sample program to split an input media file with multiplexed audio and
 * video tracks into files of individual elementary track.
 */
public class AudioSplitter {

    SplitDataSource[] splitDS;

    final Object fileSync = new Object();

    boolean allDone = false;

    /**
     * Main program
     */
    public static void main(String[] args) throws Exception {
long t = System.currentTimeMillis();
        new AudioSplitter(args[0]);
System.err.println("+ all takes " + (System.currentTimeMillis() - t) + " ms");
        System.exit(0);
    }

    AudioSplitter(String inputURL) throws Exception {

        // Generate the input media locators.
        MediaLocator iml;

        if ((iml = createMediaLocator(inputURL)) == null) {
            throw new IllegalArgumentException("Cannot build media locator from: " + inputURL);
        }

        // Trancode with the specified parameters.
        doIt(iml);
    }

    /**
     * Splits the tracks from a multiplexed input.
     */
    void doIt(MediaLocator inML) {

        Processor p;

System.err.println("- Create processor for: " + inML);
        try {
            p = Manager.createProcessor(inML);
        } catch (Exception e) {
            throw new IllegalArgumentException("Yikes!  Cannot create a processor from the given url: " + e);
        }

System.err.println("- Configure the processor for: " + inML);
        if (!StateWaiter.waitForState(p, Processor.Configured)) {
            throw new IllegalStateException("Failed to configure the processor.");
        }

        // If the input is an MPEG file, we'll first convert that to
        // raw audio and video.
        ContentDescriptor cd = fileExtToCD(inML.getRemainder());
        if (cd != null && FileTypeDescriptor.MPEG.equals(cd.getEncoding())) {
            transcodeMPEGToRaw(p);
        }

System.err.println("- Realize the processor for: " + inML);
        if (!StateWaiter.waitForState(p, Controller.Realized)) {
            throw new IllegalStateException("Failed to realize the processor.");
        }

        // Get the output data streams from the first processor.
        // Create a SplitDataSource for each of these elementary stream.
        PushBufferDataSource pbds = (PushBufferDataSource) p.getDataOutput();
        PushBufferStream[] pbs = pbds.getStreams();
        splitDS = new SplitDataSource[pbs.length];

        allDone = false;
        boolean atLeastOne = false;

        // Create a file writer for each SplitDataSource to generate
        // the resulting media file.
        for (int i = 0; i < pbs.length; i++) {
            if (pbs[i].getFormat() instanceof AudioFormat) {
System.err.println("+ split audio: " + pbs[i].getFormat());
                splitDS[i] = new SplitDataSource(p, i);
                String name = "file:" + System.getProperty("user.dir") + File.separator + "split" + splitDS[i].idx;
                if ((new FileWriter(name + ".wav")).write(splitDS[i])) {
                    atLeastOne = true;
                }
            } else {
System.err.println("+ skip video: " + pbs[i].getFormat());
            }
        }

        if (!atLeastOne) {
            throw new IllegalStateException("Failed to split any of the tracks.");
        }

System.err.println("- Start splitting...");

        waitForFileDone();

System.err.println("  ...done splitting.");
    }

    void waitForFileDone() {
System.err.print("  ");
        synchronized (fileSync) {
            while (!allDone) {
                try {
                    fileSync.wait(1000);
System.err.print(".");
                } catch (Exception e) {
                }
            }
        }
        System.err.println();
    }

    /**
     * Transcode the MPEG audio to linear and video to JPEG so we can do the
     * splitting.
     */
    static void transcodeMPEGToRaw(Processor p) {

        TrackControl[] tc = p.getTrackControls();
        AudioFormat af;

        for (int i = 0; i < tc.length; i++) {
System.err.println("tc[" + i + "]: " + tc[i].getFormat());
            if (tc[i].getFormat() instanceof VideoFormat) {
                tc[i].setFormat(new VideoFormat(VideoFormat.JPEG));
            } else if (tc[i].getFormat() instanceof AudioFormat) {
                af = (AudioFormat) tc[i].getFormat();
                tc[i].setFormat(new AudioFormat(AudioFormat.LINEAR, af.getSampleRate(), af.getSampleSizeInBits(), af.getChannels()));
            }
        }
    }

    /**
     * Convert a file name to a content type. The extension is parsed to
     * determine the content type.
     */
    static ContentDescriptor fileExtToCD(String name) {

        String ext;
        int p;

        // Extract the file extension.
//System.err.println("name: " + name);
        if ((p = name.lastIndexOf('.')) < 0) {
            return null;
        }

        ext = (name.substring(p + 1)).toLowerCase();
//System.err.println("ext: " + ext);

        String type = null;

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

    static String getMimeType(String name) {
        try {
            Class<?> clazz = Class.forName("com.sun.media.MimeManager");
            Method method = clazz.getMethod("getMimeType", String.class);
            return (String) method.invoke(null, name);
        } catch (Exception e) {
            return null;
        }
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
            ml = new MediaLocator("file:" + url);
            return ml;
        } else {
            String file = "file:" + System.getProperty("user.dir") + File.separator + url;
            ml = new MediaLocator(file);
            return ml;
        }
    }

    /**
     * Given a DataSource, creates a DataSink and generate a file.
     */
    class FileWriter implements ControllerListener, DataSinkListener {

        Processor p;

        SplitDataSource ds;

        DataSink dsink;

        String name;

        FileWriter(String name) {
            this.name = name;
        }

        boolean write(SplitDataSource ds) {

            this.ds = ds;

            // Create the processor to generate the final output.
            try {
                p = Manager.createProcessor(ds);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create a processor to concatenate the inputs.");
            }

            p.addControllerListener(this);

            // Put the Processor into configured state.
            if (!StateWaiter.waitForState(p, Processor.Configured)) {
                throw new IllegalStateException("Failed to configure the processor.");
            }

            ContentDescriptor cd = fileExtToCD(name);
            // Set the output content descriptor on the final processor.
System.err.println("- Set output content descriptor to: " + cd);
            if ((p.setContentDescriptor(cd)) == null) {
                throw new IllegalStateException("Failed to set the output content descriptor on the processor.");
            }

            // We are done with programming the processor. Let's just
            // realize and prefetch it.
            if (!StateWaiter.waitForState(p, Controller.Prefetched)) {
                throw new IllegalStateException("Failed to realize the processor.");
            }

            MediaLocator oml;
            if ((oml = createMediaLocator(name)) == null) {
                throw new IllegalStateException("Cannot build media locator from: " + name);
            }
System.err.println("- name: " + name);

            // Now, we'll need to create a DataSink.
            if ((dsink = createDataSink(p, oml)) == null) {
                throw new IllegalStateException("Failed to create a DataSink for the given output MediaLocator: " + oml);
            }

            dsink.addDataSinkListener(this);

            // OK, we can now start the actual concatenation.
            try {
                p.start();
                dsink.start();
            } catch (IOException e) {
                throw new IllegalStateException("IO error during concatenation");
            }

            return true;
        }

        /**
         * Controller Listener.
         */
        @Override
        public void controllerUpdate(ControllerEvent evt) {

            if (evt instanceof ControllerErrorEvent) {
                System.err.println("Failed to split the file.");
                System.exit(-1);
            } else if (evt instanceof EndOfMediaEvent) {
                evt.getSourceController().close();
            }
        }

        /**
         * Event handler for the file writer.
         */
        @Override
        public void dataSinkUpdate(DataSinkEvent evt) {

            if (evt instanceof EndOfStreamEvent || evt instanceof DataSinkErrorEvent) {

                // Cleanup.
                try {
                    dsink.close();
                } catch (Exception e) {
                }
                p.removeControllerListener(this);
                ds.done = true;
                doneFile();
            }
        }

        /**
         * Create the DataSink.
         */
        static DataSink createDataSink(Processor p, MediaLocator outML) {

            DataSource ds;

            if ((ds = p.getDataOutput()) == null) {
                System.err.println("Something is really wrong: the processor does not have an output DataSource");
                return null;
            }

            DataSink dsink;

            try {
                System.err.println("- Create DataSink for: " + outML);
                dsink = Manager.createDataSink(ds, outML);
                dsink.open();
            } catch (Exception e) {
                System.err.println("Cannot create the DataSink: " + e);
                return null;
            }

            return dsink;
        }

        /**
         * Callback from the FileWriter when a DataSource is done.
         */
        void doneFile() {
            synchronized (fileSync) {
                for (SplitDataSource splitD : splitDS) {
                    if (splitD != null && !splitD.done) {
                        return;
                    }
                }

                // All done.
                allDone = true;
                fileSync.notify();
            }
        }
    }
}
