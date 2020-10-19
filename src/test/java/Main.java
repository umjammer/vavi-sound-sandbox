
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.spi.MidiDeviceProvider;

public final class Main {

    /**
     * This main class only runs a test to list the found ports by each
     * MidiDeviceProvider
     */
    public static void main(final String[] args) {
        final Logger logger = Logger.getLogger(Main.class.getName());
        final ServiceLoader<MidiDeviceProvider> serviceLoader =
                ServiceLoader.load(MidiDeviceProvider.class);
        final Iterator<MidiDeviceProvider> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            final MidiDeviceProvider midiDeviceProvider =
                    iterator.next();

            final Info[] deviceInfo = midiDeviceProvider.getDeviceInfo();
            logger.info(midiDeviceProvider.getClass().getName() + ": "
                    + deviceInfo.length);

            for (final Info info : deviceInfo) {
                logger.info(info.getName());
            }
            logger.info("---------------\n");
        }
    }
}
