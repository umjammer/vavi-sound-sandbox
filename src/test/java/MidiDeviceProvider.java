
import java.util.Iterator;
import java.util.ServiceLoader;

import javax.sound.midi.MidiDevice.Info;


public class MidiDeviceProvider {

    /**
     * This main class only runs a test to list the found ports by each
     * MidiDeviceProvider
     */
    public static void main(String[] args) {
        ServiceLoader<javax.sound.midi.spi.MidiDeviceProvider> serviceLoader =
                ServiceLoader.load(javax.sound.midi.spi.MidiDeviceProvider.class);
        for (javax.sound.midi.spi.MidiDeviceProvider midiDeviceProvider : serviceLoader) {
            Info[] deviceInfo = midiDeviceProvider.getDeviceInfo();
            System.err.println(midiDeviceProvider.getClass().getName() + ": " + deviceInfo.length);
            int i = 0;
            for (Info info : deviceInfo) {
                System.err.println("[" + i++ + "]: " + info.getName());
            }
            System.err.println("---------------");
        }
    }
}
