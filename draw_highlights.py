"""Draw each round's highlight over the chart image (images/honeycomb.jpg).

usage: python3 draw_highlights.py 3,4,7 0,5,1 [-o out.jpg]
"""

import argparse
import math
from pathlib import Path

from PIL import Image, ImageDraw

from honeycomb import GRID, MAX_ROUNDS, highlight_rounds

ROOT = Path(__file__).resolve().parent
SRC = ROOT / "images" / "honeycomb.jpg"

# One colour per round: red, blue, green, orange.
COLOURS = [(220, 0, 0), (0, 90, 230), (0, 160, 60), (230, 140, 0)]

# Measured digit centres (pixels) at the ends of the first/last long and short
# rows; every other centre is interpolated, which absorbs any remaining tilt.
LONG = {0: ((27.9, 41.7), (727.5, 35.8)), 14: ((27.9, 790.8), (728.5, 788.3))}
SHORT = {1: ((59.0, 95.3), (696.6, 91.4)), 15: ((61.0, 844.1), (692.0, 838.8))}
RADIUS = 35


def lerp(a, b, t):
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)


def centre(r, c):
    """Pixel centre of cell (r, c) in images/honeycomb.jpg."""
    ref = LONG if r % 2 == 0 else SHORT
    (r0, (l0, e0)), (r1, (l1, e1)) = sorted(ref.items())
    t = (r - r0) / (r1 - r0)
    left, right = lerp(l0, l1, t), lerp(e0, e1, t)
    return lerp(left, right, c / (len(GRID[r]) - 1))


def hexagon(cx, cy, rad):
    """Vertices of a pointy-top hexagon."""
    return [(cx + rad * math.cos(math.radians(90 + 60 * i)),
             cy + rad * math.sin(math.radians(90 + 60 * i))) for i in range(6)]


def draw(rounds, out):
    img = Image.open(SRC).convert("RGBA")
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    # A cell in several rounds shows only the latest round's colour.
    latest = {}
    for i, cells in enumerate(highlight_rounds(GRID, rounds)):
        latest.update({cell: COLOURS[i] for cell in cells})
    for cell, colour in latest.items():
        hexa = hexagon(*centre(*cell), RADIUS - 2)
        d.polygon(hexa, fill=colour + (90,))
        d.polygon(hexa, outline=colour + (255,), width=5)
    Image.alpha_composite(img, overlay).convert("RGB").save(out, quality=95)


def parse_round(text):
    digits = [int(x) for x in text.split(",") if x.strip().isdigit()]
    if len(digits) != 3 or any(not 0 <= x <= 9 for x in digits):
        raise argparse.ArgumentTypeError(f"expected 3 digits like 3,4,7, got {text!r}")
    return digits


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("rounds", nargs="+", type=parse_round,
                        help=f"1 to {MAX_ROUNDS} rounds, each 3 comma-separated digits")
    parser.add_argument("-o", "--out", type=Path,
                        help="output file (default: images/highlight_<rounds>.jpg)")
    args = parser.parse_args()
    if len(args.rounds) > MAX_ROUNDS:
        parser.error(f"at most {MAX_ROUNDS} rounds")
    name = "_".join("".join(map(str, r)) for r in args.rounds)
    out = args.out or ROOT / "images" / f"highlight_{name}.jpg"
    draw(args.rounds, out)
    print(out)


if __name__ == "__main__":
    main()
