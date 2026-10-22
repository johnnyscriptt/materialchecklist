# Third-party notices

Material Checklist itself is MIT licensed (see `LICENSE`). The jar also carries these, unmodified:

| Component | Version | License | Where |
| --- | --- | --- | --- |
| Tesseract OCR | 5.5.3 | Apache License 2.0 | https://github.com/tesseract-ocr/tesseract |
| Leptonica | 1.87.0 | BSD 2-clause style (Leptonica license) | http://www.leptonica.org |
| JavaCPP and JavaCPP Presets (tesseract, leptonica) | 1.5.14 | Apache License 2.0 (or GPLv2 with Classpath Exception) | https://github.com/bytedeco/javacpp-presets |
| `eng.traineddata` from tessdata_fast | 4.1.0 | Apache License 2.0 | https://github.com/tesseract-ocr/tessdata_fast |

The native libraries ship inside the JavaCPP preset jars, which are nested in the mod jar
(`META-INF/jars/`) with their own license files. The tessdata license is at
`assets/materialchecklist/tessdata/LICENSE-tessdata.txt`.

The OCR number reader compares screenshots against the digit glyphs of the font the game is
using, read at run time from the player's own game files. No Minecraft assets are included in
this repository or the mod jar.
