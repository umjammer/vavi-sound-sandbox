package jse;
/*
 *        MidiPlayerPanel.java
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
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class MidiPlayerPanel extends JPanel {
    private JSlider m_positionSlider;
    private JButton m_startButton;
    private JButton m_stopButton;
    private JButton m_pauseButton;
    private JButton m_resumeButton;
    private JSlider m_tempoSlider;
    private Sequencer m_sequencer;
    private JList m_destinationList;
    private MidiDestinationListModel m_destinationListModel;

    public MidiPlayerPanel(JPanel northPanel) {
        super();
        initSequencer();

        setLayout(new BorderLayout());
        add(northPanel, BorderLayout.NORTH);

        JPanel positionPanel = new JPanel();
        positionPanel.setLayout(new FlowLayout());
        add(positionPanel, BorderLayout.CENTER);

        // positionPanel.add(new JLabel("Position"));
/*
  m_positionSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
  m_positionSlider.addChangeListener(new ChangeListener()
  {
  public void stateChanged(ChangeEvent ce)
  {
  // changeTempoFactor();
  }
  });
  positionPanel.add(m_positionSlider);
*/
        m_destinationListModel = new MidiDestinationListModel(m_sequencer);
        m_destinationList = new JList(m_destinationListModel);

        JScrollPane scrollPane = new JScrollPane(m_destinationList);
        positionPanel.add(scrollPane);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        add(controlPanel, BorderLayout.SOUTH);
        m_startButton = new JButton("Start");
        m_startButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    // TODO: hacky; should fade
                    m_destinationListModel.commitDestinations(m_destinationList.getSelectionModel());

                    // end hacky
                    startSequencer();
                }
            });
        controlPanel.add(m_startButton);
        m_stopButton = new JButton("Stop");
        m_stopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    stopSequencer();
                }
            });
        controlPanel.add(m_stopButton);
        m_pauseButton = new JButton("Pause");
        m_pauseButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    pauseSequencer();
                }
            });
        controlPanel.add(m_pauseButton);
        m_resumeButton = new JButton("Resume");
        m_resumeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    resumeSequencer();
                }
            });
        controlPanel.add(m_resumeButton);
        m_startButton.setEnabled(false);
        m_stopButton.setEnabled(false);
        m_pauseButton.setEnabled(false);
        m_resumeButton.setEnabled(false);
        controlPanel.add(new JLabel("Tempo"));
        m_tempoSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        m_tempoSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    changeTempoFactor();
                }
            });
        controlPanel.add(m_tempoSlider);
    }

    private void initSequencer() {
        /*
         *        Now, we need a Sequencer to play the Sequence.
         *        By means of passing null to getSequence(), we request
         *        the default sequencer.
         */
        m_sequencer = null;
        try {
            m_sequencer = MidiSystem.getSequencer();
        } catch (MidiUnavailableException e) {
        }
        if (m_sequencer == null) {
            // TODO: gui message
            System.out.println("MidiPlayerPanel.<init>(): can't get a Sequencer");
            System.exit(1);
        }

        m_sequencer.addMetaEventListener(new MetaEventListener() {
                public void meta(MetaMessage event) {
                    if (event.getType() == 47) {
                        stopSequencer();
                    }
                }
            });

        /*
         *        The Sequencer is still a dead object.
         *        We have to open() it to become live.
         */
        try {
            m_sequencer.open();
        } catch (MidiUnavailableException e) {
            // TODO: gui message
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void setSequence(InputStream inputStream) {
        try {
            m_sequencer.setSequence(inputStream);
            m_startButton.setEnabled(true);
            m_stopButton.setEnabled(false);
            m_pauseButton.setEnabled(false);
            m_resumeButton.setEnabled(false);
        } catch (IOException e) {
            // TODO: gui message
            e.printStackTrace();

            // System.exit(1);
        } catch (InvalidMidiDataException e) {
            // TODO: gui message
            e.printStackTrace();

            // System.exit(1);
        }
    }

    private void startSequencer() {
        // m_sequencer.setTickPosition(0);
        System.out.println("before start()");
        m_sequencer.start();
        System.out.println("after start()");
        m_startButton.setEnabled(false);
        m_stopButton.setEnabled(true);
        m_pauseButton.setEnabled(true);
        m_resumeButton.setEnabled(false);
    }

    private void stopSequencer() {
        m_sequencer.stop();
        m_startButton.setEnabled(true);
        m_stopButton.setEnabled(false);
        m_pauseButton.setEnabled(false);
        m_resumeButton.setEnabled(false);
    }

    private void pauseSequencer() {
        m_sequencer.stop();
        m_startButton.setEnabled(true);
        m_stopButton.setEnabled(true);
        m_pauseButton.setEnabled(false);
        m_resumeButton.setEnabled(true);
    }

    private void resumeSequencer() {
        m_sequencer.start();
        m_startButton.setEnabled(false);
        m_stopButton.setEnabled(true);
        m_pauseButton.setEnabled(true);
        m_resumeButton.setEnabled(false);
    }

    public void closeSequencer() {
        stopSequencer();
        m_sequencer.close();
    }

    private void changeTempoFactor() {
        int nValue = m_tempoSlider.getValue();
        System.out.println("tempo: " + nValue);

        float fTempoFactor = (float) Math.exp(nValue * 0.01d);
        System.out.println("tempo2: " + fTempoFactor);
        m_sequencer.setTempoFactor(fTempoFactor);
    }
}
