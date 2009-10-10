package unknown.sound.mfi.info;

import unknown.sound.Message;


public class VersionMessage extends InformationMessage {
    public String getVersion() {
        return messageVersion;
    }

    public void setVersion(String ver) {
        messageVersion = ver;
    }

    public byte[] toBytes() {
        byte[] versionBytes = getVersion().getBytes();
        byte[] data = {
                          118, 101, 114, 115, (byte) (versionBytes.length >> 8),
                          (byte) versionBytes.length
                      };
        byte[] versionData = Message.arrayCopy(data, versionBytes.length);
        System.arraycopy(versionBytes, 0, versionData, data.length,
                         versionBytes.length);
        return versionData;
    }

    private String messageVersion;
    public static final String TAG = "vers";

    public VersionMessage(byte[] byteData) {
        super(byteData, "vers");
        setVersion(new String(byteData));
    }

    public VersionMessage() {
        this("0100");
    }

    public VersionMessage(String str) {
        setVersion(str);
    }
}
