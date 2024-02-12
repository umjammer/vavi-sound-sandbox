package unknown.sound.mfi.info;

public class SourceMessage extends InformationMessage {
    public int getSource() {
        return messageSource;
    }

    protected void setSource(int source) {
        messageSource = source;
    }

    @Override
    public byte[] toBytes() {
        byte[] data = { 115, 111, 114, 99, 0, 1, (byte) getSource() };
        return data;
    }

    private int messageSource;
    public static final String TAG = "sorc";

    public SourceMessage(byte[] byteData) {
        super(byteData, "sorc");
        setSource(byteData[0]);
    }

    public SourceMessage(int source) {
        setSource(source);
    }
}
