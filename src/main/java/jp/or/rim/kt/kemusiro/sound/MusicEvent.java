/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

/**
 * A class that represents an event.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1.1.1 $
 */
public abstract class MusicEvent implements Comparable<MusicEvent> {

    protected int tick;
    protected int channel;

    @Override
    public int compareTo(MusicEvent o) {
        if (o != null) {
            return Integer.compare(this.tick, o.tick);
        } else {
            throw new ClassCastException();
        }
    }

    /**
     * Get the number of ticks.
     *
     * @return the number of ticks.
     */
    public int getTick() {
        return tick;
    }

    /**
     * Get channel number.
     *
     * @return channel number
     */
    public int getChannel() {
        return channel;
    }
}
