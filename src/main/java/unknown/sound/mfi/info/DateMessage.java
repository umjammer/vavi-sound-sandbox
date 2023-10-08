package unknown.sound.mfi.info;

import unknown.sound.Message;


public class DateMessage extends InformationMessage {
    public String getDate() {
        return messageDate;
    }

    public void setDate(String date) {
        messageDate = date;
    }

    @Override
    public byte[] toBytes() {
        byte[] dateBytes = getDate().getBytes();
        byte[] data = {
                          100, 97, 116, 101, (byte) (dateBytes.length >> 8),
                          (byte) dateBytes.length
                      };
        byte[] dateData = Message.arrayCopy(data, dateBytes.length);
        System.arraycopy(dateBytes, 0, dateData, data.length, dateBytes.length);
        return dateData;
    }

    private String messageDate;
    public static final String TAG = "date";

    public DateMessage(byte[] byteData) {
        super(byteData, "date");
        setDate(new String(byteData));
    }

    public DateMessage(String date) {
        setDate(date);
    }
}
