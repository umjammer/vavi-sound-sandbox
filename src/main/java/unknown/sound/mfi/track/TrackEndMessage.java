package unknown.sound.mfi.track;


/**
 * 0xdf End Of Track
 */
public class TrackEndMessage extends SystemMessage {
    public int getEndData() {
        return messageEndData;
    }

    public void setEndData(int data) {
        messageEndData = data;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = { (byte) getDeltaTime(), -1, -33, (byte) getEndData() };
        return bytes;
    }

    private int messageEndData;

    public TrackEndMessage(int deltaTime, int data) {
        super(deltaTime);
        setEndData(data);
    }
}
