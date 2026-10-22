"""Draws a Litematica-style material list, in the game's own font, the way it looks in a paused
YouTube video: a dark panel over the world, an icon square per row, coloured columns, a drop
shadow, then shrunk and JPEG-compressed like a 1080p stream.

The font is read from the Minecraft client jar in Loom's cache; it is never copied into the repo.

    python make_litematica_frame.py <output.png>
"""
import io
import random
import sys
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

JAR = (Path.home() / ".gradle" / "caches" / "fabric-loom" / "minecraftMaven" / "net" / "minecraft"
       / "minecraft-clientonly-deobf" / "26.3" / "minecraft-clientonly-deobf-26.3.jar")

ROWS = [
    ("Spruce Planks", 448, 320), ("Stripped Spruce Log", 96, 96), ("Cobblestone", 192, 64),
    ("Stone Bricks", 1088, 1088), ("Spruce Stairs", 140, 140), ("Glass Pane", 52, 0),
    ("Spruce Trapdoor", 24, 24), ("Lantern", 12, 0), ("Oak Leaves", 230, 230), ("Flower Pot", 6, 6),
]


def load_font():
    with zipfile.ZipFile(JAR) as jar:
        atlas = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/font/ascii.png"))).convert("RGBA")
    glyphs = {}
    for code in range(33, 127):
        cx, cy = (code % 16) * 8, (code // 16) * 8
        cells = [[atlas.getpixel((cx + x, cy + y))[3] > 0 for x in range(8)] for y in range(8)]
        width = max((x + 1 for y in range(8) for x in range(8) if cells[y][x]), default=0)
        glyphs[chr(code)] = (cells, width)
    return glyphs


def text(draw, glyphs, s, x, y, scale, colour):
    shadow = tuple(c // 4 for c in colour)
    for ox, oy, col in ((1, 1, shadow), (0, 0, colour)):
        pen = x + ox
        for ch in s:
            if ch == " ":
                pen += 4
                continue
            cells, width = glyphs[ch]
            for r in range(8):
                for c in range(width):
                    if cells[r][c]:
                        px, py = (pen + c) * scale, (y + oy + r) * scale
                        draw.rectangle([px, py, px + scale - 1, py + scale - 1], fill=col)
            pen += width + 1


def main(out):
    glyphs = load_font()
    scale = 4
    w, h = 480, 270  # font pixels; at scale 4 this is a 1920x1080 frame
    img = Image.new("RGB", (w * scale, h * scale), (120, 170, 255))
    draw = ImageDraw.Draw(img)
    rnd = random.Random(7)
    for gx in range(0, w * scale, 16 * scale // 4):
        for gy in range(h * scale // 2, h * scale, 16 * scale // 4):
            g = 120 + rnd.randint(-25, 25)
            draw.rectangle([gx, gy, gx + 15, gy + 15], fill=(70, g, 45))
    img = img.filter(ImageFilter.GaussianBlur(3))
    draw = ImageDraw.Draw(img, "RGBA")
    px0, py0, pw, ph = 60, 30, 300, 14 + 12 * (len(ROWS) + 1)
    draw.rectangle([px0 * scale, py0 * scale, (px0 + pw) * scale, (py0 + ph) * scale], fill=(16, 16, 24, 225))
    draw = ImageDraw.Draw(img)
    cols = [px0 + 18, px0 + 150, px0 + 195, px0 + 245]
    for c, label in zip(cols, ["Item", "Total", "Missing", "Available"]):
        text(draw, glyphs, label, c, py0 + 4, scale, (255, 255, 160))
    icon_colours = [(130, 95, 55), (150, 110, 70), (125, 125, 125), (110, 110, 115), (120, 85, 50),
                    (200, 225, 235), (105, 75, 45), (240, 190, 90), (60, 130, 40), (150, 80, 50)]
    for i, (name, total, missing) in enumerate(ROWS):
        y = py0 + 4 + 12 * (i + 1) + 2
        draw.rectangle([(px0 + 5) * scale, (y - 1) * scale, (px0 + 14) * scale, (y + 8) * scale],
                       fill=icon_colours[i % len(icon_colours)])
        text(draw, glyphs, name, cols[0], y, scale, (255, 255, 255))
        text(draw, glyphs, str(total), cols[1], y, scale, (255, 255, 255))
        text(draw, glyphs, str(missing), cols[2], y, scale, (255, 85, 85) if missing else (85, 255, 85))
        text(draw, glyphs, str(total - missing), cols[3], y, scale, (85, 255, 85))
    # A 1440p recording shown at 1080p, then streamed.
    frame = img.resize((int(img.width * 0.75), int(img.height * 0.75)), Image.BILINEAR)
    buf = io.BytesIO()
    frame.save(buf, "JPEG", quality=70)
    Image.open(io.BytesIO(buf.getvalue())).save(out)
    print("wrote", out, frame.size)


if __name__ == "__main__":
    main(sys.argv[1])
