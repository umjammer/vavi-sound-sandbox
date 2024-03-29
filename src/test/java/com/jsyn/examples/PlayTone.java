/*
 * Copyright 2010 Phil Burk, Mobileer Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jsyn.examples;

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.SineOscillator;
import com.jsyn.unitgen.UnitOscillator;

/**
 * Play a tone using a JSyn oscillator.
 *
 * @author Phil Burk (C) 2010 Mobileer Inc
 */
public class PlayTone {
    Synthesizer synth;
    UnitOscillator osc;
    LineOut lineOut;

    private void test() {

        // Create a context for the synthesizer.
        synth = JSyn.createSynthesizer();

        // Start synthesizer using default stereo output at 44100 Hz.
        synth.start();

        // Add a tone generator.
        synth.add(osc = new SineOscillator());
        // Add a stereo audio output unit.
        synth.add(lineOut = new LineOut());

        // Connect the oscillator to both channels of the output.
        osc.output.connect(0, lineOut.input, 0);
        osc.output.connect(0, lineOut.input, 1);

        // Set the frequency and amplitude for the sine wave.
        osc.frequency.set(345.0);
        osc.amplitude.set(0.6);

        // We only need to start the LineOut. It will pull data from the
        // oscillator.
        lineOut.start();

        System.out.println("You should now be hearing a sine wave. ---------");

        // Sleep while the sound is generated in the background.
        try {
            double time = synth.getCurrentTime();
            System.out.println("time = " + time);
            // Sleep for a few seconds.
            synth.sleepUntil(time + 4.0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Stop playing. -------------------");
        // Stop everything.
        synth.stop();
    }

    public static void main(String[] args) {
        new PlayTone().test();
    }
}