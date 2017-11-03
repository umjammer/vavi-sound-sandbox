[![](https://jitpack.io/v/umjammer/vavi-sound-sandbox.svg)](https://jitpack.io/#umjammer/vavi-sound-sandbox)

# vavi-sound-sandbox

| **SPI** |  **Codec** |  **Description** | **IN Status** | **OUT Status** | **SPI Status** |
|:--------|:-----------|:-----------------|:--------------|:---------------|:---------------|
| midi | unknown | MFi by unknown | x | x | - |
| midi | ittake | MFi by ittake | x | x | - |
| - | ilbc  |  | x | x | - |
| - | ldcelp |  | x | x | - |
| - | mp3 |  | x | - | - |
| - | sse  | equalizing | x | - | x |
| - | laoe | resampling | ? | - | - |
| - | rohm | resampling | ? | - | - |
| - | polyphase | sox resampling | o | - | - |
| - | resampler | sox resampling | ? | - | - |
| - | perfext | sox resampling | x | - | - |
| - | ssrc2 | resampling | x | - | - |
| - | trironus | resampling| o | - | o |
| - | alac | Aple Lossless Audio Decoder | o | - | x |
| - | rocococa | QT wrapper | o | - | - |
| - | twinvq |  | x | x | - |
| - | vsq | YAMAHA Vocaloid | o | - | - |

### Legend ###

|Mark|Meaning|
|:--|:---|
| o | ok |
| ? | not tested |
| c | under construction |
| x | ng |
| - | n/a |
