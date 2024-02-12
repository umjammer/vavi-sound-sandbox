package unknown.sound.converter;

import java.io.EOFException;
import java.io.IOException;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;

import unknown.sound.Message;
import unknown.sound.mfi.info.CopyMessage;
import unknown.sound.mfi.info.DateMessage;
import unknown.sound.mfi.info.InformationChunkHeader;
import unknown.sound.mfi.info.SourceMessage;
import unknown.sound.mfi.info.TitleMessage;
import unknown.sound.mfi.info.VersionMessage;
import unknown.sound.midi.MIDIInputStream;
import unknown.sound.midi.header.HeaderChunkInputStream;
import unknown.sound.midi.header.HeaderMessage;
import unknown.sound.midi.track.TrackChunkInputStream;


/** */
public class MIDIToMLDInputStream {

    public MIDIToMLDInputStream(MIDIInputStream stream, Preferences pref) {
        mldTracksLength = 0;
        mldTitleName = null;
        mldRightInformation = null;
        messages = new Vector<>();
        mldReadIndex = 0;
        setStream(stream);
        setPreferences(pref);
        try {
            decodeMIDIHeader((HeaderChunkInputStream) getStream().readMIDIChunk());
            do {
                new MIDITrackChunkToMLDTrackChunk(this, (TrackChunkInputStream) getStream().readMIDIChunk());
            } while (true);
        } catch (EOFException e) {
        } catch (IOException e) {
            System.out.println("IOException 発生");
        } catch (InvalidMidiDataException e) {
            System.out.println("MIDIFormat例外発生 ");
        }
        createHeaderChunk();
    }

    public void setStream(MIDIInputStream stream) {
        midiStream = stream;
    }

    public MIDIInputStream getStream() {
        return midiStream;
    }

    protected void setPreferences(Preferences pref) {
        mldPreferences = pref;
    }

    public Preferences getPreferences() {
        return mldPreferences;
    }

    protected void setTracksLength(int len) {
        mldTracksLength = len;
    }

    public int getTracksLength() {
        return mldTracksLength;
    }

    public void incTracksLength(int len) {
        mldTracksLength += len;
    }

    protected void setTitleName(String title) {
        mldTitleName = title;
    }

    public String getTitleName() {
        return mldTitleName;
    }

    protected void setRightInformation(String right) {
        mldRightInformation = right;
    }

    public String getRightInformation() {
        return mldRightInformation;
    }

    private void setTrackCount(int count) {
        mldMIDITrackCount = count;
    }

    public int getTrackCount() {
        return mldMIDITrackCount;
    }

    private void setResolution(int resolution) {
        mldMIDIResolution = resolution;
    }

    public int getResolution() {
        return mldMIDIResolution;
    }

    public void addMessage(Message message) {
        messages.addElement(message);
    }

    public Message messageAt(int index) {
        return messages.elementAt(index);
    }

    public void insertMessage(Message message, int index) {
        messages.insertElementAt(message, index);
    }

    public int getMessageCount() {
        return messages.size();
    }

    public int getReadIndex() {
        return mldReadIndex;
    }

    public void incReadIndex() {
        mldReadIndex++;
    }

    public byte[] readMessageAsBytes()
    throws EOFException {
        return readMLDMessage().toBytes();
    }

    public Message readMLDMessage()
    throws EOFException {
        int index = getReadIndex();
        if (index >= getMessageCount()) {
            throw new EOFException();
        }
        else {
            incReadIndex();
            return messageAt(index);
        }
    }

    private void decodeMIDIHeader(HeaderChunkInputStream stream)
    throws IOException, InvalidMidiDataException {
        HeaderMessage message = (HeaderMessage) stream.readMessage();
        setTrackCount(message.getTrackCount());
        setResolution(message.getResolution());
    }

    private void createHeaderChunk() {
        Preferences preference = getPreferences();
        int informationLength = 3;
        if (preference.rightInfo != null && getRightInformation() != null) {
            CopyMessage copy;
            if (preference.rightInfo.isEmpty())
                copy = new CopyMessage(getRightInformation());
            else
                copy = new CopyMessage(preference.rightInfo);
            informationLength += copy.toBytes().length;
            insertMessage(copy, 0);
        }
        DateMessage date;
        if (preference.date != null) {
            date = new DateMessage(preference.date);
            informationLength += date.toBytes().length;
            insertMessage(date, 0);
        }
        else {
            date = null;
        }
        VersionMessage version;
        if (preference.version != null) {
            version = new VersionMessage(preference.version);
            informationLength += version.toBytes().length;
            insertMessage(version, 0);
        }
        else {
            version = null;
        }
        TitleMessage title;
        if (preference.title != null) {
            if (preference.title.isEmpty() && getTitleName() != null)
                title = new TitleMessage(getTitleName());
            else
                title = new TitleMessage(preference.title);
            informationLength += title.toBytes().length;
            insertMessage(title, 0);
        }
        else {
            title = null;
        }
        SourceMessage source;
        if (preference.right != -1) {
            source = new SourceMessage(preference.right);
            informationLength += source.toBytes().length;
            insertMessage(source, 0);
        }
        else {
            source = null;
        }
        int allLength = 2 + informationLength + getTracksLength();
        InformationChunkHeader header = new InformationChunkHeader(allLength, informationLength, preference.start, preference.stop, getTrackCount());
        insertMessage(header, 0);
    }

    private MIDIInputStream midiStream;
    private Preferences mldPreferences;
    private int mldTracksLength;
    private String mldTitleName;
    private String mldRightInformation;
    private int mldMIDITrackCount;
    private int mldMIDIResolution;
    private final Vector<Message> messages;
    private int mldReadIndex;
    public static final int[] DEFAULT_SOUND = {
        63, 63, 63, 63
    };
}

/* */
