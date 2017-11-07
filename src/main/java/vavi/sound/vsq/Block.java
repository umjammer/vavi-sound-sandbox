/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Block.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080628 nsano initial version <br>
 */
public interface Block {

    /** */
    class Factory {

        /**
         * @throws IllegalStateException
         */
        public static Block getBlock(String label, List<String> params) {
            if (methods.containsKey(label)) {
                try {
                    return (Block) methods.get(label).invoke(null, label, params);
                } catch (Exception e) {
Debug.printStackTrace(e);
                    throw (RuntimeException) new IllegalStateException(label).initCause(e);
                }
            } else {
                String wildcardLabel1 = label.replaceAll("#\\d+", "*");
                if (methods.containsKey(wildcardLabel1)) {
                    try {
                        return (Block) methods.get(wildcardLabel1).invoke(null, label, params);
                    } catch (Exception e) {
Debug.printStackTrace(e);
                        throw (RuntimeException) new IllegalStateException(wildcardLabel1).initCause(e);
                    }
                } else {
                    if (label.matches("\\w+BPList") && methods.containsKey("*BPList")) {
                        try {
                            return (Block) methods.get("*BPList").invoke(null, label, params);
                        } catch (Exception e) {
Debug.printStackTrace(e);
                            throw (RuntimeException) new IllegalStateException("*BPList").initCause(e);
                        }
                    } else {
Debug.println(Level.SEVERE, "error block: " + label);
                        throw new IllegalStateException("error block: " + label);
                    }
                }
            }
        }

        /**
         * {@link Block} オブジェクトのファクトリメソッド集。
         */
        private static Map<String, Method> methods = new HashMap<>();

        static {
            try {
                // props
                Properties props = new Properties();
                props.load(Factory.class.getResourceAsStream("/vavi/sound/vsq/vsq.properties"));

                //
                Iterator<?> i = props.keySet().iterator();
                while (i.hasNext()) {
                    String key = (String) i.next();
Debug.println("block class: " + props.getProperty(key));
                    @SuppressWarnings("unchecked")
                    Class<Block> clazz = (Class<Block>) Class.forName(props.getProperty(key));
Debug.println("block class: " + StringUtil.getClassName(clazz));
                    Method method = clazz.getMethod("newInstance", String.class, List.class);

                    methods.put(key, method);
                }
            } catch (Exception e) {
Debug.printStackTrace(e);
                throw new IllegalStateException(e);
            }
        }
    }
}

/* */
