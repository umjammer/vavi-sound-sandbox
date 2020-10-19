/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.ituneslibrary;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.rococoa.cocoa.foundation.NSArray;
import org.rococoa.cocoa.foundation.NSString;

import vavix.rococoa.foundation.NSDictionary;


/**
 * ITLibraryTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/02/15 umjammer initial version <br>
 */
@EnabledOnOs(OS.MAC)
class ITLibraryTest {

    /**
     * @param args top_directory regex_pattern
     */
    public static void main(String[] args) throws Exception {
        ITLibraryTest app = new ITLibraryTest();
        app.exec(args);
    }

    BufferedImage image;

    /** show all art works */
    private void exec(String[] args) throws Exception {
        JFrame frame = new JFrame();
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel() {
            public void paint(Graphics g) {
                g.drawImage(image, 0, 0, this);
            }
        };
        frame.getContentPane().add(panel);
        frame.setVisible(true);

        ITLibrary library = ITLibrary.libraryWithAPIVersion("1.1");
        library.getMediaItems().stream()
            .filter(t -> t.mediaKind() == 2)
            .forEach(t -> {
                try {
                    frame.setTitle(t.artist().name() + " - " + t.title());
                    image = t.artwork().getImage();
                    panel.repaint();
                } catch (IOException e) {
                    System.err.println(e);
                }
            });
    }

    @Test
    void test() {
        ITLibrary library = ITLibrary.libraryWithAPIVersion("1.1");
        library.getMediaItems().stream()
            .filter(t -> t.mediaKind() == 2)
            .filter(t -> t.artist().name().contains("a-ha"))
            .forEach(t -> System.err.println(t.artist().name() + " - " + t.title() + " [" + t.composer() + "]"));
    }

    int c = 0;

    // TODO doesn't work
    @Test
    @Disabled
    void test2() {
        ITLibrary library = ITLibrary.libraryWithAPIVersion("1.1");
        library.getMediaItems().stream()
            .filter(t -> t.mediaKind() == 2)
            .filter(t -> c++ < 1)
            .forEach(t -> {
                System.err.println(t.artist().name() + " - " + t.title());
                NSDictionary dict = t.fields();
                NSArray keys = dict.allKeys();
                for (int i = 0; i < keys.count(); i++) {
                    System.err.println(org.rococoa.Rococoa.cast(keys.objectAtIndex(i), NSString.class));
                }
            });
    }
}

/* */
