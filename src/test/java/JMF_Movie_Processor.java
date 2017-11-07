import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.media.Buffer;
import javax.media.ConfigureCompleteEvent;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Duration;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.MediaTimeSetEvent;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.SizeChangeEvent;
import javax.media.Time;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.util.BufferToImage;
import javax.swing.JFileChooser;


public class JMF_Movie_Processor {

    MovieProcessor MP;

    Frame frm;

    int skip = 1;

    Rectangle theRoi = null;

    int firstF, lastF;

    boolean playing = false;

    boolean end_flag = false;

    int orientation = +1;

    int framecount;

    Panel p0, p1, p2;

    Button stepBW, stepFW;

    Button playF, playB, pauseB;

    Button goToAcqB;

    Scrollbar delaySB;

    int timeDelay = 25;

    public static void main(String[] args) {
        new JMF_Movie_Processor("");
    }

    JMF_Movie_Processor(String arg) {
        MP = new MovieProcessor();

        frm = new Frame("");
        frm.setLayout(new BorderLayout());

        /* Movie component */
        Component vc = MP.getVC();
        p0 = new Panel();
        p0.setLayout(new FlowLayout());
        if (vc != null) {
            // IJ.write("Adding visual component");
            p0.add(vc);
        }

        p1 = new Panel();
        p2 = new Panel();

        p2.setLayout(new FlowLayout());
        p1.setLayout(new GridLayout(2, 3, 6, 1));

        stepBW = new Button("Step BW");
        stepBW.addActionListener(actionListener);

        stepFW = new Button("Step FW");
        stepFW.addActionListener(actionListener);

        pauseB = new Button("Pause");
        pauseB.addActionListener(actionListener);

        playF = new Button("Play FW");
        playF.addActionListener(actionListener);

        playB = new Button("Play BW");
        playB.addActionListener(actionListener);

        goToAcqB = new Button("Go to Acqisitor");
        goToAcqB.addActionListener(actionListener);

        delaySB = new Scrollbar(Scrollbar.HORIZONTAL, 160, 4, 0, 1000);
        delaySB.addAdjustmentListener(adjustmentListener);

        p1.add(stepBW);
        p1.add(pauseB);
        p1.add(stepFW);

        p1.add(playB);
        p1.add(delaySB);
        p1.add(playF);

        p2.add(goToAcqB);

        frm.add("North", p0);
        frm.add("Center", p1);
        frm.add("South", p2);
        frm.pack();
        frm.setVisible(true);

        /* Listen to keyboard events */
        p0.addKeyListener(keyListener);
        frm.addWindowListener(windowListener);

        MP.getFrame(+1);
        MP.getFrame(-1);
        frm.setTitle("Movie Player - frame: " + MP.getPosition(1) + "/" + MP.getPosition(0));

        firstF = 0;
        lastF = MP.getPosition(0);
        p0.requestFocus();

        // Enter Play Movie Loop

        framecount = 0;
        long basetime = 0;
        double fps;
        int ifps;
        while (!end_flag) {
            if (playing) {
                framecount++;
                if (framecount == 1)
                    basetime = System.currentTimeMillis();

                int retval = MP.getFrame(orientation);
                frm.setTitle("Movie Player - frame: " + MP.getPosition(1) + "/" + MP.getPosition(0));
                // Delay between frames
                try {
                    Thread.sleep(timeDelay);
                } catch (InterruptedException e) {
                }
                if (framecount > 1) {
                    fps = 1000.0 * (framecount - 1) / (System.currentTimeMillis() - basetime);
                    ifps = (int) fps;
                    System.err.println(ifps + "fps, delay=" + timeDelay + "ms");
                }
                if (retval != 0)
                    playing = false;
            } else {
                framecount = 0;
                Thread.yield();
            }

        }
    }

    public void updateTitle(Frame frm) {
        frm.setTitle("Movie Player - frame: " + MP.getPosition(1) + "/" + MP.getPosition(0));
    }

    // LISTENERS...

    /** Key Listener */
    KeyListener keyListener = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();

            // IJ.write("Key Pressed!");

            switch (keyCode) {
            case KeyEvent.VK_PAGE_UP:
                MP.getFrame(-skip);
                break;
            case KeyEvent.VK_PAGE_DOWN:
                MP.getFrame(+skip);
                break;
            case KeyEvent.VK_END:
//                new JMF_Movie_Acquisitor(this, MP);
                break;
            }

            switch (e.getKeyChar()) {
            case '1':
                skip = 1;
                break;
            case '2':
                skip = 2;
                break;
            case '3':
                skip = 3;
                break;
            case '4':
                skip = 4;
                break;
            case '5':
                skip = 5;
                break;

            }

            frm.setTitle("Movie Player - frame: " + MP.getPosition(1) + "/" + MP.getPosition(0));
            p0.requestFocus();
        }
    };

    /** Action Listener (Buttons) */
    ActionListener actionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {

            Object b = e.getSource();

            if (b == playF) {
                playing = true;
                orientation = +1;
            }
            if (b == playB) {
                playing = true;
                orientation = -1;
            }
            if (b == pauseB) {
                playing = false;
            }
            if (b == stepFW) {
                playing = false;
                MP.getFrame(+1);
            }
            if (b == stepBW) {
                playing = false;
                MP.getFrame(-1);
            }
            if (b == goToAcqB) {
                playing = false;
//                new JMF_Movie_Acquisitor(this, MP);
            }

            frm.setTitle("Movie Player - frame: " + MP.getPosition(1) + "/" + MP.getPosition(0));
            p0.requestFocus();
        }
    };

    /** Adjustment Listener (Scroll bar) */
    AdjustmentListener adjustmentListener = new AdjustmentListener() {
        public void adjustmentValueChanged(AdjustmentEvent evt) {
            Object s = evt.getSource();
            {
                if (s.equals(delaySB)) {
                    timeDelay = delaySB.getValue() / 4;
                    framecount = 0; // To compute fps correctly
                }
            }
        }
    };

    /** Window Listener */
    WindowListener windowListener = new WindowAdapter() {
        public void windowClosing(WindowEvent we) {
            end_flag = true;
            frm.setVisible(false);
            frm.dispose();
        }

        public void windowClosed(java.awt.event.WindowEvent evt) {
            p0.removeKeyListener(keyListener);
            MP.kill();
        }
    };
}

/*******************************************************************************
 * MOVIE PROCESSOR USING Java Media Framework
 ******************************************************************************/

class MovieProcessor implements ControllerListener {

    Player p;

    MediaLocator ml;

    FramePositioningControl fpc;

    FrameGrabbingControl fgc;

    Object waitSync = new Object();

    boolean stateTransitionOK = true;

    int totalFrames = FramePositioningControl.FRAME_UNKNOWN;

    int currentFrame = 0;

    BufferToImage frameConverter;

    String name;

    boolean grayscale = false;

    Image img;

    public MovieProcessor() {

        JFileChooser od = new JFileChooser();
        int r = od.showDialog(null, "Open Movie...");
        if (r != JFileChooser.APPROVE_OPTION) {
            return;
        }
//      File dir = od.getCurrentDirectory();
        String path = od.getSelectedFile().getAbsolutePath();
        String url = encodeURL("file:///" + path);
System.err.println("url: " + url);
        if ((ml = new MediaLocator(url)) == null) {
            System.err.println("IJ: Cannot build media locator from: " + ml);
            return;
        }

        DataSource ds = null;
        // Create a DataSource given the media locator.
System.err.println("creating JMF data source");
// IJ.write("Current URL is " + url);
// IJ.write("Current full path is "+path);
        try {
            MediaLocator ml = new MediaLocator("file://" + path);
            // MediaLocator ml = new MediaLocator(url);
            // ds = new com.omnividea.media.protocol.file.DataSource(ml);
            ds = Manager.createDataSource(ml);
        } catch (Exception e) {
            System.err.println("IJ: Cannot create DataSource from: " + ml);
            return;
        }

        openMovie(ds);
    }

    public String encodeURL(String url) {
        int index = 0;
        while (index > -1) {
            index = url.indexOf(' ');
            if (index > -1) {
                url = url.substring(0, index) + "%20" + url.substring(index + 1, url.length());
            }
        }
        return url;
    }

    public boolean openMovie(DataSource ds) {

        System.err.println("opening: " + ds.getContentType());
        try {
            p = Manager.createPlayer(ds);
        } catch (Exception e) {
System.err.println("JMF Movie Reader: " + "Failed to create a player from the given DataSource:\n \n" + e.getMessage());
            return false;
        }

        p.addControllerListener(this);
        p.realize();
        if (!waitForState(Controller.Realized)) {
System.err.println("JMF Movie Reader: " + "Failed to realize the JMF player.");
            return false;
        }

        // Try to retrieve a FramePositioningControl from the player.
        fpc = (FramePositioningControl) p.getControl("javax.media.control.FramePositioningControl");
        if (fpc == null) {
System.err.println("JMF Movie Reader: " + "The player does not support FramePositioningControl.");
            return false;
        }

        // Try to retrieve a FrameGrabbingControl from the player.
        fgc = (FrameGrabbingControl) p.getControl("javax.media.control.FrameGrabbingControl");
        if (fgc == null) {
System.err.println("JMF Movie Reader: " + "The player does not support FrameGrabbingControl.");
            return false;
        }

        Time duration = p.getDuration();
        if (duration != Duration.DURATION_UNKNOWN) {
System.err.println("IJ: Movie duration: " + duration.getSeconds());
            // totalFrames = fpc.mapTimeToFrame(duration)+1;
            totalFrames = (int) (duration.getSeconds() * 25.0);
System.err.println("IJ: Movie duration: " + totalFrames + " frames.");
            if (totalFrames == FramePositioningControl.FRAME_UNKNOWN) {
System.err.println("IJ: The FramePositioningControl does not support mapTimeToFrame.");
            }
        } else {
System.err.println("IJ: Movie duration: unknown");
        }

        // Prefetch the player.
        p.prefetch();
        if (!waitForState(Controller.Prefetched)) {
System.err.println("JMF Movie Reader: " + "Failed to prefetch the player.");
            return false;
        }

        Buffer frame = fgc.grabFrame();
        VideoFormat format = (VideoFormat) frame.getFormat();
        frameConverter = new BufferToImage(format);

        img = frameConverter.createImage(frame);
        currentFrame = 0;

        return true;
    }

    //----

    /** */
    public Image getImageFrame1(int step) {
        int newpos = currentFrame + step;
        if (newpos < 0)
            newpos = 0;
        if (newpos > totalFrames - 1)
            newpos = totalFrames - 1;

        fpc.seek(newpos);
        currentFrame = newpos;

        Image img = frameConverter.createImage(fgc.grabFrame());

        return img;
    }

    /** */
    public Image getImageFrame(int step) {
        int newpos = currentFrame + step;

        if (newpos < 0) {
            step = -currentFrame;
            newpos = 0;
        }
        if (newpos > totalFrames - 1) {
            step = (totalFrames - 1) - currentFrame;
            newpos = totalFrames - 1;
        }

        int actualStep;
        actualStep = fpc.skip(step);
        if (actualStep != step) {
System.err.println("IJ: Could not skip as desired!");
        }
        currentFrame = newpos;

        // int currentFrame = fpc.mapTimeToFrame(p.getMediaTime());
        Image img = frameConverter.createImage(fgc.grabFrame());
        if (img == null) {
System.err.println("IJ: frameConverter.createImage FAILED!");
        }

        return img;
    }

    /** */
    public int getFrame(int step) {
        int newpos = currentFrame + step;
        int retval = 0;

        if (newpos < 0) {
            newpos = 0;
            retval = -1;
        }
        if (newpos > totalFrames - 1) {
            newpos = totalFrames - 1;
            retval = -1;
        }

        int actualStep;
        actualStep = fpc.skip(step);
        if (actualStep != step) {
            System.err.println("IJ: Could not skip as desired!");
        } else {
//System.err.ptinln("IJ: Have skipped "+actualStep+" steps.");
        }

        currentFrame = newpos;
//System.err.ptinln("IJ: Current position: "+getPosition(1));

        return retval;
    }

    /** */
    public int getPosition(int mode) {
        if (mode == 0) {
            return totalFrames - 1;
        } else {
            return currentFrame;
        }
    }

    /** */
    public void close() {
        p.close();
    }

    /** Block until the player has transitioned to the given state. */
    boolean waitForState(int state) {
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

    /** Controller Listener */
    public void controllerUpdate(ControllerEvent evt) {

        if (evt instanceof ConfigureCompleteEvent || evt instanceof RealizeCompleteEvent || evt instanceof PrefetchCompleteEvent) {
            synchronized (waitSync) {
                stateTransitionOK = true;
                waitSync.notifyAll();
            }
        } else if (evt instanceof ResourceUnavailableEvent) {
            synchronized (waitSync) {
                stateTransitionOK = false;
                waitSync.notifyAll();
            }
        } else if (evt instanceof EndOfMediaEvent) {
            p.setMediaTime(new Time(0));
        } else if (evt instanceof SizeChangeEvent) {
        } else if (evt instanceof MediaTimeSetEvent) {
            synchronized (waitSync) {
                waitSync.notifyAll();
            }
        }
    }

    public Component getVC() {
        return p.getVisualComponent();
    }

    public void kill() {
        p.stop();
        p.close();
    }
}

/* */
