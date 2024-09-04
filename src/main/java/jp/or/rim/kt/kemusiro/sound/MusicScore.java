/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import java.io.OutputStream;
import java.util.LinkedList;


/**
 * A class that represents musical scores.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.3 $
 */
public final class MusicScore {

    private final int tickPerBeat;
    private final int channelCount;
    private final LinkedList<MusicEvent> eventList;
    private static final int defaultTempo = 100;

    /**
     * Create a new score.
     *
     * @param newTickPerBeat Counts per beat
     * @param newChannelCount    Number of channels
     */
    public MusicScore(int newTickPerBeat, int newChannelCount) {
        tickPerBeat = newTickPerBeat;
        channelCount = newChannelCount;
        eventList = new LinkedList<>();

        for (int ch = 0; ch < newChannelCount; ch++) {
            add(new ChangeInstrument(0, ch, new SquareWaveInstrument()));
            add(new ChangeTempo(0, ch, defaultTempo));
        }
    }

    public int getTickPerBeat() {
        return tickPerBeat;
    }

    public LinkedList<MusicEvent> getEventList() {
        return eventList;
    }

    public int getChannelCount() {
        return channelCount;
    }

    /**
     * Adds an event. New events are guaranteed to be sorted
     * in ascending order on the list.
     *
     * @param event an event
     */
    public void add(MusicEvent event) {
        for (int i = eventList.size() - 1; i >= 0; i--) {
            MusicEvent e = eventList.get(i);

            if (e.getTick() <= event.getTick()) {
                eventList.add(i + 1, event);
                return;
            }
        }
        eventList.addFirst(event);
    }

    public void dump(OutputStream output) {
        for (MusicEvent o : eventList) {
            System.out.print("tick:" + o.getTick());
            System.out.print(" ch:" + o.getChannel());
            System.out.println(o);
        }
    }

    public String toString() {
        return "MusicScore: Ticks/Beat=" + tickPerBeat + " channelCount:" + channelCount;
    }
}
