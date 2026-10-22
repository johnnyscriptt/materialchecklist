# Upload pack: Material Checklist 0.1.0

Everything needed to fill in both listings. Nothing here is referenced by the build.

```
upload-pack/
├─ UPLOAD-GUIDE.md                  you are here
├─ copy/
│  ├─ summary.txt                   short blurb + two alternates, with character counts
│  ├─ description-modrinth.md       paste into Modrinth's markdown editor
│  ├─ description-curseforge.html   paste into CurseForge's editor in source mode
│  └─ changelog-0.1.0.md            paste into the version/file changelog on both sites
├─ gallery/
│  ├─ 01..06-*.png                  captions burned in, 01 is the featured image
│  ├─ clean/                        the same six without captions
│  └─ captions.md                   title + description for each image
├─ icon/
│  ├─ modrinth-icon-512.png         512x512, under Modrinth's 256 KiB cap
│  ├─ curseforge-avatar-400.png     400x400
│  ├─ mod-icon-128.png              already in the jar as the mod-list icon
│  └─ source-icon-1254*.png         the ChatGPT original, plus a transparent-corner copy
├─ files/
│  └─ materialchecklist-0.1.0.jar   the upload (main jar only, never -sources)
├─ work/litematica-frame.png        the sample "video frame" used for gallery shot 02
├─ make_litematica_frame.py         regenerates that frame from the game's font
└─ make_store_images.py             rebuilds gallery/ from a gametest run
```

To regenerate the gallery: `python make_litematica_frame.py work/litematica-frame.png`, then
`gradlew runClientGameTest` in the mod folder, then `python make_store_images.py`.

---

## Shared facts

| Field | Value |
| --- | --- |
| Mod name | Material Checklist |
| Mod id | `materialchecklist` |
| Version | 0.1.0 |
| Minecraft | 26.3 |
| Loader | Fabric (Loader 0.19.5 or newer) |
| Java | 25 |
| Dependency | **Fabric API, required** |
| License | MIT (bundled Tesseract is Apache 2.0, see THIRD-PARTY-NOTICES.md) |
| Source | https://github.com/johnnyscriptt/materialchecklist (create and make public first, or the links 404) |
| Issues | https://github.com/johnnyscriptt/materialchecklist/issues |
| Author | johnnyscriptt |

House rule: no em dashes anywhere in store text (some got stuck in CurseForge changelogs that
can't be edited during moderation). The copy here has none.

---

## Modrinth

Create project, Mod, Fabric.

| Field | What to put |
| --- | --- |
| Name | `Material Checklist` |
| Slug | `material-checklist` |
| Summary | line 2 of `copy/summary.txt` |
| Description | `copy/description-modrinth.md` |
| Icon | `icon/modrinth-icon-512.png` |
| Categories | **Utility**, **Storage**, **Management** |
| Client side | **Required** |
| Server side | **Optional**: lists, import, HUD and inventory counting work without it; tracking and Find need it |
| License | MIT |
| Source / Issues | the GitHub URLs above |

Gallery: all six in order, **01** featured, titles and descriptions from `gallery/captions.md`.

Version: file `files/materialchecklist-0.1.0.jar`, number `0.1.0`, name
`Material Checklist 0.1.0`, Fabric, game version 26.3, Fabric API **Required**, changelog from
`copy/changelog-0.1.0.md`, channel Release.

## CurseForge

Create project, Minecraft, Mods.

| Field | What to put |
| --- | --- |
| Name | `Material Checklist` |
| Summary | same line from `copy/summary.txt` |
| Description | `copy/description-curseforge.html` (click the source button first) |
| Avatar | `icon/curseforge-avatar-400.png` |
| Categories | **Utility & QoL**, **Storage**, **Map and Information** |
| License | MIT |

File: `files/materialchecklist-0.1.0.jar`, display name `Material Checklist 0.1.0`, Release,
game version **26.3** with the **Fabric** loader ticked and Java 25, Relations: **Fabric API**
as *Required Dependency*, changelog from `copy/changelog-0.1.0.md`.

**Heads-up for CurseForge review:** the jar carries native libraries (Tesseract, Leptonica and
JavaCPP for Windows, macOS and Linux, unmodified from Maven Central). That is allowed (Simple
Voice Chat ships natives the same way) but moderators sometimes ask. If they do: it's offline
OCR for reading screenshots, open source, Apache 2.0, nothing is downloaded at run time, and
`THIRD-PARTY-NOTICES.md` lists every component.

---

## Before you upload

1. **Play it in the launcher** (the jar is already in `.minecraft\mods`): press B, import a list,
   K a chest, J to find, and try **Read latest screenshot** on a real YouTube frame
   (Win+Shift+S saves to Pictures\Screenshots, which the button checks).
2. **macOS and Linux OCR are untested.** The natives are bundled and load the same way, but they
   have only been run on Windows. Worth one try on the Mac before announcing.
3. Create the GitHub repo and make it public so the source and issue links work, then tag
   `v0.1.0`.
4. Upload to both stores before announcing anywhere.
