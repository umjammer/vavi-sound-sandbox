# vavi.sound.opl3

### status

| type     | status | ext  | sequencer | description                                                               |
|----------|:------:|------|-----------|---------------------------------------------------------------------------|
| mid      |   ✅    | mid  | midi      | MIDI Audio File Format                                                    |
| cmf      |   ✅    | cmf  | midi      | Creative Music File Format by Creative Technology                         |
| laa      |   ✅    | laa  | midi      | LucasArts AdLib Audio File Format by LucasArts                            |
| laa?     |   ?    | laa? | midi      | old LucasArts AdLib Audio File Format by LucasArts                        |
| dro      |   🚧   | dro  | dro       | DOSBox Raw OPL Format v1 (dual OPL2: 2nd chip not mixed, same as javamod) |
| dro v2   |   ✅    | dro  | dro2      | DOSBox Raw OPL Format v2                                                  |
| sci      |   ✅    | sci  | midi      | Sierra's AdLib Audio File Format                                          |
| adv sci  |   ✅    | sci? | midi      | advanced Sierra's AdLib Audio File Format                                 |
| hsc      |   ✅    | hsc  | hsc       | HSC AdLib Composer / HSC-Tracker Format                                   |
| sng      |   ✅    | sng  | sng       | SNG File Format (Adlib Visual Composer "ObsM")                            |
| d00      |   ✅    | d00  | d00       | EdLib packed Format (JCH, versions 0-4)                                   |
| rad      |   ✅    | rad  | rad       | Reality AdLib Tracker Format (v1 and v2)                                  |
| adl      |   ✅    | adl  | adl       | Westwood ADL Format (EoB, Dune II, Kyrandia, LoL; versions 1-4)           |
| idadl*   |   ✅️   |      | idadl     | id Software Adlib Sound Effect                                            |
| bam*     |   ✅️   | bam  | bam       | Bob's Adlib                                                               |
| imf*     |   ✅️   | wfm  | imf       | IMF                                                                       |
| ksm*     |   ✅️   | ksm  | ksm       | Ken Silverman's Music Format                                              |
| lds*     |   ✅️   | lds  | lds       | Loudness Sound System                                                     |
| mkj*     |   ✅️   | mkj  | mkj       | MKJamz                                                                    |
| s3m*     |   ✅️   | s3m  | s3m       | Scream Tracker 3                                                          |
| xsm*     |   ✅️   | xsm  | xsm       | eXtra Simple Music                                                        |
| xad      |   ✅    | xad  | xad       | XAD Shell Format (hyp, psi, flash, bmf, rat, hybrid)                      |
| a2m      |   ✅    | a2m  | a2m       | AdLib Tracker 2 (old loader; versions 1, 4, 5, 8)                         |
| a2m v2   |   ✅    | a2m  | a2m-v2    | AdLib Tracker 2 (versions 1-14) / A2T tiny module, all depackers          |
| adtrack  |   ✅    | sng  | adtrack   | Adlib Tracker (sng + ins instrument file pair)                            |
| amd      |   ✅    | amd  | amd       | AMUSIC Adlib Tracker                                                      |
| bmf      |   ✅    | bmf  | xad       | Easy AdLib 1.0 (via XAD shell)                                            |
| cff      |   ✅    | cff  | cff       | BoomTracker 4.0 (CUD-FM-File)                                             |
| cmfmssop |   🚧   | cmf  | cmfmcsop  | SoundFX Macs Opera CMF (plays; diverges from adplug refs)                 |
| coktel   |   ✅    | adl  | coktel    | Coktel Vision ADL                                                         |
| dfm      |   ✅    | dfm  | dfm       | Digital-FM                                                                |
| dmo      |   🚧   | dmo  | dmo       | Twin TrackPlayer (TwinTeam) (plays; diverges from adplug refs)            |
| dtm      |   ✅    | dtm  | dtm       | DeFy Adlib Tracker                                                        |
| flash    |   ✅    | xad  | xad       | Flash (via XAD shell)                                                     |
| fmc      |   ✅    | fmc  | fmc       | Faust Music Creator                                                       |
| got      |   ✅    | got  | got       | God of Thunder Music (3-byte time/reg/val records)                        |
| hrad     |   🚧   | sdb  | herad     | Herbulot AdLib System (sdb, agd, ha2, hsq, sqx; diverges from adplug)     |
| hsp      |   ✅    | hsp  | hsp       | HSC Packed                                                                |
| hybrid   |   ✅    | xad  | xad       | Hybrid (via XAD shell)                                                    |
| hyp      |   ✅    | xad  | xad       | Hypnosis (via XAD shell)                                                  |
| jbm      |   ✅    | jbm  | jbm       | JBM Adlib Music (Johannes Bjerregaard)                                    |
| mad      |   ✅    | mad  | mad       | Mlat Adlib Tracker                                                        |
| mdi      |   ✅    | mdi  | mdi       | AdLib MIDIPlay File (SMF type 0 with AdLib meta events)                   |
| msc      |   ✅    | msc  | msc       | AdLib MSCplay (Ceres)                                                     |
| mtk      |   ✅    | mtk  | hsc       | MPU-401 Trakker (LZ-compressed HSC)                                       |
| mtr      |   ✅    | mtr  | mtr       | Master Tracker (v1 and v2)                                                |
| mus      |   ✅    | mus  | mus       | AdLib Visual Composer MIDI (mus, mdy) / IMPlay Song (ims), SND/BNK banks  |
| pis      |   ✅    | pis  | pis       | Beni Tracker PIS module                                                   |
| plx      |   ✅    | plx  | plx       | PALLADIX Sound System                                                     |
| raw      |   ✅    | raw  | raw       | Raw AdLib Capture (RdosPlay RAW, dual OPL2)                               |
| rix      |   ✅    | rix  | rix       | Softstar RIX OPL Music (rix, mkf archives)                                |
| rol      |   ✅    | rol  | rol       | AdLib Visual Composer ROL (instruments from standard.bnk)                 |
| sa2      |   ✅    | sa2  | sa2       | Surprise! Adlib Tracker / SAdT2 (versions 1-9)                            |
| sop      |   ✅    | sop  | sop       | sopepos' Note Sequencer (v1 and v2, OPL3 with 4-op mode)                  |
| u6m      |   ✅    | m    | u6m       | Ultima 6 Music (LZW-compressed)                                           |

<sub> * ported from NScumm </sub>

✅ formats are validated register-for-register against adplug's reference dumps
by `vavi.sound.opl3.AdplugRefTest` (fixtures in `src/test/resources/opl3`,
gzipped reference dumps from adplug's `test/testref` in `src/test/resources/opl3ref`).

note: psi and rat are XAD shell subformats, covered by the xad row.

note: idadl (id Software Adlib Sound Effect) has no official file extension —
these are chunks inside id Software AUDIOT/AUDIOHED archives (Wolfenstein 3D
etc.), so no standalone samples exist; `.idadl` is this project's invented
extension for extracted lumps (format: length u32 + priority u16 +
instrument[16] + octave u8 + note bytes).

## References

 * https://moddingwiki.shikadi.net/wiki/AdLib_Instrument_Bank_Format
 * https://moddingwiki.shikadi.net/wiki/AdLib_Instrument_Format

### original

* [opl3](https://opl3.cozendey.com/)
* [adplug](https://github.com/adplug/adplug)
* [adplug](https://adplug.github.io/)

## TODO

 * ~~https://github.com/scemino/NScumm.Audio~~
 * dro 🚧 maybe dual opl implementation is needed ... see [javamod](https://github.com/umjammer/javamod)
 * vavi.sound.midi.opl3 and vavi.sound.opl3 are interdependence (Context, Opl3Instrument) 
 