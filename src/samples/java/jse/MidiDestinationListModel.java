package jse;
/*
 *        MidiDestinationListModel.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 2000 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;
import javax.sound.midi.Receiver;
import javax.swing.AbstractListModel;
import javax.swing.ListSelectionModel;


public class MidiDestinationListModel extends AbstractListModel {
    private MidiDevice m_source;
    private MidiDevice.Info[] m_aDestinationInfos;

    public MidiDestinationListModel(MidiDevice source) {
        m_source = source;
        m_aDestinationInfos = MidiSystem.getMidiDeviceInfo();
    }

    public int getSize() {
        return m_aDestinationInfos.length;
    }

    public Object getElementAt(int nIndex) {
        return m_aDestinationInfos[nIndex].getName();
    }

    public void commitDestinations(ListSelectionModel selectionModel) {
        if (selectionModel.isSelectionEmpty()) {
            return;
        }
        for (int i = selectionModel.getMinSelectionIndex();
             i < selectionModel.getMaxSelectionIndex(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                setConnection(i);
            }
        }
    }

    private void setConnection(int nDestinationIndex) {
        try {
            MidiDevice device = MidiSystem.getMidiDevice(m_aDestinationInfos[nDestinationIndex]);
            device.open();

            Receiver receiver = device.getReceiver();
            Transmitter transmitter = m_source.getTransmitter();
            transmitter.setReceiver(receiver);
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }
}

/* */
