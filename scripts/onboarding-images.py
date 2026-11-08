"""Turn the black-on-transparent source art in design/onboarding/src into app assets.

- Writes res/drawable-nodpi/onboarding_<name>.webp: a white alpha mask, tinted at runtime with
  the look's accent (ColorFilter.tint). Heroes are 1080 px wide; spots are 384 px square.
- Writes design/onboarding/contact-sheet.png: every image tinted on all six look backgrounds,
  for review.

Usage: python scripts/onboarding-images.py
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "design" / "onboarding" / "src"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
SHEET = ROOT / "design" / "onboarding" / "contact-sheet.png"

# (name, background, accent) from ui/theme/Color.kt
LOOKS = [
    ("Obsidian dark", "#0F0F0E", "#C9A45C"), ("Obsidian light", "#F3F2EF", "#8A6726"),
    ("Stone light", "#ECEBE7", "#B85E17"), ("Stone dark", "#151716", "#E08A3C"),
    ("Sage dark", "#0C1310", "#93B8A0"), ("Sage light", "#EEF2EF", "#3F6B53"),
]


def mask(src: Path) -> Image.Image:
    """Alpha = line strength. Works for true transparency and for black-on-white fallbacks."""
    im = Image.open(src).convert("RGBA")
    r, g, b, a = im.split()
    lum = Image.merge("RGB", (r, g, b)).convert("L")
    # Dark pixels are line; light pixels are background. Combine with existing alpha.
    line = lum.point(lambda v: 255 - v)
    alpha = Image.composite(line, Image.new("L", im.size, 0), a)
    # Generated hairlines are faint; lift them so they read at phone size, and drop speckle noise.
    alpha = alpha.point(lambda v: 0 if v < 10 else min(255, round(255 * (v / 255) ** 0.45)))
    white = Image.new("L", im.size, 255)
    return Image.merge("RGBA", (white, white, white, alpha))


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    sources = sorted(SRC.glob("*.png"))
    if not sources:
        raise SystemExit(f"No PNGs in {SRC}")
    assets = []
    for src in sources:
        m = mask(src)
        width = 384 if src.stem.startswith("spot_") else 1080
        m = m.resize((width, round(m.height * width / m.width)), Image.LANCZOS)
        bbox = m.getchannel("A").getbbox()
        dest = OUT / f"onboarding_{src.stem}.webp"
        m.save(dest, "WEBP", lossless=False, quality=88, method=6, exact=False)
        assets.append((src.stem, m, dest.stat().st_size, bbox))
        print(f"{dest.name}: {m.width}x{m.height}, {dest.stat().st_size // 1024} KB, ink box {bbox}")

    cell_w, cell_h, label = 360, 240, 22
    sheet = Image.new("RGB", (cell_w * len(LOOKS), (cell_h + label) * len(assets)), "#888888")
    draw = ImageDraw.Draw(sheet)
    for row, (name, m, size, _) in enumerate(assets):
        for col, (look, bg, accent) in enumerate(LOOKS):
            x, y = col * cell_w, row * (cell_h + label)
            cell = Image.new("RGB", (cell_w, cell_h), bg)
            art = m.copy()
            art.thumbnail((cell_w - 20, cell_h - 20), Image.LANCZOS)
            tint = Image.new("RGBA", art.size, accent)
            tint.putalpha(art.getchannel("A"))
            cell.paste(tint, ((cell_w - art.width) // 2, (cell_h - art.height) // 2), tint)
            sheet.paste(cell, (x, y + label))
            draw.text((x + 6, y + 4), f"{name} · {look} · {size // 1024} KB", fill="#000000")
    sheet.save(SHEET)
    print(f"Contact sheet: {SHEET}")


if __name__ == "__main__":
    main()
