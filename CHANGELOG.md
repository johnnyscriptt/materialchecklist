# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-10-22

### Added

- Material lists you can import three ways: paste or type them, drop a Litematica `.txt` or
  `.csv` export, or read a screenshot with built-in offline OCR.
- A parser that understands stacks, shulker boxes, double chests, `x64`, `1sb + 3st + 5`,
  thousands separators, bullets, numbering and table columns, with fuzzy item-name matching that
  forgives typos and common OCR mix-ups.
- A game-font number reader, so amounts in Minecraft's own font (slashed zero and all) are read
  exactly rather than guessed.
  It copes with rescaled and blurred web screenshots, and reads cells lined up in a column of
  numbers as numbers even when the general OCR saw letters.
- A review screen: every imported line is shown with its match and amount, and anything
  uncertain is marked for checking before it goes on the list.
- A HUD checklist: have against need for every item, in stacks and shulker boxes, missing items
  first, with a progress bar. Corner, row count and hide-finished are configurable.
- Inventory counting, including the contents of carried shulker boxes.
- Container tracking: mark chests, barrels and shulker boxes with Y or the Track button; their
  contents, nested shulkers included, count toward the list, and keep counting from a saved
  snapshot while their chunk is unloaded. Optional ender chest counting.
- Find items (J): every nearby container holding something still missing pulses gold through
  walls, and the matching slots glow when it is opened.
- Several lists at once, saved per game install, with manual tick-off, editable amounts and an
  item picker.
- Works with Iris shader packs (highlights switch to a glow shaders draw), and reads amounts against
  both the vanilla font and a resource pack's font. Default keys avoid Iris, Litematica and MiniHUD.

### Known limitations

- OCR reads English printed text only.
- A row covered by a hover tooltip in the screenshot can be missed; the review screen shows what
  was read so it can be added by hand.
- Items match by type; enchantments and names are ignored.
- A tracked shulker box that is picked up and placed elsewhere needs marking again.
