package unknown.sound;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.TextArea;
import java.awt.TextComponent;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Calendar;

import unknown.sound.converter.MIDIToMLDInputStream;
import unknown.sound.converter.Preferences;
import unknown.sound.midi.MIDIInputStream;
import vavi.util.Debug;


public class MTMWindow extends Frame {
    public static class MTMMIDIFileFilter implements FilenameFilter {
        @Override
        public boolean accept(File file, String s) {
            int i = s.lastIndexOf('.');
            if ((i > 0) && (i < (s.length() - 1))) {
                String s1 = s.substring(i + 1).toLowerCase();
                if (s1.equals("mid") || s1.equals("midi")) {
                    return true;
                }
            }
            return false;
        }
    }

    public class MTMVolumeListener implements ItemListener, TextListener {
        @Override
        public void itemStateChanged(ItemEvent itemevent) {
            boolean flag = volumeCheckbox.getState();
            volumeVelocityCheckbox.setEnabled(flag);
            volumeConstCheckbox.setEnabled(flag);

            boolean flag1 = volumeConstCheckbox.getState();
            volumeCh1Checkbox.setEnabled(flag && flag1);
            volumeCh2Checkbox.setEnabled(flag && flag1);
            volumeCh3Checkbox.setEnabled(flag && flag1);
            volumeCh4Checkbox.setEnabled(flag && flag1);

            boolean flag2 = volumeCh1Checkbox.getState();
            boolean flag3 = volumeCh2Checkbox.getState();
            boolean flag4 = volumeCh3Checkbox.getState();
            boolean flag5 = volumeCh4Checkbox.getState();
            volumeCh1Text.setEnabled(flag && flag1 && flag2);
            volumeCh2Text.setEnabled(flag && flag1 && flag3);
            volumeCh3Text.setEnabled(flag && flag1 && flag4);
            volumeCh4Text.setEnabled(flag && flag1 && flag5);
            volumeCh1Text.setEditable(flag && flag1 && flag2);
            volumeCh2Text.setEditable(flag && flag1 && flag3);
            volumeCh3Text.setEditable(flag && flag1 && flag4);
            volumeCh4Text.setEditable(flag && flag1 && flag5);
            if (flag && flag1 && flag2) {
                volumeCh1Text.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            } else if (flag && flag1 && flag3) {
                volumeCh2Text.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            } else if (flag && flag1 && flag4) {
                volumeCh3Text.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            } else if (flag && flag1 && flag5) {
                volumeCh4Text.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            } else {
                volumeCh1Text.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                volumeCh2Text.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                volumeCh3Text.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                volumeCh4Text.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }

        @Override
        public void textValueChanged(TextEvent textevent) {
            checkText(volumeCh1Text);
            checkText(volumeCh2Text);
            checkText(volumeCh3Text);
            checkText(volumeCh4Text);
        }

        private static void checkText(TextComponent textcomponent) {
            try {
                int i = Integer.decode(textcomponent.getText());
                if ((i < 0) || (i > 63)) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException _ex) {
            }
        }
    }

    class MTMFileButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionevent) {
            FileDialog filedialog = new FileDialog(MTMWindow.this,
                                                   "Select MIDI file", 0);
            if (midiFile == null) {
                filedialog.setDirectory(System.getProperty("user.dir"));
            } else {
                filedialog.setFile(midiFile.getAbsolutePath());
            }
            filedialog.setFilenameFilter(new MTMMIDIFileFilter());
            filedialog.setVisible(true);
            if (filedialog.getFile() != null) {
                File file = new File(filedialog.getDirectory(),
                                     filedialog.getFile());
                File file1 = new File(file.getAbsolutePath());
                if (fileCheckbox.getState() && (file1.getParent() != null)) {
                    midiFile = new File(file1.getParent());
                } else {
                    midiFile = file1;
                }
                filePathText.setText(midiFile.getAbsolutePath());
                mldButton.setEnabled(true);
                file_Exchange.setEnabled(true);
            }
        }
    }

    class MTMMLDButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionevent) {
            String[] as;
            if (midiFile.isDirectory()) {
                as = midiFile.list(new MTMMIDIFileFilter());
            } else {
                as = new String[1];
                as[0] = midiFile.getAbsolutePath();
            }
            for (String a : as) {
                File file = new File(a);
                if ((file != null) && file.isFile()) {
                    Preferences preferences = new Preferences();
                    preferences.start = 1;
                    preferences.stop = 1;
                    if (rightCheckbox.getState()) {
                        if (rightNoneCheckbox.getState()) {
                            preferences.right = 0;
                        } else if (rightIsCheckbox.getState()) {
                            preferences.right = 1;
                        } else if (rightOtherCheckbox.getState()) {
                            try {
                                int j = Integer.decode(rightOtherText.getText());
                                if ((j < 0) || (j > 255)) {
                                    throw new NumberFormatException();
                                }
                                preferences.right = j;
                            } catch (NumberFormatException _ex) {
                                preferences.right = -1;
                            }
                        } else {
                            preferences.right = -1;
                        }
                    } else {
                        preferences.right = -1;
                    }
                    if (titleCheckbox.getState()) {
                        if (titleFileCheckbox.getState()) {
                            String s = file.getName();
                            preferences.title = s.substring(0,
                                    s.lastIndexOf("."));
                        } else if (titleUserCheckbox.getState()) {
                            preferences.title = titleUserText.getText();
                        } else {
                            preferences.title = null;
                        }
                    } else {
                        preferences.title = null;
                    }
                    if (versionCheckbox.getState()) {
                        if (versionDefaultCheckbox.getState()) {
                            preferences.version = "0100";
                        } else if (versionUserCheckbox.getState()) {
                            preferences.version = versionUserText.getText();
                        } else {
                            preferences.version = null;
                        }
                    } else {
                        preferences.version = null;
                    }
                    if (dateCheckbox.getState()) {
                        if (dateTodayCheckbox.getState()) {
                            Calendar calendar = Calendar.getInstance();
                            int l = calendar.get(Calendar.YEAR);
                            int i1 = calendar.get(Calendar.MONTH);
                            String s3 = (i1 >= 10) ? "" : "0";
                            int j2 = calendar.get(Calendar.DATE);
                            String s6 = (j2 >= 10) ? "" : "0";
                            preferences.date = l + s3 + i1 + s6 + j2;
                        } else if (dateFileCheckbox.getState()) {
                            preferences.date = null;
                        } else if (dateUserCheckbox.getState()) {
                            preferences.date = dateUserText.getText();
                        } else {
                            preferences.date = null;
                        }
                    } else {
                        preferences.date = null;
                    }
                    if (informationCheckbox.getState()) {
                        if (informationFileCheckbox.getState()) {
                            preferences.rightInfo = "";
                        } else if (informationUserCheckbox.getState()) {
                            preferences.rightInfo = informationUserText.getText();
                        } else {
                            preferences.rightInfo = null;
                        }
                    } else {
                        preferences.rightInfo = null;
                    }

                    int k = resolutionChoice.getSelectedIndex();
                    preferences.resolution = k;
                    preferences.fullChorus = fullCheckbox.getState();
                    if (volumeCheckbox.getState()) {
                        if (volumeVelocityCheckbox.getState()) {
                            preferences.useVelocity = true;
                            preferences.sound = null;
                        } else if (volumeConstCheckbox.getState()) {
                            preferences.useVelocity = false;

                            int[] ai = new int[4];
                            try {
                                int j1 = Integer.decode(volumeCh1Text.getText());
                                if ((j1 < 0) || (j1 > 99) ||
                                        !volumeCh1Checkbox.getState()) {
                                    throw new NumberFormatException();
                                }
                                ai[0] = j1;
                            } catch (NumberFormatException _ex) {
                                ai[0] = -1;
                            }
                            try {
                                int k1 = Integer.decode(volumeCh2Text.getText());
                                if ((k1 < 0) || (k1 > 99) ||
                                        !volumeCh2Checkbox.getState()) {
                                    throw new NumberFormatException();
                                }
                                ai[1] = k1;
                            } catch (NumberFormatException _ex) {
                                ai[1] = -1;
                            }
                            try {
                                int l1 = Integer.decode(volumeCh3Text.getText());
                                if ((l1 < 0) || (l1 > 99) ||
                                        !volumeCh3Checkbox.getState()) {
                                    throw new NumberFormatException();
                                }
                                ai[2] = l1;
                            } catch (NumberFormatException _ex) {
                                ai[2] = -1;
                            }
                            try {
                                int i2 = Integer.decode(volumeCh4Text.getText());
                                if ((i2 < 0) || (i2 > 99) ||
                                        !volumeCh4Checkbox.getState()) {
                                    throw new NumberFormatException();
                                }
                                ai[3] = i2;
                            } catch (NumberFormatException _ex) {
                                ai[3] = -1;
                            }
                            preferences.sound = ai;
                        } else {
                            preferences.useVelocity = false;
                            preferences.sound = null;
                        }
                    } else {
                        preferences.useVelocity = false;
                        preferences.sound = null;
                    }

                    String s1 = file.getName();
                    String s2 = file.getParent();
                    String s4 = s1.substring(0, s1.lastIndexOf("."));
                    String s5 = s4 + ".mld";
                    int k2 = 2;
                    File file1;
                    do {
                        file1 = new File(s2, s5);
                        if (!file1.exists()) {
                            break;
                        }
                        s5 = s4 + k2 + ".mld";
                        if (k2 >= 0x7fffffff) {
                            return;
                        }
                        k2++;
                    } while (true);
                    try {
                        MIDIInputStream mis = new MIDIInputStream(Files.newInputStream(file.toPath()));
                        MIDIToMLDInputStream m2mis = new MIDIToMLDInputStream(mis,
                                preferences);
                        FileOutputStream fos = new FileOutputStream(file1);
                        try {
                            do {
                                fos.write(m2mis.readMessageAsBytes());
                            } while (true);
                        } catch (EOFException ignore) {
                        }
                        fos.close();
                    } catch (IOException _ex) {
                        Debug.printStackTrace(_ex);
                    }
                }
            }
        }
    }

    public class MTMDateListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent itemevent) {
            boolean flag = dateCheckbox.getState();
            dateTodayCheckbox.setEnabled(flag);
            dateFileCheckbox.setEnabled(false);
            dateUserCheckbox.setEnabled(flag);

            boolean flag1 = dateUserCheckbox.getState();
            dateUserText.setEnabled(flag && flag1);
            dateUserText.setEditable(flag && flag1);
            if (flag && flag1) {
                dateUserText.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            } else {
                dateUserText.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    public class MTMRightListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent itemevent) {
            boolean flag = rightCheckbox.getState();
            rightNoneCheckbox.setEnabled(flag);
            rightIsCheckbox.setEnabled(flag);
            rightOtherCheckbox.setEnabled(flag);

            boolean flag1 = rightOtherCheckbox.getState();
            rightOtherText.setEnabled(flag && flag1);
            rightOtherText.setEditable(flag && flag1);
            if (flag && flag1) {
                rightOtherText.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            } else {
                rightOtherText.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    public class MTMInformationListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent itemevent) {
            boolean flag = informationCheckbox.getState();
            informationFileCheckbox.setEnabled(flag);
            informationUserCheckbox.setEnabled(flag);

            boolean flag1 = informationUserCheckbox.getState();
            informationUserText.setEnabled(flag && flag1);
            informationUserText.setEditable(flag && flag1);
            if (flag && flag1) {
                informationUserText.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            } else {
                informationUserText.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    public class MTMVersionListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent itemevent) {
            boolean flag = versionCheckbox.getState();
            versionDefaultCheckbox.setEnabled(flag);
            versionUserCheckbox.setEnabled(flag);

            boolean flag1 = versionUserCheckbox.getState();
            versionUserText.setEnabled(flag && flag1);
            versionUserText.setEditable(flag && flag1);
            if (flag && flag1) {
                versionUserText.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            } else {
                versionUserText.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    public MTMWindow() {
        super("MIDIToMLD ver.0.1.3");
        setLayout(null);
        setBackground(new Color(0xeeeeee));
        addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent windowevent) {
                    System.exit(0);
                }
            });
        bar = new MenuBar();
        fileMenu = new Menu("File");
        file_Open = new MenuItem("Select MIDI File...");
        file_Open.addActionListener(new MTMFileButtonListener());
        fileMenu.add(file_Open);
        file_Exchange = new MenuItem("Convert to MLD file");
        file_Exchange.setEnabled(false);
        file_Exchange.addActionListener(new MTMMLDButtonListener());
        fileMenu.add(file_Exchange);
        file_Pref_Open = new MenuItem("Show details");
        file_Pref_Open.addActionListener(actionevent -> {
            setSize(510, 470);
            fileMenu.remove(file_Pref_Open);
            fileMenu.insert(file_Pref_Close, 3);
        });
        fileMenu.add(file_Pref_Open);
        file_Pref_Close = new MenuItem("Hide deteails");
        file_Pref_Close.addActionListener(actionevent -> {
            setSize(270, 470);
            fileMenu.remove(file_Pref_Close);
            fileMenu.insert(file_Pref_Open, 3);
        });
        fileMenu.addSeparator();
        file_Quit = new MenuItem("Quit");
        fileMenu.add(file_Quit);
        bar.add(fileMenu);
        setSize(510, 470);
        setResizable(false);
        titleCheckbox = new Checkbox("Add a title to the song", true);
        titleCheckbox.setBounds(35, 20, 120, 20);
        titleCheckbox.addItemListener(itemevent -> {
            boolean flag;
            if (((Checkbox) itemevent.getSource()).getState()) {
                flag = true;
            } else {
                flag = false;
            }
            titleFileCheckbox.setEnabled(flag);
            titleUserCheckbox.setEnabled(flag);
            if (titleUserCheckbox.getState()) {
                titleUserText.setEnabled(flag);
                titleUserText.setEditable(flag);
                if (flag) {
                    titleUserText.setCursor(new Cursor(Cursor.TEXT_CURSOR));
                } else {
                    titleUserText.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        add(titleCheckbox);
        titleGroup = new CheckboxGroup();
        titleFileCheckbox = new Checkbox("Let filename title", true, titleGroup);
        titleFileCheckbox.setBounds(55, 40, 150, 20);
        titleFileCheckbox.addItemListener(itemevent -> {
            if (titleFileCheckbox.getState()) {
                titleUserText.setEnabled(false);
                titleUserText.setEditable(false);
                titleUserText.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            } else {
                titleUserText.setEnabled(true);
                titleUserText.setEditable(true);
                titleUserText.setCursor(new Cursor(Cursor.TEXT_CURSOR));
            }
        });
        add(titleFileCheckbox);
        titleUserCheckbox = new Checkbox("", false, titleGroup);
        titleUserCheckbox.setBounds(55, 60, 20, 20);
        titleUserCheckbox.addItemListener(itemevent -> {
            if (titleUserCheckbox.getState()) {
                titleUserText.setEnabled(true);
                titleUserText.setEditable(true);
                titleUserText.setCursor(new Cursor(Cursor.TEXT_CURSOR));
            } else {
                titleUserText.setEnabled(false);
                titleUserText.setEditable(false);
                titleUserText.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
        add(titleUserCheckbox);
        titleUserText = new TextField("Enter the song name");
        titleUserText.setBounds(75, 60, 150, 20);
        titleUserText.setEnabled(false);
        titleUserText.setEditable(false);
        titleUserText.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        add(titleUserText);

        MTMVolumeListener mtmvolumelistener = new MTMVolumeListener();
        volumeCheckbox = new Checkbox("Set the volume", true);
        volumeCheckbox.setBounds(35, 100, 90, 20);
        volumeCheckbox.addItemListener(mtmvolumelistener);
        add(volumeCheckbox);
        volumeGroup = new CheckboxGroup();
        volumeVelocityCheckbox = new Checkbox("Use velocity for volume", false,
                                              volumeGroup);
        volumeVelocityCheckbox.setBounds(55, 120, 150, 20);
        volumeVelocityCheckbox.addItemListener(mtmvolumelistener);
        add(volumeVelocityCheckbox);
        volumeConstCheckbox = new Checkbox("Use fixed values (0-63)", true, volumeGroup);
        volumeConstCheckbox.setBounds(55, 140, 140, 20);
        volumeConstCheckbox.addItemListener(mtmvolumelistener);
        add(volumeConstCheckbox);
        volumeCh1Checkbox = new Checkbox("channel 1", true);
        volumeCh1Checkbox.setBounds(75, 160, 80, 20);
        volumeCh1Checkbox.addItemListener(mtmvolumelistener);
        add(volumeCh1Checkbox);
        volumeCh1Text = new TextField("63");
        volumeCh1Text.setBounds(155, 160, 40, 20);
        volumeCh1Text.addTextListener(mtmvolumelistener);
        add(volumeCh1Text);
        volumeCh2Checkbox = new Checkbox("channel 2", true);
        volumeCh2Checkbox.setBounds(75, 180, 80, 20);
        volumeCh2Checkbox.addItemListener(mtmvolumelistener);
        add(volumeCh2Checkbox);
        volumeCh2Text = new TextField("63");
        volumeCh2Text.setBounds(155, 180, 40, 20);
        volumeCh2Text.addTextListener(mtmvolumelistener);
        add(volumeCh2Text);
        volumeCh3Checkbox = new Checkbox("channel 3", true);
        volumeCh3Checkbox.setBounds(75, 200, 80, 20);
        volumeCh3Checkbox.addItemListener(mtmvolumelistener);
        add(volumeCh3Checkbox);
        volumeCh3Text = new TextField("63");
        volumeCh3Text.setBounds(155, 200, 40, 20);
        volumeCh3Text.addTextListener(mtmvolumelistener);
        add(volumeCh3Text);
        volumeCh4Checkbox = new Checkbox("channel 4", true);
        volumeCh4Checkbox.setBounds(75, 220, 80, 20);
        volumeCh4Checkbox.addItemListener(mtmvolumelistener);
        add(volumeCh4Checkbox);
        volumeCh4Text = new TextField("63");
        volumeCh4Text.setBounds(155, 220, 40, 20);
        volumeCh4Text.addTextListener(mtmvolumelistener);
        add(volumeCh4Text);
        fileLabel = new Label("MIDI file");
        fileLabel.setBounds(35, 330, 70, 20);
        add(fileLabel);
        filePathText = new TextArea("", 20, 2, 1);
        filePathText.setBounds(35, 370, 200, 40);
        filePathText.setEditable(false);
        filePathText.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        add(filePathText);
        fileButton = new Button("Select");
        fileButton.setBounds(35, 350, 40, 20);
        fileButton.addActionListener(new MTMFileButtonListener());
        add(fileButton);
        fileCheckbox = new Checkbox("Select directory", false);
        fileCheckbox.setBounds(95, 350, 130, 20);
        add(fileCheckbox);
        mldButton = new Button("Create MLD file");
        mldButton.setBounds(55, 440, 160, 20);
        mldButton.setEnabled(false);
        mldButton.addActionListener(new MTMMLDButtonListener());
        add(mldButton);
        prefLabel = new Label("Set details");
        prefLabel.setBounds(280, 20, 40, 20);
        add(prefLabel);
        resolutionLabel = new Label("Quarter note resolution");
        resolutionLabel.setBounds(35, 270, 80, 20);
        add(resolutionLabel);
        resolutionChoice = new Choice();
        resolutionChoice.setBounds(55, 290, 160, 20);
        resolutionChoice.add("6");
        resolutionChoice.add("12");
        resolutionChoice.add("24");
        resolutionChoice.add("48");
        resolutionChoice.add("96");
        resolutionChoice.add("192");
        resolutionChoice.add("384");
        resolutionChoice.add("768");
        resolutionChoice.add("1536");
        resolutionChoice.select("48");
        add(resolutionChoice);
        dateCheckbox = new Checkbox("Created date", false);
        dateCheckbox.setBounds(300, 40, 50, 20);
        dateCheckbox.addItemListener(new MTMDateListener());
        add(dateCheckbox);
        dateGroup = new CheckboxGroup();
        dateTodayCheckbox = new Checkbox("Today's date", true, dateGroup);
        dateTodayCheckbox.setBounds(320, 60, 70, 20);
        dateTodayCheckbox.setEnabled(false);
        dateTodayCheckbox.addItemListener(new MTMDateListener());
        add(dateTodayCheckbox);
        dateFileCheckbox = new Checkbox("File modification date", false, dateGroup);
        dateFileCheckbox.setBounds(320, 80, 100, 20);
        dateFileCheckbox.setEnabled(false);
        dateFileCheckbox.addItemListener(new MTMDateListener());
        add(dateFileCheckbox);
        dateUserCheckbox = new Checkbox("", false, dateGroup);
        dateUserCheckbox.setBounds(320, 100, 20, 20);
        dateUserCheckbox.setEnabled(false);
        dateUserCheckbox.addItemListener(new MTMDateListener());
        add(dateUserCheckbox);
        dateUserText = new TextField("");
        dateUserText.setBounds(340, 100, 100, 20);
        dateUserText.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        dateUserText.setEnabled(false);
        add(dateUserText);
        rightCheckbox = new Checkbox("Copyright status", true);
        rightCheckbox.setBounds(300, 140, 80, 20);
        rightCheckbox.addItemListener(new MTMRightListener());
        add(rightCheckbox);
        rightGroup = new CheckboxGroup();
        rightNoneCheckbox = new Checkbox("No copyright (00)", true, rightGroup);
        rightNoneCheckbox.setBounds(320, 160, 110, 20);
        rightNoneCheckbox.addItemListener(new MTMRightListener());
        add(rightNoneCheckbox);
        rightIsCheckbox = new Checkbox("Copyrighted (01) ", false, rightGroup);
        rightIsCheckbox.setBounds(320, 180, 110, 20);
        rightIsCheckbox.addItemListener(new MTMRightListener());
        add(rightIsCheckbox);
        rightOtherCheckbox = new Checkbox("", false, rightGroup);
        rightOtherCheckbox.setBounds(320, 200, 20, 20);
        rightOtherCheckbox.addItemListener(new MTMRightListener());
        add(rightOtherCheckbox);
        rightOtherText = new TextField();
        rightOtherText.setBounds(340, 200, 40, 20);
        rightOtherText.setEnabled(false);
        rightOtherText.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        add(rightOtherText);

        Label label = new Label("(0 - 255)");
        label.setBounds(380, 200, 50, 20);
        add(label);
        informationCheckbox = new Checkbox("Copyright information", false);
        informationCheckbox.setBounds(300, 240, 70, 20);
        informationCheckbox.addItemListener(new MTMInformationListener());
        add(informationCheckbox);
        informationGroup = new CheckboxGroup();
        informationFileCheckbox = new Checkbox("Import from file", true,
                                              informationGroup);
        informationFileCheckbox.setBounds(320, 260, 120, 20);
        informationFileCheckbox.setEnabled(false);
        informationFileCheckbox.addItemListener(new MTMInformationListener());
        add(informationFileCheckbox);
        informationUserCheckbox = new Checkbox("", false, informationGroup);
        informationUserCheckbox.setBounds(320, 280, 20, 20);
        informationUserCheckbox.setEnabled(false);
        informationUserCheckbox.addItemListener(new MTMInformationListener());
        add(informationUserCheckbox);
        informationUserText = new TextArea("", 20, 3, 1);
        informationUserText.setEnabled(false);
        informationUserText.setEditable(false);
        informationUserText.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        informationUserText.setBounds(340, 280, 110, 60);
        add(informationUserText);
        versionCheckbox = new Checkbox("Version", true);
        versionCheckbox.setBounds(300, 360, 70, 20);
        versionCheckbox.addItemListener(new MTMVersionListener());
        add(versionCheckbox);
        versionGroup = new CheckboxGroup();
        versionDefaultCheckbox = new Checkbox("Standard \"0100\"", true, versionGroup);
        versionDefaultCheckbox.setBounds(320, 380, 80, 20);
        versionDefaultCheckbox.addItemListener(new MTMVersionListener());
        add(versionDefaultCheckbox);
        versionUserCheckbox = new Checkbox("", false, versionGroup);
        versionUserCheckbox.addItemListener(new MTMVersionListener());
        versionUserCheckbox.setBounds(320, 400, 20, 20);
        add(versionUserCheckbox);
        versionUserText = new TextField("");
        versionUserText.setEnabled(false);
        versionUserText.setEditable(false);
        versionUserText.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        versionUserText.setBounds(340, 400, 100, 20);
        add(versionUserText);
        fullCheckbox = new Checkbox("Full chorus", false);
        fullCheckbox.setBounds(300, 435, 80, 20);
        add(fullCheckbox);
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.gray);
        g.drawRect(25, 30, 220, 60);
        g.drawRect(25, 110, 220, 150);
        g.drawRect(45, 150, 170, 100);
        g.drawRect(25, 340, 220, 80);
        g.drawRect(270, 30, 210, 430);
        g.drawRect(25, 280, 220, 40);
        g.drawRect(290, 50, 170, 80);
        g.drawRect(290, 150, 170, 80);
        g.drawRect(290, 250, 170, 100);
        g.drawRect(290, 370, 170, 60);
        super.paint(g);
    }

    MenuBar bar;
    Menu fileMenu;
    MenuItem file_Open;
    MenuItem file_Exchange;
    MenuItem file_Pref_Open;
    MenuItem file_Pref_Close;
    MenuItem file_Quit;
    Checkbox titleCheckbox;
    Checkbox titleFileCheckbox;
    Checkbox titleUserCheckbox;
    CheckboxGroup titleGroup;
    TextField titleUserText;
    Checkbox volumeCheckbox;
    Checkbox volumeVelocityCheckbox;
    Checkbox volumeConstCheckbox;
    CheckboxGroup volumeGroup;
    Checkbox volumeCh1Checkbox;
    Checkbox volumeCh2Checkbox;
    Checkbox volumeCh3Checkbox;
    Checkbox volumeCh4Checkbox;
    TextField volumeCh1Text;
    TextField volumeCh2Text;
    TextField volumeCh3Text;
    TextField volumeCh4Text;
    Label fileLabel;
    TextArea filePathText;
    Button fileButton;
    File midiFile;
    Checkbox fileCheckbox;
    Button mldButton;
    Label prefLabel;
    Label resolutionLabel;
    Choice resolutionChoice;
    Checkbox dateCheckbox;
    Checkbox dateTodayCheckbox;
    Checkbox dateFileCheckbox;
    Checkbox dateUserCheckbox;
    CheckboxGroup dateGroup;
    TextField dateUserText;
    Checkbox rightCheckbox;
    Checkbox rightNoneCheckbox;
    Checkbox rightIsCheckbox;
    Checkbox rightOtherCheckbox;
    CheckboxGroup rightGroup;
    TextField rightOtherText;
    Checkbox informationCheckbox;
    Checkbox informationFileCheckbox;
    Checkbox informationUserCheckbox;
    CheckboxGroup informationGroup;
    TextArea informationUserText;
    Checkbox versionCheckbox;
    Checkbox versionDefaultCheckbox;
    Checkbox versionUserCheckbox;
    CheckboxGroup versionGroup;
    TextField versionUserText;
    Checkbox fullCheckbox;
}
