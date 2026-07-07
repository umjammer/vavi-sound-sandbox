# vavi.sound.opl3

## original

 * [opl3](https://opl3.cozendey.com/)
 * [adplug](https://github.com/adplug/adplug)
 * [adplug](https://adplug.github.io/)
 
| type    | status | ext  | sequencer | description                                                               |
|---------|:------:|------|-----------|---------------------------------------------------------------------------|
| mid     |  ✅     | mid  | midi      | MIDI Audio File Format                                                    |
| cmt     |   ✅    | cmf  | midi      | Creative Music File Format by Creative Technology                         |
| laa     |   ✅    | laa  | midi      | LucasArts AdLib Audio File Format by LucasArts                            |
| laa?    |   ?    | laa? | midi      | old LucasArts AdLib Audio File Format by LucasArts                        |
| dro     |   🚧   | dro  | dro       | DOSBox Raw OPL Format v1 (dual OPL2: 2nd chip not mixed, same as javamod) |
| dro v2  |   ✅    | dro  | dro2      | DOSBox Raw OPL Format v2                                                  |
| sci     |   ✅    | sci  | midi      | Sierra's AdLib Audio File Format                                          |
| adv sci |   ✅    | sci? | midi      | advanced Sierra's AdLib Audio File Format                                 |
| hsc     |   ✅    | hsc  | hsc       | HSC AdLib Composer / HSC-Tracker Format                                   |
| sng     |   ✅    | sng  | sng       | SNG File Format (Adlib Visual Composer "ObsM")                            |
| d00     |   ✅    | d00  | d00       | EdLib packed Format (JCH, versions 0-4)                                   |
| rad     |   ✅    | rad  | rad       | Reality AdLib Tracker Format (v1 and v2)                                  |
| adl     |   ✅    | adl  | adl       | Westwood ADL Format (EoB, Dune II, Kyrandia, LoL; versions 1-4)          |

## References

 * https://moddingwiki.shikadi.net/wiki/AdLib_Instrument_Bank_Format
 * https://moddingwiki.shikadi.net/wiki/AdLib_Instrument_Format

## TODO

 * https://github.com/scemino/NScumm.Audio
 * 🚧 maybe dual opl implementation is needed ... see [javamod](https://github.com/umjammer/javamod)
 * vavi.sound.midi.opl3 and vavi.sound.opl3 are interdependence (Context, Opl3Instrument) 
