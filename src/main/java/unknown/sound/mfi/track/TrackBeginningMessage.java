package unknown.sound.mfi.track;


/**
 * Performance position information (cue point)
 * delta, 0xff, 0xd0,
 */
public class TrackBeginningMessage extends SystemMessage {
    public int getBeginningData() {
        return messageBeginningData;
    }

    public void setBeginningData(int data) {
        messageBeginningData = data;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = {
                           (byte) getDeltaTime(), -1, -48,
                           (byte) getBeginningData()
                       };
        return bytes;
    }

    private int messageBeginningData;

    public TrackBeginningMessage(int deltaTime, int data) {
    }
}
