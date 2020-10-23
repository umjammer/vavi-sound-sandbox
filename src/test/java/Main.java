
import java.util.Iterator;
import java.util.ServiceLoader;

import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.spi.MidiDeviceProvider;

public final class Main {

    /**
     * This main class only runs a test to list the found ports by each
     * MidiDeviceProvider
     */
    public static void main(final String[] args) {
        final ServiceLoader<MidiDeviceProvider> serviceLoader =
                ServiceLoader.load(MidiDeviceProvider.class);
        final Iterator<MidiDeviceProvider> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            final MidiDeviceProvider midiDeviceProvider =
                    iterator.next();

            final Info[] deviceInfo = midiDeviceProvider.getDeviceInfo();
            System.err.println(midiDeviceProvider.getClass().getName() + ": "
                    + deviceInfo.length);
            int i = 0;
            for (final Info info : deviceInfo) {
                System.err.println("[" + i++ + "]: " + info.getName());
            }
            System.err.println("---------------");
        }
    }
}
