package jse;
/*
 *        MidiPlayerApplication.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999, 2000 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class MidiPlayerApplication extends JFrame {
    private JButton m_loadButton;
    private JLabel m_sequenceLabel;
    private JFileChooser m_fileChooser;
    private MidiPlayerPanel m_midiPlayerPanel;

    public MidiPlayerApplication() {
        super("MidiPlayerApplication");
        this.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    if (m_midiPlayerPanel != null) {
                        m_midiPlayerPanel.closeSequencer();
                    }
                    System.exit(0);
                }
            });

        JPanel sequencePanel = new JPanel();
        sequencePanel.setLayout(new FlowLayout());
        m_loadButton = new JButton("Load...");
        m_loadButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    loadSequence();
                }
            });
        sequencePanel.add(m_loadButton);
        m_sequenceLabel = new JLabel("no sequence loaded");
        sequencePanel.add(m_sequenceLabel);

        m_midiPlayerPanel = new MidiPlayerPanel(sequencePanel);
        this.getContentPane().add(m_midiPlayerPanel);
    }

    private void loadSequence() {
        /*
         *        We try to create a file chooser only once.
         */
        if (m_fileChooser == null) {
            m_fileChooser = new JFileChooser();
        }

        /*
         *        Now display it.
         */
        int nOption = m_fileChooser.showOpenDialog(this);
        if (nOption != JFileChooser.APPROVE_OPTION) {
            return;
        }

        /*
         *        We get the selected file from the file chooser. Then,
         *        we create an input stream from it.
         */
        File midiFile = m_fileChooser.getSelectedFile();
        InputStream sequenceStream = null;
        try {
            sequenceStream = new FileInputStream(midiFile);
            sequenceStream = new BufferedInputStream(sequenceStream, 1024);
        } catch (IOException e) {
            /*
             *        In case of an exception, we dump the exception
             *        including the stack trace to the console
             *        output. Then, we exit the program.
             */
            e.printStackTrace();
            System.exit(1);
        }

        /*
         *        We tell the main component about the new sequence
         *        and display the name of it.
         */
        m_midiPlayerPanel.setSequence(sequenceStream);
        m_sequenceLabel.setText(midiFile.getName());
    }

    /*
     *        The starting point is here.
     */
    public static void main(String[] args) {
        MidiPlayerApplication mpa = new MidiPlayerApplication();
        mpa.pack();
        mpa.setVisible(true);
    }
}
