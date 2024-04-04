/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

/**
 * A class that represents an event that changes the tempo.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class ChangeTempo extends MusicEvent {
    private final int tempo;

    public ChangeTempo(int newTick, int newChannel, int newTempo) {
        tick = newTick;
        channel = newChannel;
        tempo = newTempo;
    }

    public int getTempo() {
        return tempo;
    }

    public String toString() {
        return "Change Tempo " + tempo;
    }
}
