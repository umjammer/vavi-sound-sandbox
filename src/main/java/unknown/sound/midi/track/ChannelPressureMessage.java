package unknown.sound.midi.track;

import unknown.sound.Message;


public class ChannelPressureMessage extends ChannelMessage {
    protected void setPressure(int velocity) {
        messagePressure = velocity;
    }

    public int getPressure() {
        return messagePressure;
    }

    public byte[] toBytes() {
        byte[] byteData = Message.arrayCopy(super.toBytes(), 1);
        int index = byteData.length - 1;
        byteData[index] = (byte) getPressure();
        return byteData;
    }

    private int messagePressure;
    public static final int LENGTH = 1;

    public ChannelPressureMessage(int deltaTime, int statusByte, byte[] data) {
        super(deltaTime, statusByte, data);
        setPressure(data[0]);
    }

    public ChannelPressureMessage(int deltaTime, int statusByte, int pressure) {
        setDeltaTime(deltaTime);
        setStatus(statusByte);
        setPressure(pressure);
    }
}
