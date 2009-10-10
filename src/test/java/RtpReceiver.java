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
import javax.media.datasink.DataSinkEvent;
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
 * RtpReceiver RTPパケットの受信機クラス
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
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
        new RtpReceiver(new File(args[0]).toURL().toString(), Integer.parseInt(args[1]));
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
            public void configureComplete(ConfigureCompleteEvent event) {
                // Set the output content type and realize the processor

                checkTrackFormats(processor);
                processor.realize();
            }
            public void realizeComplete(RealizeCompleteEvent event) {
System.err.println("start play");
                timer.setInitialDelay(0);
                timer.start();
            }
            public void endOfMedia(EndOfMediaEvent event) {
System.err.println("stop play");
                timer.stop();
                event.getSourceController().close();
                System.exit(0);
            }
            public void controllerError(ControllerErrorEvent event) {
                System.err.println(event);
                System.exit(-1);
            }
        });

        timer = new Timer(this.interval, new ActionListener() {
            UrlMaker urlMaker = new MyUrlMaker();
            Processor outProcessor;
            /** Capture for 10 seconds */
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
                        public void configureComplete(ConfigureCompleteEvent event) {
                            outProcessor.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.WAVE));
                            outProcessor.realize();
Debug.println("outProcessor: configured");
                        }
                        public void realizeComplete(RealizeCompleteEvent event) {
                            outProcessor.prefetch();
Debug.println("outProcessor: realized");
                        }
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
                        public void endOfMedia(EndOfMediaEvent event) {
                            event.getSourceController().close();
                        }
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
Debug.println(e);
            }
        }
System.err.println();
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
        }
    };

    /**
     * Transcode the MPEG audio to linear and video to JPEG so
     * we can do the cutting.
     */
    void checkTrackFormats(Processor p) {

        TrackControl tc[] = p.getTrackControls();
        VideoFormat mpgVideo = new VideoFormat(VideoFormat.MPEG);
        AudioFormat rawAudio = new AudioFormat(AudioFormat.LINEAR);

        for (int i = 0; i < tc.length; i++) {
            Format preferred = null;

            if (tc[i].getFormat().matches(mpgVideo)) {
                preferred = new VideoFormat(VideoFormat.JPEG);
            } else if (tc[i].getFormat() instanceof AudioFormat && !tc[i].getFormat().matches(rawAudio)) {
                preferred = rawAudio;
            }

            if (preferred != null) {
                Format supported[] = tc[i].getSupportedFormats();
                Format selected = null;

                for (int j = 0; j < supported.length; j++) {
                    if (supported[j].matches(preferred)) {
                        selected = supported[j];
                        break;
                    }
                }

                if (selected != null) {
                    System.err.println("  Transcode:");
                    System.err.println("     from: " + tc[i].getFormat());
                    System.err.println("     to: " + selected);
                    tc[i].setFormat(selected);
                }
            }
        }
    }

    /** 現在の秒に対応するファイルを取得 {@link #interval} [mesc] おき */
    class MyUrlMaker implements UrlMaker {
        /** */
        Calendar calendar = Calendar.getInstance();
        /** */
        MyUrlMaker() {
            calendar.roll(Calendar.SECOND, 10);
        }
        /** 現在使用中の時間 */
        int currentNo = 0;

        /** */
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

/* */
