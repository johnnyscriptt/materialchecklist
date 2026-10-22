# Material Checklist

A Fabric mod for Minecraft 26.3. Import a build's material list by pasting it, dropping a
Litematica export, or screenshotting the video it came from, and get a live checklist on your
HUD. Mark the chests and shulker boxes you're filling so they count toward it, and light up
nearby containers that hold what's still missing.

Player-facing details are in [`upload-pack/copy/description-modrinth.md`](upload-pack/copy/description-modrinth.md).

## Keys

B opens the checklist, N toggles the HUD, Y tracks the container you're looking at, J finds
missing items nearby. All rebindable.

## Building

Needs JDK 25. From this folder:

```
gradlew build                 # jar in build/libs/materialchecklist-<version>.jar
gradlew test                  # unit tests, including OCR accuracy on real and synthetic screenshots
gradlew runClientGameTest     # in-game tests + store screenshots in build/run/clientGameTest/screenshots
gradlew runClient             # dev client
```

`build-mod.bat` and `run-client.bat` do the same with a local JDK 25 path (machine-specific,
gitignored).

## Architecture

The split is the usual one for this repo family: pure, unit-tested logic in `core/`, Minecraft
code around it.

- `core/parse`: `ListParser` (free text to lines), `ItemNameMatcher` (fuzzy matching with
  OCR-lookalike costs), `LitematicaImport` (CSV), `OcrCleanup` (digit repairs, table borders).
- `core/count/StackMath`: amounts in and out of stacks and shulker boxes.
- `core/list`: `Project`, `Entry`, `Progress`.
- `core/track/TrackedSpots`: who marked which container for which list, plus content snapshots
  so unloaded containers keep counting.
- `core/ocr`: `OcrEngine` (Tesseract via JavaCPP), `ImagePrep`, and `BitmapDigitReader`, which
  re-reads every word against the game font's digit glyphs. Tesseract reads Minecraft's slashed
  zero as 6 or 8; the font is regular enough (5x7 digits on a 6 px pitch, quarter-bright drop
  shadow) to read exactly instead.
- `server/`: `ChecklistServer` (rate-limited handlers for track, counts and find),
  `ContainerScanner`, `TrackedStore` (JSON in the world's `data/` folder).
- `client/`: `ProgressModel` (inventory plus server counts), the HUD, the screens, the world and
  slot highlights, and the OCR service.

Why the server is involved at all: vanilla never sends a client the contents of a container it
hasn't opened, so counting marked chests and scanning for Find can only happen server-side.
Without the mod on the server, lists, import, the HUD and inventory counting still work.

### OCR packaging

Tesseract, Leptonica and JavaCPP ship as nested jars for windows-x86_64, macosx-arm64,
macosx-x86_64 and linux-x86_64 (about 27 MB). JavaCPP unpacks the right one on first use. The
English model is in `assets/materialchecklist/tessdata/` and is copied to
`config/materialchecklist/tessdata/` the first time a screenshot is read. Start the game with
`-Dmaterialchecklist.ocrSelfTest=true` to log whether the engine loads on a given machine.

## License

MIT. See `THIRD-PARTY-NOTICES.md` for the bundled OCR components.
