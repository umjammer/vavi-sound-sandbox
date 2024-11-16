/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * Block.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080628 nsano initial version <br>
 */
public interface Block {

    Logger logger = getLogger(Block.class.getName());

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
logger.log(Level.INFO, e.getMessage(), e);
                    throw new IllegalStateException(label, e);
                }
            } else {
                String wildcardLabel1 = label.replaceAll("#\\d+", "*");
                if (methods.containsKey(wildcardLabel1)) {
                    try {
                        return (Block) methods.get(wildcardLabel1).invoke(null, label, params);
                    } catch (Exception e) {
logger.log(Level.INFO, e.getMessage(), e);
                        throw new IllegalStateException(wildcardLabel1, e);
                    }
                } else {
                    if (label.matches("\\w+BPList") && methods.containsKey("*BPList")) {
                        try {
                            return (Block) methods.get("*BPList").invoke(null, label, params);
                        } catch (Exception e) {
logger.log(Level.INFO, e.getMessage(), e);
                            throw new IllegalStateException("*BPList", e);
                        }
                    } else {
logger.log(Level.ERROR, "error block: " + label);
                        throw new IllegalStateException("error block: " + label);
                    }
                }
            }
        }

        /**
         * collection of factory methods for {@link Block} objects.
         */
        private static final Map<String, Method> methods = new HashMap<>();

        static {
            try {
                // props
                Properties props = new Properties();
                props.load(Factory.class.getResourceAsStream("/vavi/sound/vsq/vsq.properties"));

                //
                for (Object o : props.keySet()) {
                    String key = (String) o;
logger.log(Level.DEBUG, "block class: " + props.getProperty(key));
                    @SuppressWarnings("unchecked")
                    Class<Block> clazz = (Class<Block>) Class.forName(props.getProperty(key));
logger.log(Level.DEBUG, "block class: " + StringUtil.getClassName(clazz));
                    Method method = clazz.getMethod("newInstance", String.class, List.class);

                    methods.put(key, method);
                }
            } catch (Exception e) {
logger.log(Level.INFO, e.getMessage(), e);
                throw new IllegalStateException(e);
            }
        }
    }
}
