/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

/**
 * 消音イベントを表すクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class NoteOff extends MusicEvent {

    private final int number;        // 0 - 127
    private final int velocity;    // 0 - 127 (0 means note-off)

    public NoteOff(int newTick, int newChannel, int newNumber, int newVelocity) {
        tick = newTick;
        channel = newChannel;
        number = newNumber;
        velocity = newVelocity;
    }

    public String toString() {
        return "Note OFF " + number;
    }
}
