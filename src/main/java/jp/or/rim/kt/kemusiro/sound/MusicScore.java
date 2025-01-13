/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import java.io.PrintStream;
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
     * @param tickPerBeat  Counts per beat
     * @param channelCount Number of channels
     */
    public MusicScore(int tickPerBeat, int channelCount) {
        this.tickPerBeat = tickPerBeat;
        this.channelCount = channelCount;
        eventList = new LinkedList<>();

        for (int ch = 0; ch < channelCount; ch++) {
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

    public void dump(PrintStream output) {
        for (MusicEvent o : eventList) {
            output.print("tick:" + o.getTick());
            output.print(" ch:" + o.getChannel());
            output.println(o);
        }
    }

    @Override
    public String toString() {
        return "MusicScore: Ticks/Beat=" + tickPerBeat + " channelCount:" + channelCount;
    }
}
