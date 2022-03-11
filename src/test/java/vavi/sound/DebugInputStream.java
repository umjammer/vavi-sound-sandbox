/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import vavi.util.Debug;


/**
 * problem detector for silent SPI factory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/20 umjammer initial version <br>
 */
public class DebugInputStream extends FilterInputStream {

    static final boolean debug = false;

    public DebugInputStream(InputStream in) {
        super(in);
if (debug)
 Debug.println("@@@@@@@@@@@@@@@@@@@@ available: " + ava());
    }

    int ava() {
        try {
            return in.available();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
if (debug)
 Debug.println("@@@@@@@@@@@@@@@@@@@@ available: " + ava() + ", mark: " + readlimit);
        super.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
if (debug)
 printStackTrace(new Exception("@@@@@@@@@@@@@@@@@@@@ available: " + ava() + ", markSupported: " + super.markSupported()));
        return super.markSupported();
    }

    @Override
    public int read() throws IOException {
if (debug)
 printStackTrace(new Exception("@@@@@@@@@@@@@@@@@@@@ available: " + ava()));
        return super.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
if (debug)
 printStackTrace(new Exception("@@@@@@@@@@@@@@@@@@@@ available: " + ava()));
        return super.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
if (debug)
 printStackTrace(new Exception("@@@@@@@@@@@@@@@@@@@@ available: " + ava()));
        return super.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
if (debug)
 printStackTrace(new Exception("@@@@@@@@@@@@@@@@@@@@ close"));
        super.close();
    }

    static final String regex = "org\\.(eclipse|junit)\\.[\\d.#]+";

    static void printStackTrace(Throwable e) {
        PrintStream ps = System.err;
        ps.printf("%s: %s%n", e.getClass().getName(), e.getMessage());
        StackTraceElement[] stes = e.getStackTrace();
        AtomicBoolean f = new AtomicBoolean(true);
        Arrays.stream(stes).forEach(ste -> {
            String cm = ste.getClassName() + "#" + ste.getMethodName();
            if (
                f.get() && (
                !cm.startsWith("org.junit") &&
                !cm.startsWith("sun.reflect") &&
                !cm.startsWith("java.lang.reflect") &&
                !cm.startsWith("java.util.ArrayList#forEach") &&
                !cm.startsWith("vavi.sound.DebugInputStream") &&
                !cm.startsWith("org.eclipse")
                )) {
                ps.printf("\tat %s.%s(%s:%d)%n",
                              ste.getClassName(),
                              ste.getMethodName(),
                              ste.getFileName(),
                              ste.getLineNumber());
            }
            if (cm.contains(System.getProperty("test"))) {
                f.set(false);
            }
        });
    }
}

/* */
