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

package jse;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import static java.lang.System.getLogger;
import static javax.swing.JOptionPane.showMessageDialog;


/**
 * AudioPlayerPanel.java
 *
 * This file is part of the Java Sound Examples.
 */
public class AudioPlayerPanel extends JPanel {

    private static final Logger logger = getLogger(AudioPlayerPanel.class.getName());

    private JButton m_loadButton;
    private JLabel m_fileLabel;
    private final JButton m_startButton;
    private final JButton m_stopButton;
    private final JButton m_pauseButton;
    private final JButton m_resumeButton;
    private JCheckBox m_muteCheckBox;
    private final JSlider m_gainSlider;
    private final JSlider m_panSlider;

    /** The SimpleAudioStream object used to play the audio files. */
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
        m_startButton.addActionListener(ae -> startPlayback());
        subControlPanel1.add(m_startButton);
        m_stopButton = new JButton("Stop");
        m_stopButton.addActionListener(ae -> stopPlayback());
        subControlPanel1.add(m_stopButton);

        JPanel subControlPanel2 = new JPanel();
        subControlPanel2.setLayout(new FlowLayout());
        controlPanel.add(subControlPanel2);
        m_pauseButton = new JButton("Pause");
        m_pauseButton.addActionListener(ae -> pausePlayback());
        subControlPanel2.add(m_pauseButton);
        m_resumeButton = new JButton("Resume");
        m_resumeButton.addActionListener(ae -> resumePlayback());
        subControlPanel2.add(m_resumeButton);
        m_startButton.setEnabled(false);
        m_stopButton.setEnabled(false);
        m_pauseButton.setEnabled(false);
        m_resumeButton.setEnabled(false);
        subControlPanel1.add(new JLabel("Volume"));
        m_gainSlider = new JSlider(JSlider.HORIZONTAL, -90, 24, 0);
        m_gainSlider.addChangeListener(ce -> changeGain());
        subControlPanel1.add(m_gainSlider);
        subControlPanel2.add(new JLabel("Balance"));
        m_panSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        m_panSlider.addChangeListener(ce -> changePan());
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
        } catch (UnsupportedAudioFileException | IllegalArgumentException e) {
            showMessageDialog(null, "The format of the audio data is not supported.");
            return false;
        } catch (LineUnavailableException e) {
            showMessageDialog(null, "There is currently no line to play.");
            return false;
        } catch (IOException e) {
            showMessageDialog(null, "Error while reading audio data.");
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
        logger.log(Level.DEBUG, "AudioPlayerPanel.changeGain(): setting gain to " + fGain);
        m_audioStream.setGain(fGain);
    }

    private void changePan() {
        int nValue = m_panSlider.getValue();
        float fPan = nValue * 0.01F;
        logger.log(Level.DEBUG, "AudioPlayerPanel.changeGain(): setting pan to " + fPan);
        m_audioStream.setPan(fPan);
    }
}
