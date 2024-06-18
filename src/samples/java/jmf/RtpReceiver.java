package jmf;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Calendar;

import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerAdapter;
import javax.media.ControllerErrorEvent;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.control.TrackControl;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.swing.Timer;

import vavi.apps.packetcast.SuperCutDataSource;
import vavi.util.Debug;


/**
 * RtpReceiver. Receiver class for RTP packets.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 0510156 nsano initial version <br>
 */
public class RtpReceiver {
    Processor processor;
    Timer timer;
    MediaLocator sourceML;
    /** in msec */
    int interval;
    long playingTime = 0;
    public static void main(String[] args) throws Exception {
        new RtpReceiver(new File(args[0]).toURI().toString(), Integer.parseInt(args[1]));
    }
    /**
     * @param url
     * @param interval in sec
     */
    public RtpReceiver(String url, int interval) throws Exception {
        this.interval = interval * 1000;
Debug.println("url: " + url + ", " + this.interval);
        sourceML = new MediaLocator(url);
        processor = Manager.createProcessor(sourceML);
        processor.addControllerListener(new ControllerAdapter() {
            @Override
            public void configureComplete(ConfigureCompleteEvent event) {
                // Set the output content type and realize the processor

                checkTrackFormats(processor);
                processor.realize();
            }
            @Override
            public void realizeComplete(RealizeCompleteEvent event) {
System.err.println("start play");
                timer.setInitialDelay(0);
                timer.start();
            }
            @Override
            public void endOfMedia(EndOfMediaEvent event) {
System.err.println("stop play");
                timer.stop();
                event.getSourceController().close();
                System.exit(0);
            }
            @Override
            public void controllerError(ControllerErrorEvent event) {
                System.err.println(event);
                System.exit(-1);
            }
        });

        timer = new Timer(this.interval, new ActionListener() {
            final UrlMaker urlMaker = new MyUrlMaker();
            Processor outProcessor;
            /** Capture for 10 seconds */
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    long start = playingTime * 1000 * 1000;
Debug.println("start: " + start);
                    playingTime += RtpReceiver.this.interval;
                    long end = playingTime * 1000 * 1000;
Debug.println("end: " + end);

                    // get the output of the processor
                    DataSource dataSource = new SuperCutDataSource(processor, sourceML, new long[] { start }, new long[] { end });

                    outProcessor = Manager.createProcessor(dataSource);
                    outProcessor.addControllerListener(new ControllerAdapter() {
                        @Override
                        public void configureComplete(ConfigureCompleteEvent event) {
                            outProcessor.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.WAVE));
                            outProcessor.realize();
Debug.println("outProcessor: configured");
                        }
                        @Override
                        public void realizeComplete(RealizeCompleteEvent event) {
                            outProcessor.prefetch();
Debug.println("outProcessor: realized");
                        }
                        @Override
                        public void prefetchComplete(PrefetchCompleteEvent event) {
                            try {
                                // create a File protocol MediaLocator with the location of the
                                // file to which the data is to be written
                                String url = urlMaker.getUrl();
                                MediaLocator destMediaLocator = new MediaLocator(url);
Debug.println("out: " + url);
                                // create a datasink to do the file writing & open the sink to
                                // make sure we can write to it.
                                DataSink dest = Manager.createDataSink(outProcessor.getDataOutput(), destMediaLocator);
                                dest.open();
                                dest.addDataSinkListener(dataSinkListener);
                                fileDone = false;

                                outProcessor.start();
                                // now start the filewriter and processor
                                dest.start();

                                // Wait for EndOfStream event.
                                waitForFileDone();

                                // Wait for an EndOfStream from the DataSink and close it...
                                dest.close();
Debug.println("fileSuccess: " + fileSuccess);
                                outProcessor.removeControllerListener(this);
                            } catch (Exception e) {
                                e.printStackTrace(System.err);
                            }
                        }
                        @Override
                        public void endOfMedia(EndOfMediaEvent event) {
                            event.getSourceController().close();
                        }
                        @Override
                        public void controllerError(ControllerErrorEvent event) {
                            System.err.println(event);
                        }
                    });
                    outProcessor.configure();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        });

        // Configure the processor
        processor.configure();
    }

    /** */
    private final Object waitFileSync = new Object();

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
Debug.println(e);
            }
        }
System.err.println();
        return fileSuccess;
    }

    /**
     * Event handler for the file writer.
     */
    private final DataSinkListener dataSinkListener = event -> {

        if (event instanceof EndOfStreamEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                waitFileSync.notifyAll();
System.err.print("O");
            }
        } else if (event instanceof DataSinkErrorEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                fileSuccess = false;
                waitFileSync.notifyAll();
System.err.print("X");
            }
        }
    };

    /**
     * Transcode the MPEG audio to linear and video to JPEG so
     * we can do the cutting.
     */
    void checkTrackFormats(Processor p) {

        TrackControl[] tc = p.getTrackControls();
        VideoFormat mpgVideo = new VideoFormat(VideoFormat.MPEG);
        AudioFormat rawAudio = new AudioFormat(AudioFormat.LINEAR);

        for (TrackControl trackControl : tc) {
            Format preferred = null;

            if (trackControl.getFormat().matches(mpgVideo)) {
                preferred = new VideoFormat(VideoFormat.JPEG);
            } else if (trackControl.getFormat() instanceof AudioFormat && !trackControl.getFormat().matches(rawAudio)) {
                preferred = rawAudio;
            }

            if (preferred != null) {
                Format[] supported = trackControl.getSupportedFormats();
                Format selected = null;

                for (Format format : supported) {
                    if (format.matches(preferred)) {
                        selected = format;
                        break;
                    }
                }

                if (selected != null) {
                    System.err.println("  Transcode:");
                    System.err.println("     from: " + trackControl.getFormat());
                    System.err.println("     to: " + selected);
                    trackControl.setFormat(selected);
                }
            }
        }
    }

    /** Get the file corresponding to the current second {@link #interval} [mesc] */
    class MyUrlMaker implements UrlMaker {
        /** */
        Calendar calendar = Calendar.getInstance();
        /** */
        MyUrlMaker() {
            calendar.roll(Calendar.SECOND, 10);
        }
        /** time currently in use */
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
