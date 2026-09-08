# vavi.sound.sampled.rococoa

Provides Rococoa spi implementation classes.

## Connecting midi (gervill) output to AudioUnit effects

`RococoaSourceDataLine` is a `SourceDataLine` whose output is rendered by an `AVAudioEngine`,
through a chain of AudioUnit effects. It is published by the already registered
`RococoaMixerProvider`, so nothing new has to be installed.

    AVAudioPlayerNode --> aufx --> aufx --> mainMixerNode --> outputNode

gervill writes pcm into the line, the line hands each buffer to the player node, and the
completion handler of `scheduleBuffer:completionHandler:` releases the slot again. `write()`
blocks until a slot comes back, so core audio's HAL clock is the only clock in the pipeline.

Two ways to wire it up:

```java
// spi only, works for any player, not just midi
System.setProperty("javax.sound.sampled.SourceDataLine", "#Rococoa Mixer");
System.setProperty("vavi.sound.sampled.rococoa.RococoaSourceDataLine.effects", "appl:mrev,appl:dely");
Synthesizer synthesizer = MidiSystem.getSynthesizer(); // "#Gervill"
synthesizer.open();
```

```java
// handing the line over, when you want to keep the AVAudioUnitEffect objects
RococoaSourceDataLine line = new RococoaSourceDataLine();
line.setEffects("appl:mrev,appl:dely");
line.open(new AudioFormat(44100, 16, 2, true, false));
new SoftSynthesizer().open(line, null);
```

`aufx` is assumed for a two part `"manufacturer:subtype"` spec, use
`"type:manufacturer:subtype"` for anything else (`aumf` music effects, say). `auval -a` lists
what is installed.

### effect parameters

The factory defaults are often unusable — AUMatrixReverb comes up at **100% wet**, AUDelay at
50% wet with a 1 second delay. Append `?name=value;name=value` to an effect in the spec, a key
being either the display name (case insensitive) or the numeric parameter id:

    appl:mrev?Dry/Wet Mix=20,appl:dely?Dry/Wet Mix=15;Delay Time=0.25;Feedback=20

`,` separates effects, `;` separates parameters, so this works from a system property too.

Or after the line is open, on the `AVAudioUnit` itself:

```java
AVAudioUnitEffect reverb = line.getEffects().getFirst();
reverb.getParameters().forEach(System.out::println);
//   0: Dry/Wet Mix [0.0..100.0] default 100.0 equalPowerCrossfade
//   4: Pre-Delay [0.001..0.03] default 0.015795 seconds
//   ...
reverb.setParameter("Dry/Wet Mix", 20);
```

## References

[rococoa](https://github.com/iterate-ch/rococoa)

## TODO

* ~~complete~~
* `AVAudioEngine#enableManualRenderingMode` so the chain can be rendered offline (and tested
  without a sound card)
