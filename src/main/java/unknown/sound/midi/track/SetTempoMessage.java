package unknown.sound.midi.track;

public class SetTempoMessage extends MetaEventMessage {
    protected void setQuarterTime(int quarter) {
        setTempo(0x3938700 / quarter);
    }

    protected void setTempo(int tempo) {
        messageTempo = tempo;
    }

    public int getTempo() {
        return messageTempo;
    }

    public byte[] toBytes() {
        byte[] byteData = MetaEventMessage.arrayCopy(super.toBytes(), 3);
        int index = byteData.length - 3;
        byteData[index] = (byte) getTempo();
        return byteData;
    }

    private int messageTempo;
    public static final int LENGTH = 3;

    public SetTempoMessage(int deltaTime, int dataType, byte[] data) {
        super(deltaTime, dataType, data);
        setQuarterTime(((data[0] & 0xff) << 16) + ((data[1] & 0xff) << 8) +
                       (data[2] & 0xff));
    }

    public SetTempoMessage(int deltaTime, int tempo) {
        setDeltaTime(deltaTime);
        setTempo(tempo);
        setDataType(81);
    }
}
