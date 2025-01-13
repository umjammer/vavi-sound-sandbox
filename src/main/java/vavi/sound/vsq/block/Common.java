/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq.block;

import java.awt.Color;
import java.lang.System.Logger.Level;
import java.util.List;

import vavi.sound.vsq.Block;


/**
 * Common.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080628 nsano initial version <br>
 */
public class Common implements Block {

    /** */
    String version; // DSB301
    /** */
    String name; // Voice1
    /** */
    Color color; // 181,162,123
    /** */
    int dynamicsMode; // 1
    /** */
    int playMode; // 1

    /** */
    public static Block newInstance(String label, List<String> params) {
        Common block = new Common();
        for (String param : params) {
            String[] pair = param.split("=");
            switch (pair[0]) {
                case "Version" -> block.version = pair[1];
                case "Name" -> block.name = pair[1];
                case "Color" -> {
                    String[] rgb = pair[1].split(",");
                    int r = Integer.parseInt(rgb[0]);
                    int g = Integer.parseInt(rgb[1]);
                    int b = Integer.parseInt(rgb[2]);
                    block.color = new Color(r, g, b);
                }
                case "DynamicsMode" -> block.dynamicsMode = Integer.parseInt(pair[1]);
                case "PlayMode" -> block.playMode = Integer.parseInt(pair[1]);
                case null, default -> logger.log(Level.DEBUG, "unhandled param: " + pair[0]);
            }
        }
        return block;
    }
}
