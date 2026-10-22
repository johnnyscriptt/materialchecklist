"""Builds the store gallery from the store-shot gametest's captures.

    gradlew runClientGameTest          (writes build/run/clientGameTest/screenshots/store-*.png)
    python make_store_images.py

Each capture is taken at the game window's own size, where the GUI is laid out, and enlarged
2x with nearest-neighbour so the pixel art stays crisp. gallery/clean/ gets the plain images,
gallery/ the captioned ones. Shot 02 also gets the source "video frame" set beside the result,
so the screenshot import is shown end to end.

Caption panels: translucent black card, a gold accent bar in the Find highlight's colour, Arial
Black headline over an Arial Bold sub-line. No em dashes anywhere (house rule for store text).
"""

import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
SHOTS = os.path.join(HERE, "..", "build", "run", "clientGameTest", "screenshots")
FRAME = os.path.join(HERE, "work", "litematica-frame.png")
OUT = os.path.join(HERE, "gallery")
CLEAN = os.path.join(OUT, "clean")

HEAD_FONT = r"C:\Windows\Fonts\ariblk.ttf"
SUB_FONT = r"C:\Windows\Fonts\arialbd.ttf"

GOLD = (255, 196, 0, 255)
PANEL = (0, 0, 0, 178)
WHITE = (255, 255, 255, 255)
GREY = (210, 210, 210, 255)

# output name, source capture, headline, sub-line, corner or (x, y) of the panel, max panel width
GALLERY = [
    ("01-a-live-checklist-on-your-hud.png", "store-01-hud-and-find.png",
     "A live checklist on your HUD",
     "Find lights up the nearby chests holding what you still need",
     "top-left", 900),
    ("02-screenshot-the-video.png", None,
     "Or just screenshot the video",
     "Offline OCR reads Litematica material lists, game font and all",
     "top-left", 1300),
    ("03-paste-the-list.png", "store-02-paste.png",
     "Paste the list from the description",
     "Stacks, shulkers, x64, 1,280 and typos all understood",
     (760, 400), 880),
    ("04-check-it-before-it-counts.png", "store-03-review.png",
     "Check it before it counts",
     "Anything the parser is unsure of is marked for you",
     (60, 560), 1100),
    ("05-missing-items-glow.png", "store-04-chest-glow.png",
     "Missing items glow in every chest",
     "Click Track and this chest counts toward your list from anywhere",
     (1222, 300), 500),
    ("06-all-your-builds.png", "store-05-checklist.png",
     "Every build, one list each",
     "Tick things off by hand and edit amounts",
     (40, 300), 470),
]


def enlarge(path):
    img = Image.open(path).convert("RGB")
    return img.resize((img.width * 2, img.height * 2), Image.NEAREST)


def screenshot_story():
    """The video frame on the left, what the mod made of it on the right."""
    review = enlarge(os.path.join(SHOTS, "store-06-from-screenshot.png"))
    frame = Image.open(FRAME).convert("RGB")
    w, h = review.size
    canvas = Image.new("RGB", (w, h), (18, 18, 24))
    # Left: the frame, cropped to the list and scaled to half the canvas.
    crop = frame.crop((150, 70, 1110, 560))
    left_w = int(w * 0.46)
    crop = crop.resize((left_w, int(crop.height * left_w / crop.width)), Image.LANCZOS)
    top = (h - crop.height) // 2 + 40
    canvas.paste(crop, (int(w * 0.02), top))
    # Right: the review screen.
    right_w = int(w * 0.48)
    shot = review.resize((right_w, int(review.height * right_w / review.width)), Image.NEAREST)
    canvas.paste(shot, (w - right_w - int(w * 0.02), (h - shot.height) // 2 + 40))
    d = ImageDraw.Draw(canvas)
    arrow_x = int(w * 0.02) + left_w + int(w * 0.005)
    mid = h // 2 + 40
    d.polygon([(arrow_x, mid - 22), (arrow_x + 30, mid), (arrow_x, mid + 22)], fill=GOLD[:3])
    return canvas


def text_width(font, text):
    box = font.getbbox(text)
    return box[2] - box[0]


def wrap(font, text, max_width):
    lines, line = [], ""
    for word in text.split():
        trial = (line + " " + word).strip()
        if line and text_width(font, trial) > max_width:
            lines.append(line)
            line = word
        else:
            line = trial
    return lines + ([line] if line else [])


def caption(img, head, sub, corner, max_panel):
    """A panel of fixed-size text that wraps to fit, so narrow spots get more lines, not tiny type."""
    img = img.convert("RGBA")
    w, h = img.size
    scale = w / 1920.0
    pad_x, pad_y = int(30 * scale), int(24 * scale)
    bar_w = max(3, int(7 * scale))
    margin = int(40 * scale)
    gap = int(12 * scale)
    radius = int(10 * scale)
    hf = ImageFont.truetype(HEAD_FONT, int(50 * scale))
    sf = ImageFont.truetype(SUB_FONT, int(27 * scale))
    max_text = int(max_panel * scale) - bar_w - pad_x * 2
    head_lines, sub_lines = wrap(hf, head, max_text), wrap(sf, sub, max_text)
    head_lh = int(hf.size * 1.18)
    sub_lh = int(sf.size * 1.3)
    text_w = max(text_width(f, t) for f, lines in ((hf, head_lines), (sf, sub_lines)) for t in lines)
    panel_w = bar_w + pad_x * 2 + text_w
    panel_h = pad_y * 2 + head_lh * len(head_lines) + gap + sub_lh * len(sub_lines)

    if isinstance(corner, tuple):
        px, py = corner  # top-left of the panel, in the enlarged image's pixels
    else:
        px = w - margin - panel_w if corner.endswith("right") else margin
        py = margin if corner.startswith("top") else h - margin - panel_h

    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.rounded_rectangle([px, py, px + panel_w, py + panel_h], radius=radius, fill=PANEL)
    inset = radius // 2
    d.rounded_rectangle([px + inset, py + inset, px + inset + bar_w, py + panel_h - inset],
                        radius=bar_w // 2, fill=GOLD)
    tx, ty = px + bar_w + pad_x + inset, py + pad_y
    for line in head_lines:
        d.text((tx, ty), line, font=hf, fill=WHITE)
        ty += head_lh
    ty += gap
    for line in sub_lines:
        d.text((tx, ty), line, font=sf, fill=GREY)
        ty += sub_lh
    return Image.alpha_composite(img, layer).convert("RGB")


def run():
    os.makedirs(CLEAN, exist_ok=True)
    for name, source, head, sub, corner, max_panel in GALLERY:
        img = screenshot_story() if source is None else enlarge(os.path.join(SHOTS, source))
        img.save(os.path.join(CLEAN, name))
        caption(img, head, sub, corner, max_panel).save(os.path.join(OUT, name))
        print(f"{name:42} {img.width}x{img.height}")


if __name__ == "__main__":
    run()
