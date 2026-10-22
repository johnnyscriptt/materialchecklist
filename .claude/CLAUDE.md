# Material Checklist (Fabric mod)

## Platform and stack
- Windows (PowerShell) build machine. Minecraft 26.3, Fabric Loader 0.19.5, Fabric API
  0.161.0+26.3, Loom 1.18-SNAPSHOT, Gradle 9.7.1, Java 25, Mojang mappings (no remap step).
- JUnit 5 for `core/`; Fabric client gametests in `src/gametest`.
- Offline OCR: Tesseract 5.5.3 through `org.bytedeco` JavaCPP presets 1.5.14, nested via the
  `ocrLibs` configuration in `build.gradle`.

## Commands (run from this folder; the parent folder is not a Gradle project)
- `.\gradlew.bat test`: unit tests, including `OcrAccuracyTest` (needs the 26.3 client jar in
  Loom's cache for the game font, which any build puts there).
- `.\gradlew.bat runClientGameTest`: in-game test plus store screenshots.
- `.\gradlew.bat build`: jar in `build\libs\`.
- Deploy for testing: copy `build\libs\materialchecklist-<ver>.jar` into
  `%APPDATA%\.minecraft\mods\` (the user tests in the real launcher).

## Conventions
- TDD: a failing test is seen before implementation. Pure logic lives in `core/` with no
  Minecraft imports; `src/test` only compiles against `src/main`.
- Store text: no em dashes.
- 26.3 API notes: screens switch with `minecraft.gui.setScreen`, rendering goes through
  `extractRenderState(GuiGraphicsExtractor, ...)`, HUD via `HudElementRegistry`, colour
  variants via `ColorCollection` (`Items.WOOL.white()`).

## Upload
- `upload-pack/UPLOAD-GUIDE.md` has every store field. Stores: Modrinth and CurseForge, logged
  in on the Windows Chrome.
