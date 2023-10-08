package unknown.sound.mfi.info;

import unknown.sound.Message;


public abstract class InformationMessage extends Message {
    public void setTag(String tag) {
        informationTag = tag;
    }

    public String getTag() {
        return informationTag;
    }

    @Override
    public abstract byte[] toBytes();

    private String informationTag;

    public InformationMessage(byte[] byteData, String tag) {
        super(byteData);
        setTag(tag);
    }

    protected InformationMessage() {
    }
}
