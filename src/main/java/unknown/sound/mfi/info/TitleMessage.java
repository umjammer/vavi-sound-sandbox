package unknown.sound.mfi.info;

import unknown.sound.Message;


public class TitleMessage extends InformationMessage {
    public String getTitle() {
        return messageTitle;
    }

    public void setTitle(String str) {
        messageTitle = str;
    }

    @Override
    public byte[] toBytes() {
        byte[] title = getTitle().getBytes();
        byte[] data = {
                          116, 105, 116, 108, (byte) (title.length >> 8),
                          (byte) title.length
                      };
        byte[] titleData = Message.arrayCopy(data, title.length);
        System.arraycopy(title, 0, titleData, data.length, title.length);
        return titleData;
    }

    private String messageTitle;
    public static String TAG = "titl";

    public TitleMessage(byte[] byteData) {
        super(byteData, TAG);
        setTitle(new String(byteData));
    }

    public TitleMessage(String title) {
        setTitle(title);
    }
}
