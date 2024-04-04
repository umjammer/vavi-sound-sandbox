/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

/**
 * A class that represents MML syntax errors.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class MMLException extends Exception {

    public MMLException() {
        super();
    }

    public MMLException(String message) {
        super(message);
    }
}
