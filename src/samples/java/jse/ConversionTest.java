package jse;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;


public class ConversionTest {
    public static void main(String[] args) {
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                   44100.0F, 16, 1, 2,
                                                   44100.0F, false);
        AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.ULAW,
                                                   44100.0F, 8, 1, 1, 44100.0F,
                                                   false);
        System.out.println(AudioSystem.isConversionSupported(targetFormat,
                                                             sourceFormat));
    }
}
