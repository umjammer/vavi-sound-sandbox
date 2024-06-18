/*
 * @(#)DataSource.java    1.2 01/03/13
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

package jmfsample.media.protocol.live;

import java.io.IOException;

import javax.media.Time;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;


public class DataSource extends PushBufferDataSource {

    protected Object[] controls = new Object[0];

    protected boolean started = false;

    protected String contentType = "raw";

    protected boolean connected = false;

    protected Time duration = DURATION_UNKNOWN;

    protected LiveStream[] streams = null;

    protected LiveStream stream = null;

    public DataSource() {
    }

    @Override
    public String getContentType() {
        if (!connected) {
            System.err.println("Error: DataSource not connected");
            return null;
        }
        return contentType;
    }

    @Override
    public void connect() throws IOException {
        if (connected)
            return;
        connected = true;
    }

    @Override
    public void disconnect() {
        try {
            if (started)
                stop();
        } catch (IOException e) {
        }
        connected = false;
    }

    @Override
    public void start() throws IOException {
        // we need to throw error if connect() has not been called
        if (!connected)
            throw new java.lang.Error("DataSource must be connected before it can be started");
        if (started)
            return;
        started = true;
        stream.start(true);
    }

    @Override
    public void stop() throws IOException {
        if ((!connected) || (!started))
            return;
        started = false;
        stream.start(false);
    }

    @Override
    public Object[] getControls() {
        return controls;
    }

    @Override
    public Object getControl(String controlType) {
        try {
            Class<?> cls = Class.forName(controlType);
            Object[] cs = getControls();
            for (Object c : cs) {
                if (cls.isInstance(c))
                    return c;
            }
            return null;

        } catch (Exception e) { // no such controlType or such control
            return null;
        }
    }

    @Override
    public Time getDuration() {
        return duration;
    }

    @Override
    public PushBufferStream[] getStreams() {
        if (streams == null) {
            streams = new LiveStream[1];
            stream = streams[0] = new LiveStream();
        }
        return streams;
    }
}
