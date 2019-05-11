/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.equalizing.sse;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import org.junit.jupiter.api.Disabled;

import vavi.sound.pcm.equalizing.sse.Equalizer.Parameter;


/**
 * Graphical GUI Test.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060419 nsano initial version <br>
 */
@Disabled
public class Test2 {

    String inFile;
    String outFile = "tmp/out.vavi.wav";

    /** */
    public static void main(String[] args) throws Exception {
System.setOut(new PrintStream("NUL")); // shut fuckin' j-ogg's mouth
        new Test2();
    }

    /** */
    Test2() throws Exception {
        Properties props = new Properties();
        props.load(EqualizerTest.class.getResourceAsStream("/vavi/sound/pcm/equalizing/sse/local.properties"));
        inFile = props.getProperty("equalizer.in.wav");

        equalizer = new Equalizer(14);

        // max 0 ~ 96 min, [0] is preamp
        int[] lslpos = new int[19], rslpos = new int[19];

        for (int i = 0; i <= 18; i++) {
            lslpos[i] = Integer.parseInt(props.getProperty("lslpos." + i));
            rslpos[i] = Integer.parseInt(props.getProperty("rslpos." + i));
        }

        //----

        // min0 ~ 1.0 max ???
        double[] lbands = new double[19];
        double[] rbands = new double[19];
        lbands[18] = Math.pow(10, 0 / -20.0);
        rbands[18] = Math.pow(10, 0 / -20.0);
        List<Parameter> params = new ArrayList<>();

        double lpreamp = lslpos[0] == 96 ? 0 : Math.pow(10, lslpos[0] / -20.0);
        double rpreamp = rslpos[0] == 96 ? 0 : Math.pow(10, rslpos[0] / -20.0);

System.err.println("---- init ----");
        for (int i = 0; i < equalizer.getBandsCount(); i++) {
            //
            Parameter param = new Parameter();
            lbands[i] = lslpos[i + 1] == 96 ? 0 : lpreamp * Math.pow(10, lslpos[i + 1] / -20.0);
            param.left = true;
            param.right = false;
            param.gain = lbands[i];
            param.lower = i == 0 ? 0 : equalizer.getBand(i - 1);
            param.upper = i == equalizer.getBandsCount()  - 1 ? 44100 : equalizer.getBand(i);
System.err.println(param);
            params.add(param);
            //
            rbands[i] = rslpos[i + 1] == 96 ? 0 : rpreamp * Math.pow(10, rslpos[i + 1] / -20.0);
            param.left = false;
            param.right = true;
            param.gain = rbands[i];
            param.lower = i == 0 ? 0 : equalizer.getBand(i - 1);
            param.upper = i == equalizer.getBandsCount()  - 1 ? 44100 : equalizer.getBand(i);
System.err.println(param);
            params.add(param);
        }
System.err.println("---- init ----");

        equalizer.equ_makeTable(lbands, rbands, params, 44100);

        //
        Model model = new Model();
        model.bands = equalizer.getBandsCount();
System.err.println("bands: " + model.bands);
        model.rgains = new double[model.bands + 1];
        model.lgains = new double[model.bands + 1];
        model.band = new double[model.bands];
        for (int i = 0; i < model.bands; i++) {
            model.band[i] = equalizer.getBand(i);
        }

        View view = new View(model);
        view.hashCode();

        Thread thread = new Thread(player);
        thread.start();
    }

    /** */
    class Model {
        int bands;
        double[] band;
        double[] lgains;
        double[] rgains;
        boolean rock;
        double max;
        double min;
        double lpremain;
        double rpremain;
    }

    /** */
    Equalizer equalizer;

    /** */
    class View {
        /** */
        Model model;
        /** */
        JSlider[] lgains, rgains;
        /** */
        JSlider lpremain, rpremain;
        /** */
        View(Model model) {
            this.model = model;

            lgains = new JSlider[model.bands];
            rgains = new JSlider[model.bands];

            JFrame frame = new JFrame();
            frame.setTitle("Equalizer");
            frame.setLayout(new BorderLayout());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel bpanel = new JPanel();
            JButton playButton = new JButton();
            playButton.addActionListener(actionListener);
            playButton.setText("Play");
//          JButton stopButton = new JButton();
//          stopButton.setText("Stop");
            bpanel.add(playButton);
//          bpanel.add(stopButton);

            JPanel lpanel = new JPanel();
//          JPanel cpanel = new JPanel();
            JPanel rpanel = new JPanel();
            lpremain = new JSlider(JSlider.VERTICAL);
            lpremain.setBackground(Color.pink);
            lpremain.setMinimum(0);
            lpremain.setMaximum(96);
            lpremain.setValue(96);
            lpremain.setName("lpremain");
            lpremain.addChangeListener(changeListener);
            lpremain.addMouseListener(mouseInputListener);
            lpanel.add(lpremain);
            rpremain = new JSlider(JSlider.VERTICAL);
            rpremain.setBackground(Color.pink);
            rpremain.setMinimum(0);
            rpremain.setMaximum(96);
            rpremain.setValue(96);
            rpremain.setName("rpremain");
            rpremain.addChangeListener(changeListener);
            rpremain.addMouseListener(mouseInputListener);
            rpanel.add(rpremain);
            for (int i = 0; i < model.bands; i++) {
                lgains[i] = new JSlider(JSlider.VERTICAL);
                lgains[i].setMinimum(0);
                lgains[i].setMaximum(96);
                lgains[i].setValue(96);
                lgains[i].setName("lgains" + i);
                lgains[i].addChangeListener(changeListener);
                lgains[i].addMouseListener(mouseInputListener);
                lpanel.add(lgains[i]);
                rgains[i] = new JSlider(JSlider.VERTICAL);
                rgains[i].setMinimum(0);
                rgains[i].setMaximum(96);
                rgains[i].setValue(96);
                rgains[i].setName("rgains" + i);
                rgains[i].addChangeListener(changeListener);
                rgains[i].addMouseListener(mouseInputListener);
                rpanel.add(rgains[i]);
//              JLabel label = new JLabel();
//              label.setText(String.valueOf(equalizer.getBand(i)));
//              cpanel.add(label);
            }
            frame.add(lpanel, BorderLayout.NORTH);
//          frame.add(cpanel, BorderLayout.CENTER);
            frame.add(rpanel, BorderLayout.SOUTH);
            frame.add(bpanel, BorderLayout.EAST);
            frame.pack();
            frame.setVisible(true);
        }

        MouseInputListener mouseInputListener = new MouseInputAdapter() {
            public void mouseReleased(MouseEvent event) {
                doEqualize();
            }
        };

        ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                String name = ((Component) event.getSource()).getName();
                int value = ((JSlider) event.getSource()).getValue();
                if (name.startsWith("lgain")) {
                    int index = Integer.parseInt(name.substring(6));
                    rgains[index].removeChangeListener(this);
                    rgains[index].setValue(value);
                    rgains[index].addChangeListener(this);
                } else if (name.startsWith("rgain")) {
                    int index = Integer.parseInt(name.substring(6));
                    lgains[index].removeChangeListener(this);
                    lgains[index].setValue(value);
                    lgains[index].addChangeListener(this);
                } else if (name.startsWith("lpremain")) {
                    rpremain.removeChangeListener(this);
                    rpremain.setValue(value);
                    rpremain.addChangeListener(this);
                } else if (name.startsWith("rpremain")) {
                    lpremain.removeChangeListener(this);
                    lpremain.setValue(value);
                    lpremain.addChangeListener(this);
                } else {
                    assert false;
                }
System.err.println(name + ": " + value);
            }
        };

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    doEqualize();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        };

        void doEqualize() {
            //
            List<Parameter> params = new ArrayList<>();

            Parameter param = new Parameter();
            model.lpremain = (lpremain.getMaximum() - lpremain.getValue()) == 96 ? 0 : Math.pow(10, model.lpremain / -20.0);
            param.gain = model.lpremain;
            params.add(param);
//            param = new Parameter();
//            model.rpremain = (rpremain.getMaximum() - rpremain.getValue()) == 96 ? 0 : Math.pow(10, model.rpremain / -20.0);
//            param.gain = model.rpremain;
//            params.add(param);

            for (int i = 0; i < model.bands; i++) {
                param = new Parameter();
                model.lgains[i] = (lgains[i].getMaximum() - lgains[i].getValue()) == 96 ? 0 : Math.pow(10, model.lgains[i] / -20.0);
                param.left = true;
                param.right = true;
                param.gain = model.lgains[i];
                params.add(param);
//                model.rgains[i] = (rgains[i].getMaximum() - rgains[i].getValue()) == 96 ? 0 : Math.pow(10, model.rgains[i] / -20.0);
//                param.left = false;
//                param.right = true;
//                param.gain = model.rgains[i];
//                params.add(param);
            }

            equalizer.equ_makeTable(model.lgains, model.rgains, params, 44100);
        }
    }

    /** */
    Runnable player = new Runnable() {
        public void run() {
            while (true) {
                try {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(new File(inFile));
                    AudioFormat format = ais.getFormat();
System.err.println(format);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(format);
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .3d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
                    line.start();
                    byte[] buf = new byte[1024];
                    int l;
                    while (ais.available() > 0) {
                        l = ais.read(buf, 0, 1024);
                        int m = equalizer.equ_modifySamples(buf, l / 4, 2, 16);
                        line.write(buf, 0, m * 4);
//System.err.println("r: " + l + ", w: " + m * 4);
                    }
                    line.drain();
                    line.stop();
                    line.close();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    };
}

/* */
