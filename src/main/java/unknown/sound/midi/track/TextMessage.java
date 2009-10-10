package unknown.sound.midi.track;

public class TextMessage extends MetaEventMessage {
    protected void setText(String text) {
        messageText = text;
    }

    public String getText() {
        return messageText;
    }

    public byte[] toBytes() {
        byte[] textData = getText().getBytes();
        byte[] byteData = MetaEventMessage.arrayCopy(super.toBytes(),
                                                     textData.length);
        int index = byteData.length - textData.length;
        System.arraycopy(textData, 0, byteData, index, textData.length);
        return byteData;
    }

    private String messageText;

    public TextMessage(int deltaTime, int dataType, byte[] data) {
        super(deltaTime, dataType, data);
        setText(new String(data));
    }

    public TextMessage(int deltaTime, String text) {
        setDeltaTime(deltaTime);
        setDataType(1);
        setText(text);
    }
}
