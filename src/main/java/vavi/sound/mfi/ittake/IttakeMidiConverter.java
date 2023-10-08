/*
 * "http://tokyo.cool.ne.jp/ittake/java/MIDIToMLDv013/MIDIToMLD.html"
 */

package vavi.sound.mfi.ittake;

import java.io.IOException;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiDevice;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.MidiConverter;
import vavi.sound.mfi.NoteMessage;
import vavi.sound.mfi.Track;
import vavi.sound.mfi.vavi.header.CopyMessage;
import vavi.sound.mfi.vavi.header.ProtMessage;
import vavi.sound.mfi.vavi.header.TitlMessage;
import vavi.sound.mfi.vavi.track.ChangeBankMessage;
import vavi.sound.mfi.vavi.track.ChangeVoiceMessage;
import vavi.sound.mfi.vavi.track.CuePointMessage;
import vavi.sound.mfi.vavi.track.EndOfTrackMessage;
import vavi.sound.mfi.vavi.track.TempoMessage;
import vavi.sound.mfi.vavi.track.VolumeMessage;
import vavi.util.Debug;


/**
 * IttakeMidiConverter.
 *
 * @author ittake
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 initial version <br>
 *          1.00 030913 nsano port to my system <br>
 *          1.01 030914 nsano extends VaviMidiConverter <br>
 */
public class IttakeMidiConverter implements MidiConverter {

    /** the device information */
    private static final MfiDevice.Info info =
        new MfiDevice.Info("MIDItoMLD",
                           "Ittake",
                           "MIDItoMLD",
                           "Version 1.01") {};

    /** */
    public MfiDevice.Info getDeviceInfo() {
        return info;
    }

    /* */
    public void close() {
    }

    /* */
    public boolean isOpen() {
        return true;
    }

    /* */
    public void open() {
    }

    //----

    /**
     * Converts midi sequence to mfi sequence.
     * @deprecated
     */
    @Deprecated
    public vavi.sound.mfi.Sequence toMfiSequence(Sequence midiSequence)
        throws InvalidMidiDataException {

        return toMfiSequence(midiSequence, 0);
    }

    /** Converts midi sequence to mfi sequence. */
    public vavi.sound.mfi.Sequence toMfiSequence(Sequence midiSequence, int fileType)
        throws InvalidMidiDataException {

        try {
            return convert(midiSequence, fileType);
        } catch (IOException | InvalidMfiDataException e) {
Debug.printStackTrace(e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        }
    }

    /**
     * hack of Track
     * TODO マジで動くの？
     */
    private void insert(Track track, MfiEvent event, int index) {
        try {
            @SuppressWarnings("unchecked")
            List<MfiEvent> events = (List<MfiEvent>) track.getClass().getDeclaredField("events").get(track);
            events.add(index, event);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Converts midi sequence to mfi sequence */
    protected vavi.sound.mfi.Sequence convert(Sequence sequence, int type)
        throws InvalidMfiDataException, IOException {

        javax.sound.midi.Track[] midiTracks = sequence.getTracks();
Debug.println("divisionType: " + sequence.getDivisionType());
Debug.println("microsecondLength: " + sequence.getMicrosecondLength());
Debug.println("resolution: " + sequence.getResolution());
Debug.println("tickLength: " + sequence.getTickLength());

        //
        vavi.sound.mfi.Sequence mfiSequence = new vavi.sound.mfi.Sequence();

        boolean[] trackUsed = new boolean[16]; // TODO

        MfiContext context = new MfiContext();
        context.setMidiResolution(sequence.getResolution());

        Track mfiTrack = mfiSequence.createTrack();

        context.setMfiResolution(6L << sequence.getResolution());
        int headerIndex = mfiTrack.size(); // TODO
Debug.println("headerIndex: " + headerIndex);
        CuePointMessage biginning = new CuePointMessage(0, 0);
        mfiTrack.add(new MfiEvent(biginning, 0L));
        int volume = 0;
        if (volume != 0) {
            for (int i = 0; i < 16; i++) {
                if (trackUsed[i]) { // TODO
                    VolumeMessage sound = new VolumeMessage(0, 0xff, i, volume);
                    mfiTrack.add(new MfiEvent(sound, 0L));
                }
            }
        }
        long presentTime = 0L;

        MfiEvent[] tempo = {
            null, null, null, null
        };
        int[] prevVelocity = {
            -1, -1, -1, -1
        };

        int t = 0;
        int j = 0;
        do {
Debug.println("j: " + j);
            MidiEvent midiEvent = midiTracks[t].get(j);
            MidiMessage midiMessage = midiEvent.getMessage();
            presentTime = midiEvent.getTick();

            boolean timeOver = false;
            do {
                timeOver = false;
                for (int i = 0; i < tempo.length; i++) {
                    if (tempo[i] != null) {
                        long tempoAbsTime = tempo[i].getTick();
                        if (presentTime - tempoAbsTime > context.getLimitDeltaTime()) {
                            NoteMessage noteMessage = new NoteMessage(0, ((NoteMessage) tempo[i].getMessage()).getVoice(), ((NoteMessage) tempo[i].getMessage()).getNote(), 0);
                            noteMessage.setGateTime(255);
                            tempo[i] = new MfiEvent(noteMessage, (tempoAbsTime + context.getLimitDeltaTime()) - 1L);
                            mfiTrack.add(tempo[i]);
                            timeOver = true;
                        }
                    }
                }
Debug.println("here: " + j + ", " + timeOver);
            } while (timeOver);

            if (midiMessage instanceof ShortMessage shortMessage) {
                int command = shortMessage.getCommand();

                if (command == ShortMessage.NOTE_ON) {
                    int i = shortMessage.getChannel();
                    if (i < 4) {
                        if (tempo[i] == null) {
                            if (shortMessage.getData2() != 0) { // velocity
                                tempo[i] = new MfiEvent(new NoteMessage(0, i, context.toMLDNote(shortMessage.getData1()), 0), presentTime);
                                if (prevVelocity[i] != shortMessage.getData2() || prevVelocity[i] == -1) {
                                    VolumeMessage sound = new VolumeMessage(0, 0xff, i, shortMessage.getData2() / 2);
                                    mfiTrack.add(new MfiEvent(sound, presentTime));
                                    prevVelocity[i] = shortMessage.getData2(); // velocity
                                }
                                mfiTrack.add(tempo[i]);
                            }
                        } else {
                            if (shortMessage.getData2() == 0) { // velocity
                                if (context.toMLDNote(shortMessage.getData1()) == ((NoteMessage) tempo[i].getMessage()).getNote()) {
                                    ((NoteMessage) tempo[i].getMessage()).setGateTime((int) (context.toMLDTime(presentTime) - context.toMLDTime(tempo[i].getTick())));
                                    tempo[i] = null;
                                }
                            } else {
                                ((NoteMessage) tempo[i].getMessage()).setGateTime((int) (context.toMLDTime(presentTime) - context.toMLDTime(tempo[i].getTick()) - 1L));
                                tempo[i] = new MfiEvent(new NoteMessage(0, i, context.toMLDNote(shortMessage.getData1()), 0), presentTime);
                                mfiTrack.add(tempo[i]);
                            }
                        }
                    }
                }
                if (command == ShortMessage.NOTE_OFF) {
                    int i = shortMessage.getChannel();
                    if (i < 4 && tempo[i] != null && ((NoteMessage) tempo[i].getMessage()).getNote() == context.toMLDNote(shortMessage.getData1())) {
                        ((NoteMessage) tempo[i].getMessage()).setGateTime((int) (context.toMLDTime(presentTime) - context.toMLDTime(tempo[i].getTick())));
                        tempo[i] = null;
                    }
                }
                if (command == ShortMessage.PROGRAM_CHANGE) {
                    int i = shortMessage.getChannel();
                    if (i < 4) {
                        ChangeVoiceMessage prev = new ChangeVoiceMessage(0, 0xff, shortMessage.getChannel(), shortMessage.getData1());
                        ChangeBankMessage next = new ChangeBankMessage(0, 0xff, shortMessage.getChannel(), shortMessage.getData1());
                        mfiTrack.add(new MfiEvent(prev, presentTime));
                        mfiTrack.add(new MfiEvent(next, presentTime));
                    }
                }
            } else if (midiMessage instanceof MetaMessage metaMessage) {
                int metaType = metaMessage.getType();
                switch (metaType) {
                case 81: // テンポ設定
                    TempoMessage tempoMessage = new TempoMessage(0, 0xff, context.getMidiResolution(), (metaMessage.getData()[0] << 8) | metaMessage.getData()[1]);
                    mfiTrack.add(new MfiEvent(tempoMessage, presentTime));
                    break;
                case 47: // End of Track
                    break;
                case 3:
                    TitlMessage titl = new TitlMessage(TitlMessage.TYPE, metaMessage.getData());
                    mfiTrack.add(new MfiEvent(titl, presentTime));
                    break;
                case 1: // Text
                    ProtMessage prot = new ProtMessage(ProtMessage.TYPE, metaMessage.getData());
                    mfiTrack.add(new MfiEvent(prot, presentTime));
                    break;
                case 2: // Copyright
                    CopyMessage copy = new CopyMessage(CopyMessage.TYPE, metaMessage.getData());
                    mfiTrack.add(new MfiEvent(copy, presentTime));
                    break;
                default:
                    break;
                }
            }

            j++;
        } while (j < midiTracks[t].size());

        EndOfTrackMessage end = new EndOfTrackMessage(0, 0);
        mfiTrack.add(new MfiEvent(end, presentTime));

        long prev = 0L;
        for (int i = 0; i < mfiTrack.size(); i++) {
            MfiEvent mfiEvent = mfiTrack.get(i);
            MfiMessage mfiMessage = mfiEvent.getMessage();
            if (mfiMessage instanceof NoteMessage) {
                long myAbs = mfiMessage.getDelta();
                if (myAbs - prev > context.getLimitDeltaTime()) {
                    NoteMessage note = new NoteMessage(255, 0, 60, 0);
                    insert(mfiTrack, new MfiEvent(note, 0L /* TODO */), i);
                    mfiTrack.add(new MfiEvent(note, prev + context.getLimitDeltaTime()));
                } else {
                    mfiTrack.add(mfiEvent);
                }
                prev = myAbs;
            }
        }

        return mfiSequence;
    }

    //----

    /** Converts mfi sequence to midi sequence  */
    public Sequence toMidiSequence(vavi.sound.mfi.Sequence mfiSequence)
        throws InvalidMfiDataException {

        try {
            return convert(mfiSequence);
        } catch (IOException | InvalidMidiDataException e) {
Debug.printStackTrace(e);
            throw (InvalidMfiDataException) new InvalidMfiDataException(e);
        }
    }

    /** Converts mfi sequence to midi sequence */
    protected Sequence convert(vavi.sound.mfi.Sequence sequence)
        throws InvalidMidiDataException, IOException {

        throw new UnsupportedOperationException("not implemented");
    }
}

/* */
