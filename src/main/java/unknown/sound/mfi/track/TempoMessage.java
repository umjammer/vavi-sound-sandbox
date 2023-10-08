package unknown.sound.mfi.track;

public class TempoMessage extends SystemMessage {
    public void setMessage(int message) {
        messageMessage = message;
    }

    public int getMessage() {
        return messageMessage;
    }

    public void setTempo(int tempo) {
        messageTempo = tempo;
    }

    public int getTempo() {
        return messageTempo;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = {
                           (byte) getDeltaTime(), -1,
                           (byte) ((getMessage() & 0xf) + 192),
                           (byte) getTempo()
                       };
        return bytes;
    }

    private int messageMessage;
    private int messageTempo;

    public TempoMessage(int deltaTime, int message, int tempo) {
        super(deltaTime);
        setMessage(message & 0xf);
        setTempo(tempo);
    }

    public TempoMessage(int deltaTime, int message) {
        this(deltaTime, message, 125);
    }
}
