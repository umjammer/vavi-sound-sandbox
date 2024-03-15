package jse;
/*
 *        AudioPlayerPanel.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class AudioPlayerPanel extends JPanel {
    /**        Flag for debugging messages.
     *        If true, some messages are dumped to the console
     *        during operation.
     */
    private static boolean DEBUG = false;
    private JButton m_loadButton;
    private JLabel m_fileLabel;
    private JButton m_startButton;
    private JButton m_stopButton;
    private JButton m_pauseButton;
    private JButton m_resumeButton;
    private JCheckBox m_muteCheckBox;
    private JSlider m_gainSlider;
    private JSlider m_panSlider;

    /**        The SimpleAudioStream object used to play the audio files.
     */
    private SimpleAudioStream m_audioStream;
    private Object m_dataSource;

    public AudioPlayerPanel(JPanel northPanel) {
        m_audioStream = new SimpleAudioStream();

        this.setLayout(new BorderLayout());
        this.add("North", northPanel);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(0, 1));
        this.add("South", controlPanel);

        JPanel subControlPanel1 = new JPanel();
        subControlPanel1.setLayout(new FlowLayout());
        controlPanel.add(subControlPanel1);
        m_startButton = new JButton("Start");
        m_startButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    startPlayback();
                }
            });
        subControlPanel1.add(m_startButton);
        m_stopButton = new JButton("Stop");
        m_stopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    stopPlayback();
                }
            });
        subControlPanel1.add(m_stopButton);

        JPanel subControlPanel2 = new JPanel();
        subControlPanel2.setLayout(new FlowLayout());
        controlPanel.add(subControlPanel2);
        m_pauseButton = new JButton("Pause");
        m_pauseButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    pausePlayback();
                }
            });
        subControlPanel2.add(m_pauseButton);
        m_resumeButton = new JButton("Resume");
        m_resumeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    resumePlayback();
                }
            });
        subControlPanel2.add(m_resumeButton);
        m_startButton.setEnabled(false);
        m_stopButton.setEnabled(false);
        m_pauseButton.setEnabled(false);
        m_resumeButton.setEnabled(false);
        subControlPanel1.add(new JLabel("Volume"));
        m_gainSlider = new JSlider(JSlider.HORIZONTAL, -90, 24, 0);
        m_gainSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    changeGain();
                }
            });
        subControlPanel1.add(m_gainSlider);
        subControlPanel2.add(new JLabel("Balance"));
        m_panSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        m_panSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    changePan();
                }
            });
        subControlPanel2.add(m_panSlider);
    }

    public boolean setDataSource(URL url) {
        return setDataSourceImpl(url);
    }

    public boolean setDataSource(File file) {
        return setDataSourceImpl(file);
    }

    private boolean setDataSourceImpl() {
        return setDataSourceImpl(m_dataSource);
    }

    private boolean setDataSourceImpl(Object dataSource) {
        try {
            if (dataSource instanceof URL) {
                m_audioStream = new SimpleAudioStream((URL) dataSource);
            } else {
                m_audioStream = new SimpleAudioStream((File) dataSource);
            }
        } catch (UnsupportedAudioFileException e) {
            JOptionPane.showMessageDialog(null,
                                          "The format of the audio data is not supported.");
            return false;
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(null,
                                          "The format of the audio data is not supported.");
            return false;
        } catch (LineUnavailableException e) {
            JOptionPane.showMessageDialog(null,
                                          "There is currently no line to play.");
            return false;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                                          "Error while reading audio data.");
            return false;
        }
        m_dataSource = dataSource;
        m_startButton.setEnabled(true);
        m_stopButton.setEnabled(false);
        m_pauseButton.setEnabled(false);
        m_resumeButton.setEnabled(false);
        return true;
    }

    private void startPlayback() {
        m_audioStream.start();
        m_startButton.setEnabled(false);
        m_stopButton.setEnabled(true);
        m_pauseButton.setEnabled(true);
        m_resumeButton.setEnabled(false);
    }

    private void stopPlayback() {
        m_audioStream.stop();
        setDataSourceImpl();
        m_startButton.setEnabled(true);
        m_stopButton.setEnabled(false);
        m_pauseButton.setEnabled(false);
        m_resumeButton.setEnabled(false);
    }

    private void pausePlayback() {
        m_audioStream.pause();
        m_startButton.setEnabled(false);
        m_stopButton.setEnabled(true);
        m_pauseButton.setEnabled(false);
        m_resumeButton.setEnabled(true);
    }

    private void resumePlayback() {
        m_audioStream.resume();
        m_startButton.setEnabled(false);
        m_stopButton.setEnabled(true);
        m_pauseButton.setEnabled(true);
        m_resumeButton.setEnabled(false);
    }

    private void changeGain() {
        int nValue = m_gainSlider.getValue();
        float fGain = nValue;
        if (DEBUG) {
            System.out.println("AudioPlayerPanel.changeGain(): setting gain to " +
                               fGain);
        }
        m_audioStream.setGain(fGain);
    }

    private void changePan() {
        int nValue = m_panSlider.getValue();
        float fPan = nValue * 0.01F;
        if (DEBUG) {
            System.out.println("AudioPlayerPanel.changeGain(): setting pan to " +
                               fPan);
        }
        m_audioStream.setPan(fPan);
    }
}
