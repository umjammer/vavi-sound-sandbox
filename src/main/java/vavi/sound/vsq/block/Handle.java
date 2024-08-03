/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq.block;

import java.util.List;

import vavi.sound.vsq.Block;
import vavi.util.Debug;


/**
 * Handle. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080628 nsano initial version <br>
 */
public class Handle implements Block {

    /** */
    String id;

    /** */
    public String getId() {
        return id;
    }

    /** */
    String l0; // "れ","4 e",0.000000,64,0,0

    /** */
    int language; // 0
    /** */
    int program; // 0    

    /** */
    String iconID; // $04040001;
    /** */
    String ids; // normal
    /** */
    int original; // 1
    /** */
    String caption; // [Normal] Type 1
    /** */
    int length; // 6080;

    /** */
    int startDepth; // 64
    /** */
    int depthBPNum; // 0
    /** */
    String depthBPX; // 0.606300,0.612500 ...
    /** */
    String depthBPY;

    /** */
    int startRate; // 50
    /** */
    int rateBPNum; // 0
    /** */
    String rateBPX;
    /** */
    String rateBPY;

    /** */
    public static Block newInstance(String label, List<String> params) {
        Handle block = new Handle();
        block.id = label;
        for (String param : params) {
            String[] pair = param.split("=");
            switch (pair[0]) {
                case "L0" -> block.l0 = pair[1];
                case "IconID" -> block.iconID = pair[1];
                case "IDS" -> block.ids = pair[1];
                case "Original" -> block.original = Integer.parseInt(pair[1]);
                case "Caption" -> block.caption = pair.length == 2 ? pair[1] : null;
                case "Length" -> block.length = Integer.parseInt(pair[1]);
                case "StartDepth" -> block.startDepth = Integer.parseInt(pair[1]);
                case "DepthBPNum" -> block.depthBPNum = Integer.parseInt(pair[1]);
                case "StartRate" -> block.startRate = Integer.parseInt(pair[1]);
                case "RateBPNum" -> block.rateBPNum = Integer.parseInt(pair[1]);
                case "Language" -> block.language = Integer.parseInt(pair[1]);
                case "OpusTest" -> block.program = Integer.parseInt(pair[1]);
                case "DepthBPX" -> block.depthBPX = pair[1];
                case "DepthBPY" -> block.depthBPY = pair[1];
                case "RateBPX" -> block.rateBPX = pair[1];
                case "RateBPY" -> block.rateBPY = pair[1];
                case null, default -> Debug.println("unhandled param: " + pair[0]);
            }
        }
        return block;
    }

    public String getLyric() {
//Debug.println("l0: " + l0.split(",")[0].replace("\"", ""));
        return l0.split(",")[0].replace("\"", "");
    }
}
