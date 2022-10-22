/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import com.sun.jna.Callback;


/**
 * @protocol AUAudioUnitFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2020/??/?? nsano initial version <br>
 */
public interface AUAudioUnitFactory {

    AUAudioUnit createAudioUnitWithComponentDescription(AudioComponentDescription.ByValue desc);

    /**
     * @param completionHandler Function<AUViewControllerBase, Void>
     */
    void requestViewControllerWithCompletionHandler(Callback completionHandler);
}