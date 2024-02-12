package unknown.sound.midi.track;

import unknown.sound.Message;


public class PitchBendChangeMessage extends ChannelMessage {
    protected void setPressure(int velocity) {
        messagePressure = velocity;
    }

    public int getPressure() {
        return messagePressure;
    }

    @Override
    public byte[] toBytes() {
        byte[] byteData = Message.arrayCopy(super.toBytes(), 2);
        int index = byteData.length - 2;
        byteData[index] = (byte) (getPressure() & 0x7f);
        byteData[index + 1] = (byte) ((getPressure() << 7) & 0x7f);
        return byteData;
    }

    private int messagePressure;
    public static final int LENGTH = 2;

    public PitchBendChangeMessage(int deltaTime, int statusByte, byte[] data) {
        super(deltaTime, statusByte, data);
        setPressure((data[1] & 0x7f) << (7 + (data[0] & 0x7f)));
    }

    public PitchBendChangeMessage(int deltaTime, int statusByte, int pressure) {
        setDeltaTime(deltaTime);
        setStatus(statusByte);
        setPressure(pressure);
    }
}
