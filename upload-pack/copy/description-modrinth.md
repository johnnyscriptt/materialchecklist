# Material Checklist

The build video shows the material list for about two seconds. You pause, squint, alt-tab,
write "oak planks 6 stacks" on a sticky note, and then spend the evening opening chests to see
what you already have.

Material Checklist puts that list on your screen and keeps count for you.

## Get the list in

- **Screenshot the video.** Pause on the material list, take a screenshot, and press
  **Read latest screenshot** (or drag the image onto the import window). The mod reads it with
  built-in OCR that runs entirely on your computer: no account, no API key, nothing uploaded.
  It knows the game's own font, slashed zeros and all, so Litematica material lists from videos
  come through cleanly.
- **Paste it.** Copy the list from a video description, a forum post or a spreadsheet. It
  understands `Oak Planks 3 stacks`, `64 Stone Bricks`, `Glass Pane x 2sb`, `1sb + 3st + 5`,
  `1,280`, bullets, numbering and table columns, and it shrugs off typos.
- **Drop a Litematica export.** The `.txt` table and the `.csv` export both work; it takes the
  Total column.

Whatever you import lands on a review screen first. Lines it is sure about are green; anything it
had to guess is marked so you can fix it before it goes on the list.

## Keep track

- **A live checklist on your HUD.** Each item shows what you have against what you need, in
  stacks and shulker boxes, missing items first, ticked off as you gather.
- **Your inventory counts automatically,** including the contents of shulker boxes you carry.
- **Mark the containers you're filling.** Press **Y** while looking at a chest, barrel or shulker
  box, or click **Track** at the top of its screen. Everything inside it, shulkers inside chests
  included, now counts toward the list. Marked containers keep counting when you walk away; the
  mod remembers what they held. Your ender chest counts too, if you like.
- **Tick things off by hand** for anything you sourced some other way.

## Find what's missing

Press **J** (or **Find items**) and every container within range that holds something still
missing from your list pulses gold, through walls. Open one and the useful stacks are ringed, so
you don't have to read every slot. The HUD marks which missing items turned up nearby.

## Keys

| Key | Does |
| --- | --- |
| B | Open the checklist: your lists, import, edit, settings |
| N | Show or hide the HUD |
| Y | Track or untrack the container you're looking at |
| J | Find missing items in nearby containers |

All rebindable in Options, Controls, under **Material Checklist**.

## Multiplayer

Vanilla never tells your client what's in a chest you haven't opened, so tracking and Find run
on the server. One jar, both sides.

| Where | What works |
| --- | --- |
| Singleplayer | Everything |
| LAN world you host | Everything |
| Server with the mod | Everything |
| Server without the mod | Lists, import, the HUD and inventory counting. Tracking and Find say the server needs the mod |

The server side is careful with other people's hardware: requests are rate-limited, the scan
reads the block-entity list each loaded chunk already keeps, it never loads chunks, it never
opens unopened structure loot (reading a slot is what rolls it), and each list can track at
most 256 containers.

## Settings

In the checklist's **Settings** screen, saved to `config/materialchecklist.json`:

| Setting | Default |
| --- | --- |
| HUD on or off | On |
| HUD corner | Top right |
| HUD rows | 8 |
| Hide finished items on the HUD | Off |
| Show amounts in stacks | On |
| Count ender chest | On |
| Glow missing items in containers | On |
| Find radius | 16 blocks (8 to 64) |

Lists are saved per game install in `config/materialchecklist/projects/`, so a list made in a
creative test world is still there in survival.

## Plays well with

Tested in a real launcher alongside Sodium, Iris (shader packs on and off), Litematica, MiniHUD,
Freecam and a Faithful 32x resource pack. With a shader pack on, the highlights switch to a glow
that shaders draw. The default keys stay clear of all of those.

## Requires

- Minecraft **26.3**
- Fabric Loader **0.19.5** or newer
- **Fabric API**
- **Java 25**

The jar is about 29 MB because it carries the OCR engine (Tesseract) for Windows, macOS and
Linux. It loads only the first time you read a screenshot.

## Not there yet

- OCR is English only, and reads printed text, not handwriting.
- Items are matched by type, so an enchanted pickaxe and a plain one count as the same thing.
- A shulker box you mark counts where it is placed; pick it up and it counts from your
  inventory instead, but place it somewhere new and you'll want to mark it again.

## License

MIT. The bundled OCR engine is Tesseract (Apache 2.0) with Leptonica, through JavaCPP; see
`THIRD-PARTY-NOTICES.md` in the source.
