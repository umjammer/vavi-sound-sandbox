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

    @Override
    public void connect() throws IOException {
    }

    @Override
    public PushBufferStream[] getStreams() {
        return streams;
    }

    public Format getStreamFormat() {
        return pbss[idx].getFormat();
    }

    @Override
    public void start() throws IOException {
        p.start();
        ds.start();
    }

    @Override
    public void stop() throws IOException {
    }

    @Override
    public Object getControl(String name) {
        // No controls
        return null;
    }

    @Override
    public Object[] getControls() {
        // No controls
        return new Control[0];
    }

    @Override
    public Time getDuration() {
        return ds.getDuration();
    }

    @Override
    public void disconnect() {
    }

    @Override
    public String getContentType() {
        return ContentDescriptor.RAW;
    }

    @Override
    public MediaLocator getLocator() {
        return ds.getLocator();
    }

    @Override
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

        @Override
        public void read(Buffer buf) /* throws IOException */ {
            // This wouldn't be used.
        }

        @Override
        public ContentDescriptor getContentDescriptor() {
            return new ContentDescriptor(ContentDescriptor.RAW);
        }

        @Override
        public boolean endOfStream() {
            return pbs.endOfStream();
        }

        @Override
        public long getContentLength() {
            return LENGTH_UNKNOWN;
        }

        @Override
        public Format getFormat() {
            return pbs.getFormat();
        }

        @Override
        public void setTransferHandler(BufferTransferHandler bth) {
            this.bth = bth;
        }

        @Override
        public Object getControl(String name) {
            // No controls
            return null;
        }

        @Override
        public Object[] getControls() {
            // No controls
            return new Control[0];
        }

        @Override
        public synchronized void transferData(PushBufferStream pbs) {
            if (bth != null) {
                bth.transferData(pbs);
            }
        }
    }
}

/* */
