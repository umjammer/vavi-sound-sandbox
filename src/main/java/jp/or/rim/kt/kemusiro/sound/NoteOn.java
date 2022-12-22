/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

/**
 * 発音イベントを表すクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class NoteOn extends MusicEvent {

    private int number;        // 0 - 127
    private int velocity;    // 0 - 127 (0 means note-off)

    public NoteOn(int newTick, int newChannel, int newNumber, int newVelocity) {
        tick = newTick;
        channel = newChannel;
        number = newNumber;
        velocity = newVelocity;
    }

    public int getNumber() {
        return number;
    }

    public int getVelocity() {
        return velocity;
    }

    public String toString() {
        return "Note ON " + number;
    }
}
