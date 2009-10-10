package unknown.sound.converter;


/**
 *
 */
public class Preferences {
    public int start;
    public int stop;
    public int right;
    public String title;
    public String version;
    public String date;
    public String rightInfo;
    public int resolution;
    public boolean fullChorus;
    public boolean useVelocity;
    public int[] sound;

    public Preferences(int start, int stop, int right, String title,
                       String version, String date, String rightInfo,
                       int resolution, boolean fullChorus, boolean useVelocity,
                       int[] sound) {
        this.start = start;
        this.stop = stop;
        this.right = right;
        this.title = title;
        this.version = version;
        this.date = date;
        this.resolution = resolution;
        this.fullChorus = fullChorus;
        this.useVelocity = useVelocity;
        this.sound = sound;
    }

    public Preferences() {
        this(1, 1, 0, "sample", "0100", null, "ittake", 3, false, false,
             MIDIToMLDInputStream.DEFAULT_SOUND);
    }
}

/* */
