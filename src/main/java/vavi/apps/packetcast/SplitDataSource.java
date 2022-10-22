package vavi.apps.packetcast;

import java.io.IOException;

import javax.media.Buffer;
import javax.media.Control;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.Time;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;


/**
 * The custom DataSource to split input.
 */
class SplitDataSource extends PushBufferDataSource {

    Processor p;

    PushBufferDataSource ds;

    PushBufferStream[] pbss;

    SplitStream[] streams;

    int idx;

    boolean done = false;

    public SplitDataSource(Processor p, int idx) {
        this.p = p;
        this.ds = (PushBufferDataSource) p.getDataOutput();
        this.idx = idx;
        pbss = ds.getStreams();
        streams = new SplitStream[1];
        streams[0] = new SplitStream(pbss[idx]);
    }

    public void connect() throws IOException {
    }

    public PushBufferStream[] getStreams() {
        return streams;
    }

    public Format getStreamFormat() {
        return pbss[idx].getFormat();
    }

    public void start() throws IOException {
        p.start();
        ds.start();
    }

    public void stop() throws IOException {
    }

    public Object getControl(String name) {
        // No controls
        return null;
    }

    public Object[] getControls() {
        // No controls
        return new Control[0];
    }

    public Time getDuration() {
        return ds.getDuration();
    }

    public void disconnect() {
    }

    public String getContentType() {
        return ContentDescriptor.RAW;
    }

    public MediaLocator getLocator() {
        return ds.getLocator();
    }

    public void setLocator(MediaLocator ml) {
        System.err.println("Not interested in a media locator");
    }

    /**
     * Utility Source stream for the SplitDataSource.
     */
    static class SplitStream implements PushBufferStream, BufferTransferHandler {

        PushBufferStream pbs;

        BufferTransferHandler bth;

        Format format;

        public SplitStream(PushBufferStream pbs) {
            this.pbs = pbs;
            pbs.setTransferHandler(this);
        }

        public void read(Buffer buf) /* throws IOException */ {
            // This wouldn't be used.
        }

        public ContentDescriptor getContentDescriptor() {
            return new ContentDescriptor(ContentDescriptor.RAW);
        }

        public boolean endOfStream() {
            return pbs.endOfStream();
        }

        public long getContentLength() {
            return LENGTH_UNKNOWN;
        }

        public Format getFormat() {
            return pbs.getFormat();
        }

        public void setTransferHandler(BufferTransferHandler bth) {
            this.bth = bth;
        }

        public Object getControl(String name) {
            // No controls
            return null;
        }

        public Object[] getControls() {
            // No controls
            return new Control[0];
        }

        public synchronized void transferData(PushBufferStream pbs) {
            if (bth != null) {
                bth.transferData(pbs);
            }
        }
    }
}

/* */
