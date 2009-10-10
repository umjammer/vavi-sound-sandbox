package unknown.sound.midi.track;

public class RightMessage extends MetaEventMessage {
    protected void setRightText(String rightText) {
        messageRightText = rightText;
    }

    public String getRightText() {
        return messageRightText;
    }

    public byte[] toBytes() {
        byte[] textData = getRightText().getBytes();
        byte[] byteData = MetaEventMessage.arrayCopy(super.toBytes(),
                                                     textData.length);
        int index = byteData.length - textData.length;
        System.arraycopy(textData, 0, byteData, index, textData.length);
        return byteData;
    }

    private String messageRightText;

    public RightMessage(int deltaTime, int dataType, byte[] data) {
        super(deltaTime, dataType, data);
        setRightText(new String(data));
    }

    public RightMessage(int deltaTime, String rightText) {
        setDeltaTime(deltaTime);
        setRightText(rightText);
        setDataType(2);
    }
}
