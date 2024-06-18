/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package jse;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;

import static java.lang.System.getLogger;


/**
 * ClipPlayer
 * <p>
 * This file is part of the Java Sound Examples.
 */
public class ClipPlayer implements LineListener {

    private static final Logger logger = getLogger(ClipPlayer.class.getName());

    private Clip m_clip;

    /**
     * The clip will be played nLoopCount + 1 times.
     */
    public ClipPlayer(File clipFile, int nLoopCount) {
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(clipFile);
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
        if (audioInputStream != null) {
            AudioFormat format = audioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            try {
                m_clip = (Clip) AudioSystem.getLine(info);
                m_clip.addLineListener(this);
                m_clip.open(audioInputStream);
            } catch (LineUnavailableException | IOException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
            m_clip.loop(nLoopCount);
        } else {
            System.out.println("ClipPlayer.<init>(): can't get data from file " + clipFile.getName());
        }
    }

    @Override
    public void update(LineEvent event) {
        if (event.getType().equals(LineEvent.Type.STOP)) {
            m_clip.close();
        } else if (event.getType().equals(LineEvent.Type.CLOSE)) {
            // There is a bug in the jdk1.3.0.
            // It prevents correct termination of the VM.
            // So we have to exit ourselves.
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("ClipPlayer: usage:");
            System.out.println("\tjava ClipPlayer <soundfile> <#loops>");
        } else {
            File clipFile = new File(args[0]);
            int nLoopCount = Integer.parseInt(args[1]);
            ClipPlayer clipPlayer = new ClipPlayer(clipFile, nLoopCount);
        }
    }
}
