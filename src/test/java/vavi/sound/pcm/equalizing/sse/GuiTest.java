/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.equalizing.sse;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.SoundUtil.volume;


/**
 * Graphical equalizer GUI for {@link Equalizer}.
 * <p>
 * Sliders run from 0 (mute) to 96 (0 dB, no attenuation); left and right
 * channels are linked. Moving a slider re-builds the equalizer table, which
 * is picked up seamlessly by the running player.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060419 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class GuiTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "sse.in")
    String inFile = "src/test/resources/test.wav";

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    /** */
    public static void main(String[] args) throws Exception {
        GuiTest app = new GuiTest();
        app.setup();
        app.exec();
    }

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @Disabled("what's this?")
    void clipTest() {
        for (int i = 0; i < 97; i++) {
            System.err.println(Math.pow(10, i / -20.0));
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
        exec();
        Thread.sleep(Long.MAX_VALUE); // keep the gui alive when run as a test
    }

    /** */
    void exec() throws Exception {
Debug.println(inFile);

        equalizer = new Equalizer(14);

        Model model = new Model();
        model.bands = Equalizer.getBandsCount();
        model.band = new double[model.bands];
        for (int i = 0; i < model.bands; i++) {
            model.band[i] = Equalizer.getBand(i);
        }
        // [bands] is the segment from the top band up to fs
        model.lgains = new double[model.bands + 1];
        model.rgains = new double[model.bands + 1];

        SwingUtilities.invokeLater(() -> new View(model));
    }

    /** */
    static class Model {
        int bands;
        double[] band;
        /** linear gains fed to {@link Equalizer#equ_makeTable}, [0..bands] */
        double[] lgains;
        double[] rgains;
    }

    /** */
    Equalizer equalizer;

    /** */
    volatile boolean playing;

    /** */
    class View {
        /** */
        final Model model;
        /** */
        final JSlider[] lgains;
        final JSlider[] rgains;
        /** */
        final JSlider lpremain;
        final JSlider rpremain;
        /** */
        final JButton playButton;

        /** slider value 96 ~ 0 (top ~ bottom) maps to 0 ~ -96 dB, 0 means mute */
        static final int RANGE = 96;

        /** */
        View(Model model) {
            this.model = model;

            lgains = new JSlider[model.bands];
            rgains = new JSlider[model.bands];

            JFrame frame = new JFrame();
            frame.setTitle("Equalizer - " + new File(inFile).getName());
            frame.setLayout(new BorderLayout());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel bpanel = new JPanel();
            playButton = new JButton();
            playButton.setText("Play");
            playButton.addActionListener(event -> {
                if (playing) {
                    stop();
                } else {
                    play();
                }
            });
            bpanel.add(playButton);

            JPanel lpanel = new JPanel(new GridLayout(1, model.bands + 1));
            JPanel rpanel = new JPanel(new GridLayout(1, model.bands + 1));

            lpremain = slider("lpremain", Color.pink);
            lpanel.add(labeled(lpremain, "L pre"));
            rpremain = slider("rpremain", Color.pink);
            rpanel.add(labeled(rpremain, "R pre"));

            for (int i = 0; i < model.bands; i++) {
                lgains[i] = slider("lgains" + i, null);
                lpanel.add(labeled(lgains[i], hz(model.band[i])));
                rgains[i] = slider("rgains" + i, null);
                rpanel.add(labeled(rgains[i], hz(model.band[i])));
            }

            frame.add(lpanel, BorderLayout.NORTH);
            frame.add(rpanel, BorderLayout.SOUTH);
            frame.add(bpanel, BorderLayout.EAST);
            frame.pack();
            frame.setVisible(true);

            doEqualize();
        }

        /** */
        JSlider slider(String name, Color color) {
            JSlider slider = new JSlider(SwingConstants.VERTICAL, 0, RANGE, RANGE);
            slider.setName(name);
            if (color != null) {
                slider.setBackground(color);
            }
            slider.addChangeListener(changeListener);
            slider.addMouseListener(mouseInputListener);
            return slider;
        }

        /** */
        static JPanel labeled(JSlider slider, String text) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(slider, BorderLayout.CENTER);
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            panel.add(label, BorderLayout.SOUTH);
            return panel;
        }

        /** */
        static String hz(double freq) {
            return freq >= 1000 ? String.format("%.1fk", freq / 1000) : String.format("%.0f", freq);
        }

        /** update the equalizer table after a slider drag is finished */
        final MouseInputListener mouseInputListener = new MouseInputAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                doEqualize();
            }
        };

        /** left and right channels are linked */
        final ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                String name = ((Component) event.getSource()).getName();
                int value = ((JSlider) event.getSource()).getValue();
                if (name.startsWith("lgains")) {
                    int index = Integer.parseInt(name.substring(6));
                    mirror(rgains[index], value);
                } else if (name.startsWith("rgains")) {
                    int index = Integer.parseInt(name.substring(6));
                    mirror(lgains[index], value);
                } else if (name.equals("lpremain")) {
                    mirror(rpremain, value);
                } else if (name.equals("rpremain")) {
                    mirror(lpremain, value);
                } else {
                    assert false : name;
                }
            }

            void mirror(JSlider other, int value) {
                other.removeChangeListener(this);
                other.setValue(value);
                other.addChangeListener(this);
            }
        };

        /** slider value to linear gain, 0 mutes */
        static double gain(JSlider slider) {
            int value = slider.getValue();
            return value == 0 ? 0 : Math.pow(10, (value - RANGE) / 20.0);
        }

        /** */
        void doEqualize() {
            double lpre = gain(lpremain);
            double rpre = gain(rpremain);

            for (int i = 0; i < model.bands; i++) {
                model.lgains[i] = lpre * gain(lgains[i]);
                model.rgains[i] = rpre * gain(rgains[i]);
            }
            // the segment above the top band follows the top band slider
            model.lgains[model.bands] = model.lgains[model.bands - 1];
            model.rgains[model.bands] = model.rgains[model.bands - 1];

            equalizer.equ_makeTable(model.lgains, model.rgains, new ArrayList<>(), 44100);
        }

        /** */
        void play() {
            playing = true;
            playButton.setText("Stop");
            Thread thread = new Thread(player, "player");
            thread.setDaemon(true);
            thread.start();
        }

        /** */
        void stop() {
            playing = false;
            playButton.setText("Play");
        }
    }

    /** loops the input file until stopped, filtering through the equalizer */
    final Runnable player = new Runnable() {
        @Override
        public void run() {
            SourceDataLine line = null;
            try {
                while (playing) {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(new File(inFile));
                    AudioFormat format = ais.getFormat();
Debug.println(format);
                    if (line == null) {
                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                        line = (SourceDataLine) AudioSystem.getLine(info);
                        line.open(format);
                        volume(line, volume);
                        line.start();
                    }
                    byte[] buf = new byte[8192];
                    int l;
                    while (playing && (l = ais.read(buf, 0, buf.length)) > 0) {
                        int m = equalizer.equ_modifySamples(buf, l / 4, 2, 16);
                        line.write(buf, 0, m * 4);
                    }
                    ais.close();
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            } finally {
                if (line != null) {
                    line.stop();
                    line.close();
                }
            }
        }
    };
}
