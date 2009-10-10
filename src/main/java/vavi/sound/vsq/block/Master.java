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
 * Master. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080628 nsano initial version <br>
 */
public class Master implements Block {

    /** */
    int preMeasure; // 4

    /** */
    public static Block newInstance(String label, List<String> params) {
        Master block = new Master();
        for (String param : params) {
            String[] pair = param.split("=");
            if ("PreMeasure".equals(pair[0])) {
                block.preMeasure = Integer.parseInt(pair[1]);
            } else {
Debug.println("unhandled param: " + pair[0]);
            }
        }
        return block;
    }
}

/* */
