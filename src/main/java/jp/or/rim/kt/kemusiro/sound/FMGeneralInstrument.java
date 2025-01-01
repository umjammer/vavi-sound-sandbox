/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import jp.or.rim.kt.kemusiro.sound.tone.DummyEnvelope;
import jp.or.rim.kt.kemusiro.sound.tone.FMAlgorithm;
import jp.or.rim.kt.kemusiro.sound.tone.FMAlgorithm0;
import jp.or.rim.kt.kemusiro.sound.tone.FMAlgorithm1;
import jp.or.rim.kt.kemusiro.sound.tone.FMAlgorithm2;
import jp.or.rim.kt.kemusiro.sound.tone.FMAlgorithm3;
import jp.or.rim.kt.kemusiro.sound.tone.FMAlgorithm4;
import jp.or.rim.kt.kemusiro.sound.tone.FMAlgorithm5;
import jp.or.rim.kt.kemusiro.sound.tone.FMAlgorithm6;
import jp.or.rim.kt.kemusiro.sound.tone.FMAlgorithm7;
import jp.or.rim.kt.kemusiro.sound.tone.FMParameter;


/**
 * General-purpose FM sound source instrument.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class FMGeneralInstrument extends Instrument {

    private static final List<FMParameter> parameters = new ArrayList<>();

    public static int[] getToneNumbers() {
        return parameters.stream().mapToInt(FMParameter::getToneNumber).toArray();
    }

    public FMGeneralInstrument(int number) {
        FMParameter p = findParameter(number);
        if (p == null) {
            throw new RuntimeException("can't find tone number: " + number);
        } else {
            switch (p.getAlgorithm()) {
            case 0:
                wave = new FMAlgorithm0(p);
                break;
            case 1:
                wave = new FMAlgorithm1(p);
                break;
            case 2:
                wave = new FMAlgorithm2(p);
                break;
            case 3:
                wave = new FMAlgorithm3(p);
                break;
            case 4:
                wave = new FMAlgorithm4(p);
                break;
            case 5:
                wave = new FMAlgorithm5(p);
                break;
            case 6:
                wave = new FMAlgorithm6(p);
                break;
            case 7:
                wave = new FMAlgorithm7(p);
                break;
            default:
                throw new RuntimeException("invalid algorithm number");
            }
        }
        envelope = new DummyEnvelope();
    }

    private static FMParameter findParameter(int number) {
        for (FMParameter parameter : parameters) {
            if (parameter.getToneNumber() == number) {
                return parameter;
            }
        }
        return null;
    }

    public static void readParameterByResource() throws IOException {
        InputStream is = FMGeneralInstrument.class.getResourceAsStream("fmparameters.txt");
        if (is == null) {
            throw new IOException("no fmparameters.txt in classpath");
        }
        readParameter(new InputStreamReader(is));
    }

    public static void readParameter(Reader reader) throws IOException {
        try (BufferedReader in = new BufferedReader(reader)) {
            String line;

            for (line = in.readLine(); line != null; line = in.readLine()) {
                int toneNumber = Integer.parseInt(line);
                int opCount = 4;
                FMParameter p = new FMParameter(toneNumber, opCount);
                p.setAlgorithm(Integer.parseInt(in.readLine()));
                for (int op = 0; op < opCount; op++) {
                    p.setMultiplier(op, Double.parseDouble(in.readLine()));
                    p.setAttackRate(op, Double.parseDouble(in.readLine()));
                    p.setDecayRate(op, Double.parseDouble(in.readLine()));
                    p.setSustainRate(op, Double.parseDouble(in.readLine()));
                    p.setReleaseRate(op, Double.parseDouble(in.readLine()));
                    p.setSustainLevel(op, Double.parseDouble(in.readLine()));
                    p.setMaxLevel(op, Double.parseDouble(in.readLine()));
                }
                parameters.add(p);
            }
        }
    }

    public static void setParameter(int number, FMParameter newParameter) {
        FMParameter p = findParameter(number);
        if (p != null) {
            parameters.remove(p);
            parameters.add(newParameter);
        } else {
            parameters.add(newParameter);
        }
    }

    @Override
    public void setTimeStep(double newTimeStep) {
        super.setTimeStep(newTimeStep);
        ((FMAlgorithm) wave).setTimeStep(newTimeStep);
    }

    @Override
    public void press() {
        super.press();
        ((FMAlgorithm) wave).press();
    }

    @Override
    public void release() {
        super.release();
        ((FMAlgorithm) wave).release();
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return "FM General";
    }
}
