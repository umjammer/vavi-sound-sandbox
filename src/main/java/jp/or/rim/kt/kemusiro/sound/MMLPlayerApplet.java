/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.sound.sampled.LineEvent;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

import jp.or.rim.kt.kemusiro.sound.tone.FMParameter;


/**
 * MMLを演奏するクラス。アプレットとしてもアプリケーションとしても
 * 動作する。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.3 $
 */
public class MMLPlayerApplet extends JApplet {

    private Container container;
    private JTextField ch1;
    private JTextField ch2;
    private JTextField ch3;
    private JRadioButton[] algorithmButton;
    private int currentAlgorithm;
    private JButton[] onOffStatus;
    private JTextField[] multiplier;
    private JTextField[] attackRate;
    private JTextField[] decayRate;
    private JTextField[] sustainRate;
    private JTextField[] releaseRate;
    private JTextField[] sustainLevel;
    private JTextField[] maxLevel;
    private JButton buttonPlay;
    private JButton buttonStop;
    private MMLPlayer player;
    private JLabel messageLabel;
    private Logger printer;

    @Override
    public void init() {
        player = new MMLPlayer(e -> {
            if (e.getType() == LineEvent.Type.STOP) {
                printer.info("please input MML strings!");
            }
        });

        container = this.getContentPane();

        Box baseBox = Box.createVerticalBox();
        container.add(baseBox);

        JLabel titleLabel = new JLabel("MML Player");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        baseBox.add(titleLabel);

        Box box1 = Box.createHorizontalBox();
        box1.add(new JLabel("Channel1", JLabel.LEFT));
        ch1 = new JTextField();
        ch1.setMaximumSize(new Dimension(480, 20));
        box1.add(ch1);

        Box box2 = Box.createHorizontalBox();
        box2.add(new JLabel("Channel2", JLabel.LEFT));
        ch2 = new JTextField();
        ch2.setMaximumSize(new Dimension(480, 20));
        box2.add(ch2);

        Box box3 = Box.createHorizontalBox();
        box3.add(new JLabel("Channel3", JLabel.LEFT));
        ch3 = new JTextField();
        ch3.setMaximumSize(new Dimension(480, 20));
        box3.add(ch3);

        Box algorithmBox = Box.createHorizontalBox();

        ButtonGroup bg = new ButtonGroup();
        algorithmButton = new JRadioButton[8];
        algorithmBox.add(new JLabel("Algorithm:"));
        algorithmBox.add(Box.createHorizontalGlue());
        currentAlgorithm = 4;
        for (int i = 0; i < 8; i++) {
            algorithmButton[i] = new JRadioButton(Integer.toString(i), i == currentAlgorithm);
            algorithmButton[i].addActionListener(new RadioButtonListener());
            bg.add(algorithmButton[i]);
            algorithmBox.add(algorithmButton[i]);
            algorithmBox.add(Box.createHorizontalGlue());
        }

        Box operatorGroupBox = Box.createHorizontalBox();
        onOffStatus = new JButton[4];
        multiplier = new JTextField[4];
        attackRate = new JTextField[4];
        decayRate = new JTextField[4];
        sustainRate = new JTextField[4];
        releaseRate = new JTextField[4];
        sustainLevel = new JTextField[4];
        maxLevel = new JTextField[4];
        for (int i = 0; i < 4; i++) {
            Box operatorBox = Box.createVerticalBox();
            operatorGroupBox.add(operatorBox);
            JPanel operatorPanel = new JPanel(new GridLayout(8, 2, 2, 2));
            operatorBox.add(operatorPanel);
            operatorPanel.setBorder(new EtchedBorder());

            JLabel title = new JLabel("OP" + i);
            operatorPanel.add(title);

            onOffStatus[i] = new JButton("ON");
            operatorPanel.add(onOffStatus[i]);
            onOffStatus[i].addActionListener(new ButtonListener());

            JLabel mul = new JLabel("multiplier");
            operatorPanel.add(mul);
            multiplier[i] = new JTextField("1.0");
            operatorPanel.add(multiplier[i]);

            JLabel ar = new JLabel("AR");
            operatorPanel.add(ar);
            attackRate[i] = new JTextField("20.0");
            operatorPanel.add(attackRate[i]);

            JLabel dr = new JLabel("DR");
            operatorPanel.add(dr);
            decayRate[i] = new JTextField("7.0");
            operatorPanel.add(decayRate[i]);

            JLabel sr = new JLabel("SR");
            operatorPanel.add(sr);
            sustainRate[i] = new JTextField("0.8");
            operatorPanel.add(sustainRate[i]);

            JLabel rr = new JLabel("RR");
            operatorPanel.add(rr);
            releaseRate[i] = new JTextField("4.0");
            operatorPanel.add(releaseRate[i]);

            JLabel sl = new JLabel("SL");
            operatorPanel.add(sl);
            sustainLevel[i] = new JTextField("0.8");
            operatorPanel.add(sustainLevel[i]);

            JLabel ml = new JLabel("MAX");
            operatorPanel.add(ml);
            maxLevel[i] = new JTextField("1.0");
            operatorPanel.add(maxLevel[i]);
        }

        JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonPlay = new JButton("Play");
        panel1.add(buttonPlay);
        buttonStop = new JButton("Stop");
        panel1.add(buttonStop);

        buttonPlay.addActionListener(new ButtonListener());
        buttonStop.addActionListener(new ButtonListener());

        JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout(FlowLayout.LEFT));
        messageLabel = new JLabel("please input MML strings!");
//        printer = new LabelPrinter(messageLabel);
        panel5.add(messageLabel);

        baseBox.add(box1);
        baseBox.add(box2);
        baseBox.add(box3);
        baseBox.add(algorithmBox);
        baseBox.add(operatorGroupBox);
        baseBox.add(panel1);
        baseBox.add(panel5);
    }

    static class FMButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JButton button = (JButton) event.getSource();
        }
    }

    private FMParameter getFMParameter(int number) {
        FMParameter p = new FMParameter(number, 4);
        p.setAlgorithm(currentAlgorithm);
        for (int i = 0; i < 4; i++) {
            p.setMultiplier(i, Double.parseDouble(multiplier[i].getText()));
            p.setAttackRate(i, Double.parseDouble(attackRate[i].getText()));
            p.setDecayRate(i, Double.parseDouble(decayRate[i].getText()));
            p.setSustainRate(i, Double.parseDouble(sustainRate[i].getText()));
            p.setReleaseRate(i, Double.parseDouble(releaseRate[i].getText()));
            p.setSustainLevel(i, Double.parseDouble(sustainLevel[i].getText()));
            p.setMaxLevel(i, Double.parseDouble(maxLevel[i].getText()));
        }
        return p;
    }

    class RadioButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JRadioButton rbutton = (JRadioButton) event.getSource();
            for (int i = 0; i < 7; i++) {
                if (rbutton == algorithmButton[i]) {
                    currentAlgorithm = i;
                    return;
                }
            }
        }
    }

    class ButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            try {
                JButton button = (JButton) event.getSource();
                if (button == buttonPlay) {
                    String[] mmls = new String[3];
                    mmls[0] = ch1.getText();
                    mmls[1] = ch2.getText();
                    mmls[2] = ch3.getText();
                    FMGeneralInstrument.setParameter(0, getFMParameter(0));
                    player.setMML(mmls);
                    printer.info("start");
                    player.start();
                } else if (button == buttonStop) {
                    player.stop();
                    printer.info("stopping...");
                } else {
                    for (int i = 0; i < 4; i++) {
                        if (button == onOffStatus[i]) {
                            if (onOffStatus[i].getText().equals("ON")) {
                                onOffStatus[i].setText("OFF");
                            } else {
                                onOffStatus[i].setText("ON");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
