package unknown.sound.converter;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;

import unknown.sound.mfi.track.NoteMessage;
import unknown.sound.mfi.track.ProgramChangeNextMessage;
import unknown.sound.mfi.track.ProgramChangePrevMessage;
import unknown.sound.mfi.track.SoundMessage;
import unknown.sound.mfi.track.TempoMessage;
import unknown.sound.mfi.track.TrackBeginningMessage;
import unknown.sound.mfi.track.TrackEndMessage;
import unknown.sound.mfi.track.TrackMessage;
import unknown.sound.midi.track.EndOfTrackMessage;
import unknown.sound.midi.track.NoteOffMessage;
import unknown.sound.midi.track.NoteOnMessage;
import unknown.sound.midi.track.ProgramChangeMessage;
import unknown.sound.midi.track.RightMessage;
import unknown.sound.midi.track.SequenceTrackNameMessage;
import unknown.sound.midi.track.SetTempoMessage;
import unknown.sound.midi.track.TextMessage;


/**
 * MIDITrackChunkToMLDTrackChunk.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 040911 nsano initial version <br>
 */
public class MIDITrackChunkToMLDTrackChunk {
    public int toMLDNote(int midiNote) {
        return midiNote - 33;
    }

    private void add(TrackMessage message) {
        mldVec.add(message);
    }

    private void insert(TrackMessage message, int index) {
        mldVec.add(index, message);
    }

    public TrackMessage messageAt(int index) {
        return mldVec.get(index);
    }

    public int messageCount() {
        return mldVec.size();
    }

    public long toMLDTime(long midiAbs) {
        return (midiAbs * MLDResolution) / inputStream.getResolution();
    }

    public long getLimitDeltaTime() {
        return (255L * inputStream.getResolution()) / MLDResolution;
    }

    private MIDIToMLDInputStream inputStream;
    private List<TrackMessage> mldVec;
    public final long MLDResolution;

    public MIDITrackChunkToMLDTrackChunk(MIDIToMLDInputStream inputStream,
                                         unknown.sound.midi.track.TrackChunkInputStream stream) {
        this.inputStream = inputStream;

        mldVec = new ArrayList<>();
        MLDResolution = 6L << inputStream.getPreferences().resolution;

        int headerIndex = inputStream.getMessageCount();
        TrackBeginningMessage biginning = new TrackBeginningMessage(0, 0);
        biginning.setAbsoluteTime(0L);
        add(biginning);
        if (inputStream.getPreferences().sound != null) {
            for (int index = 0;
                 index < inputStream.getPreferences().sound.length; index++) {
                if (inputStream.getPreferences().sound[index] != -1) {
                    SoundMessage sound = new SoundMessage(0, index,
                                                          inputStream.getPreferences().sound[index]);
                    sound.setAbsoluteTime(0L);
                    add(sound);
                }
            }
        }

        long presentTime = 0L;
        try {
            NoteMessage[] tempo = { null, null, null, null };
            int[] prevVelocity = { -1, -1, -1, -1 };
            do {
                unknown.sound.midi.track.TrackMessage message = (unknown.sound.midi.track.TrackMessage) stream.readMessage();
                presentTime = message.getAbsoluteTime();

                boolean timeOver = false;
                do {
                    timeOver = false;
                    for (int index = 0; index < tempo.length; index++) {
                        if (tempo[index] != null) {
                            long tempoAbsTime = tempo[index].getAbsoluteTime();
                            if ((presentTime - tempoAbsTime) > getLimitDeltaTime()) {
                                tempo[index].setSoundLength(255);
                                tempo[index] = new NoteMessage(0,
                                                               tempo[index].getChannel(),
                                                               tempo[index].getNote(),
                                                               0);
                                tempo[index].setAbsoluteTime((tempoAbsTime +
                                                             getLimitDeltaTime()) -
                                                             1L);
                                add(tempo[index]);
                                timeOver = true;
                            }
                        }
                    }
                } while (timeOver);
                if (message instanceof NoteOnMessage) {
                    NoteOnMessage noteOn = (NoteOnMessage) message;
                    int index = noteOn.getChunnel();
                    if (index < 4) {
                        if (tempo[index] == null) {
                            if (noteOn.getVelocity() != 0) {
                                tempo[index] = new NoteMessage(0, index,
                                                               toMLDNote(noteOn.getNote()),
                                                               0);
                                tempo[index].setAbsoluteTime(presentTime);
                                if (inputStream.getPreferences().useVelocity &&
                                    ((prevVelocity[index] != noteOn.getVelocity()) ||
                                    (prevVelocity[index] == -1))) {
                                    SoundMessage sound = new SoundMessage(0,
                                                                          index,
                                                                          noteOn.getVelocity() / 2);
                                    sound.setAbsoluteTime(presentTime);
                                    add(sound);
                                    prevVelocity[index] = noteOn.getVelocity();
                                }
                                add(tempo[index]);
                            }
                        } else if (noteOn.getVelocity() == 0) {
                            if (toMLDNote(noteOn.getNote()) == tempo[index].getNote()) {
                                tempo[index].setSoundLength((int) (toMLDTime(presentTime) -
                                                            toMLDTime(tempo[index].getAbsoluteTime())));
                                tempo[index] = null;
                            }
                        } else {
                            tempo[index].setSoundLength((int) (toMLDTime(presentTime) -
                                                        toMLDTime(tempo[index].getAbsoluteTime()) -
                                                        1L));
                            tempo[index] = new NoteMessage(0, index,
                                                           toMLDNote(noteOn.getNote()),
                                                           0);
                            tempo[index].setAbsoluteTime(presentTime);
                            add(tempo[index]);
                        }
                    }
                    continue;
                }
                if (message instanceof NoteOffMessage) {
                    NoteOffMessage noteOff = (NoteOffMessage) message;
                    int index = noteOff.getChunnel();
                    if ((index < 4) && (tempo[index] != null) &&
                        (tempo[index].getNote() == toMLDNote(noteOff.getNote()))) {
                        tempo[index].setSoundLength((int) (toMLDTime(presentTime) -
                                                    toMLDTime(tempo[index].getAbsoluteTime())));
                        tempo[index] = null;
                    }
                    continue;
                }
                if (message instanceof ProgramChangeMessage) {
                    ProgramChangeMessage program = (ProgramChangeMessage) message;
                    int index = program.getChunnel();
                    if (index < 4) {
                        ProgramChangePrevMessage prev = new ProgramChangePrevMessage(0,
                                                                                     program.getChunnel(),
                                                                                     program.getProgram());
                        ProgramChangeNextMessage next = new ProgramChangeNextMessage(0,
                                                                                     program.getChunnel(),
                                                                                     program.getProgram());
                        prev.setAbsoluteTime(presentTime);
                        next.setAbsoluteTime(presentTime);
                        add(prev);
                        add(next);
                    }
                    continue;
                }
                if (message instanceof SetTempoMessage) {
                    SetTempoMessage tempoChange = (SetTempoMessage) message;
                    TempoMessage tempoMessage = new TempoMessage(0,
                                                                 inputStream.getPreferences().resolution,
                                                                 tempoChange.getTempo());
                    tempoChange.setAbsoluteTime(presentTime);
                    add(tempoMessage);
                    continue;
                }
                if (message instanceof EndOfTrackMessage) {
                    break;
                }
                if (message instanceof SequenceTrackNameMessage) {
                    SequenceTrackNameMessage name = (SequenceTrackNameMessage) message;
                    if (inputStream.getTitleName() == null) {
                        inputStream.setTitleName(name.getTrackName());
                    }
                } else if (message instanceof TextMessage) {
                    if (inputStream.getPreferences().fullChorus) {
                        TextMessage text = (TextMessage) message;
                        if (text.getText().equals("MIDIToMLDフルコーラス")) {
                            TrackBeginningMessage beginning = new TrackBeginningMessage(0,
                                                                                        1);
                            beginning.setAbsoluteTime(presentTime);
                            add(beginning);
                        }
                    }
                } else if (message instanceof RightMessage) {
                    RightMessage right = (RightMessage) message;
                    inputStream.setRightInformation(right.getRightText());
                }
            } while (true);
        } catch (EOFException e) {
        } catch (InvalidMidiDataException ex) {
        } catch (IOException e) {
        }

        TrackEndMessage end = new TrackEndMessage(0, 0);
        end.setAbsoluteTime(presentTime);
        add(end);

        long prev = 0L;
        int trackLength = 0;
        for (int index = 0; index < messageCount(); index++) {
            unknown.sound.mfi.track.TrackMessage message = messageAt(index);
            long myAbs = message.getAbsoluteTime();
            if ((myAbs - prev) > getLimitDeltaTime()) {
                NoteMessage note = new NoteMessage(255, 0, 60, 0);
                note.setAbsoluteTime(prev + getLimitDeltaTime());
                insert(note, index);
                prev = note.getAbsoluteTime();
                trackLength += note.toBytes().length;
                inputStream.addMessage(note);
            } else {
                message.setDeltaTime((int) (toMLDTime(myAbs) - toMLDTime(prev)));
                prev = myAbs;
                trackLength += message.toBytes().length;
                inputStream.addMessage(message);
            }
        }

        unknown.sound.mfi.track.TrackChunkHeader header = new unknown.sound.mfi.track.TrackChunkHeader(trackLength);
        inputStream.insertMessage(header, headerIndex);
        inputStream.incTracksLength(trackLength + header.toBytes().length);
    }
}
