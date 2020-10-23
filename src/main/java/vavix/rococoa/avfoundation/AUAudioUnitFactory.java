
package vavix.rococoa.avfoundation;

import com.sun.jna.Callback;


// protocol
public interface AUAudioUnitFactory {

    AUAudioUnit createAudioUnitWithComponentDescription(AudioComponentDescription.ByValue desc);

    /**
     * @param completionHandler Function<AUViewControllerBase, Void>
     */
    void requestViewControllerWithCompletionHandler(Callback completionHandler);
}