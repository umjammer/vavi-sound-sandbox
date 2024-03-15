package jse;
/*
 * Copyright (c) 1999 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * AudioPlayerApplication.
 *
 * This file is part of the Java Sound Examples.
 */
public class AudioPlayerApplication extends JFrame {
    // Flag for debugging messages.
    // If true, some messages are dumped to the console
    // during operation.
    private static boolean DEBUG = false;
    private JButton m_loadButton;
    private JLabel m_fileLabel;
    private JFileChooser m_fileChooser;
    private AudioPlayerPanel m_audioPlayerPanel;

    public AudioPlayerApplication() {
        super("AudioPlayerApplication");
        this.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    System.exit(0);
                }
            });

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new FlowLayout());
        m_loadButton = new JButton("Load...");
        m_loadButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    loadAudioFile();
                }
            });
        filePanel.add(m_loadButton);
        m_fileLabel = new JLabel("No audio file loaded");
        filePanel.add(m_fileLabel);

        m_audioPlayerPanel = new AudioPlayerPanel(filePanel);
        this.getContentPane().add(m_audioPlayerPanel);
    }

    private void loadAudioFile() {
        if (m_fileChooser == null) {
            m_fileChooser = new JFileChooser();
        }

        int nOption = m_fileChooser.showOpenDialog(this);
        if (nOption != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File audioFile = m_fileChooser.getSelectedFile();
        if (m_audioPlayerPanel.setDataSource(audioFile)) {
            m_fileLabel.setText("Audio file: " + audioFile.getName());
        }
    }

    public static void main(String[] args) {
        AudioPlayerApplication apa = new AudioPlayerApplication();
        apa.pack();
        apa.setVisible(true);
    }
}
