
package vavi.apps.packetcast;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;

import javax.swing.JFrame;


class VideoEncoder {
    /** */
    MovieProcessor mp;

    /** */
    Component vc;

    /** */
    int skip = 1;

    /** */
    int firstFrameNo, lastFrameNo;

    /** */
    int orientation = +1;

    /** */
    int timeDelay = 25;

    /** TODO multiple */
    private ModelListener modelListener;

    /** */
    VideoEncoder(String arg) {
        this.mp = new MovieProcessor(arg);

        /* Movie component */
        this.vc = mp.getVC();
        // vc.setPreferredSize(); // TODO
    }

    void start() {
        mp.getFrame(+1);
        mp.getFrame(-1);
        modelListener.updated();

        firstFrameNo = 0;
        lastFrameNo = mp.getPosition(0);
    }

    /** */
    void stop() {
        mp.kill();
    }

    /** */
    public void stepForeward() {
        mp.getFrame(+1);
        modelListener.updated();
    }

    /** */
    public void stepForeward(int step) {
        mp.getFrame(+step);
        modelListener.updated();
    }

    /** */
    public void stepBackward() {
        mp.getFrame(-1);
        modelListener.updated();
    }

    /** */
    public void stepBackward(int step) {
        mp.getFrame(-step);
        modelListener.updated();
    }

    /** TODO type is bad argument */
    public int getPosition(int type) {
        return mp.getPosition(type);
    }

    /** */
    public Component getComponent() {
        return vc;
    }

    /** */
    public Image getImage() {
        return mp.getCurrentImage();
    }

    /** */
    interface ModelListener {
        void updated();
    }

    /** */
    public void addModelListener(ModelListener listener) {
        this.modelListener = listener;
    }

    // ----

    /** */
    public static void main(String[] args) {
        final JFrame frm = new JFrame();
        frm.setLayout(new BorderLayout());
        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final VideoEncoder ve = new VideoEncoder(args[0]);
        ve.addModelListener(new ModelListener() {
            public void updated() {
                frm.setTitle("Movie Player - frame: " + ve.getPosition(1) + "/" + ve.getPosition(0));
            }
        });

        frm.add(ve.getComponent());
        frm.pack();
        frm.setVisible(true);
        frm.requestFocus();

        ve.start();
        for (int i = 0; i < 100; i++) {
            ve.stepForeward(5);
            Image image = ve.getImage();
System.err.println("image at " + ve.getPosition(1) + ": " + image);
        }
        ve.stop();

        frm.setVisible(false);
        frm.dispose();
    }
}

/* */
