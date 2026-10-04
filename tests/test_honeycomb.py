"""Honeycomb tests, written as scenarios.

Each scenario lists the rounds the user enters and, for every round,
- the expected matches, with cells written `R<row>C<col>:<digit>` (1-based,
  as in docs/requirements.md; the digit is checked against the grid too), and
- the expected highlight image after that round (tests/expected/*.jpg).

Run: python3 tests/test_honeycomb.py
"""

import math
import sys
import tempfile
import unittest
from itertools import combinations
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EXPECTED_DIR = ROOT / "tests" / "expected"
sys.path.insert(0, str(ROOT))

from honeycomb import GRID, find_combinations, highlight_rounds  # noqa: E402

try:
    from PIL import Image

    from draw_highlights import draw
except ImportError:  # Pillow not installed: image tests are skipped
    Image = None


# ---------------------------------------------------------------------------
# Scenarios
# ---------------------------------------------------------------------------

THREE_ROUNDS = {
    "rounds": [[3, 4, 7], [0, 5, 1], [2, 9, 3]],
    "matches": [
        # Round 1: 3 4 7, anywhere in the grid
        [
            ("R1C1:4", "R1C2:7", "R1C3:3"),
            ("R2C6:7", "R3C6:3", "R3C7:4"),
            ("R3C6:3", "R3C7:4", "R4C7:7"),
            ("R8C8:4", "R8C9:3", "R9C8:7"),
            ("R15C7:7", "R15C8:4", "R16C8:3"),
        ],
        # Round 2: 0 5 1, touching round 1
        [
            ("R1C7:5", "R1C8:0", "R2C8:1"),
            ("R2C2:1", "R3C3:5", "R4C2:0"),
            ("R3C5:0", "R4C5:5", "R4C6:1"),
            ("R9C6:0", "R9C7:5", "R10C5:1"),
        ],
        # Round 3: 2 9 3, touching round 2 (round 1 is ignored)
        [
            ("R5C2:2", "R5C3:3", "R6C3:9"),
            ("R10C6:9", "R10C7:2", "R11C6:3"),
            ("R10C6:9", "R10C7:2", "R11C8:3"),
            ("R10C6:9", "R11C6:3", "R12C5:2"),
        ],
    ],
    "images": ["347.jpg", "347_051.jpg", "347_051_293.jpg"],
}

REUSE = {
    "rounds": [[3, 4, 7], [7, 8, 9]],
    "matches": [
        THREE_ROUNDS["matches"][0],
        # Round 2: 7 8 9; all but the last reuse a round-1 7 cell
        [
            ("R1C2:7", "R2C1:8", "R3C2:9"),
            ("R1C6:8", "R2C5:9", "R2C6:7"),
            ("R2C5:9", "R2C6:7", "R2C7:8"),
            ("R3C8:8", "R4C7:7", "R5C7:9"),
            ("R4C7:7", "R5C6:8", "R5C7:9"),
            ("R4C7:7", "R5C7:9", "R6C7:8"),
            ("R7C8:9", "R8C7:8", "R9C8:7"),
            ("R8C7:8", "R9C8:7", "R9C9:9"),
            ("R9C8:7", "R9C9:9", "R10C8:8"),
            ("R14C9:7", "R15C9:9", "R16C9:8"),  # touches only
        ],
    ],
    "images": ["347.jpg", "347_789.jpg"],
}


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def parse(match):
    """("R1C2:7", ...) -> sorted ((0, 1), ...); checks each digit against GRID."""
    cells = []
    for text in match:
        pos, digit = text.split(":")
        r, c = (int(x) - 1 for x in pos[1:].split("C"))
        assert GRID[r][c] == int(digit), f"{text}: grid has {GRID[r][c]}"
        cells.append((r, c))
    return tuple(sorted(cells))


def label(rounds, n):
    return f"round {n + 1} ({' '.join(map(str, rounds[n]))})"


def brute_force(grid, digits, previous=None):
    """Independent check: adjacency from hexagon centre distance, all 3-cell subsets."""
    def centre(r, c):
        return (c + (0.5 if r % 2 else 0.0), r * math.sqrt(3) / 2)

    def adjacent(a, b):
        return abs(math.dist(centre(*a), centre(*b)) - 1) < 1e-9

    cells = [(r, c) for r, row in enumerate(grid) for c in range(len(row)) if row[c] in digits]
    found = []
    for trio in combinations(cells, 3):
        if sorted(grid[r][c] for r, c in trio) != sorted(digits):
            continue
        if sum(adjacent(a, b) for a, b in combinations(trio, 2)) < 2:
            continue
        if previous is not None and not (
            previous.intersection(trio)
            or any(adjacent(a, p) for a in trio for p in previous)
        ):
            continue
        found.append(tuple(sorted(trio)))
    return sorted(found)


class ScenarioTests:
    """Mixin: checks matches, highlights and images for SCENARIO."""

    SCENARIO = None

    def test_matches(self):
        rounds = self.SCENARIO["rounds"]
        previous = None
        for n, expected in enumerate(self.SCENARIO["matches"]):
            with self.subTest(label(rounds, n)):
                found = find_combinations(GRID, rounds[n], previous)
                self.assertEqual(found, sorted(parse(m) for m in expected))
                self.assertEqual(found, brute_force(GRID, rounds[n], previous))
            previous = {cell for combo in found for cell in combo}

    def test_highlights(self):
        rounds = self.SCENARIO["rounds"]
        highlights = highlight_rounds(GRID, rounds)
        for n, expected in enumerate(self.SCENARIO["matches"]):
            with self.subTest(label(rounds, n)):
                cells = {cell for m in expected for cell in parse(m)}
                self.assertEqual(highlights[n], cells)

    @unittest.skipIf(Image is None, "Pillow not installed")
    def test_images(self):
        rounds = self.SCENARIO["rounds"]
        with tempfile.TemporaryDirectory() as tmp:
            for n, name in enumerate(self.SCENARIO["images"]):
                with self.subTest(f"after {label(rounds, n)}: {name}"):
                    out = Path(tmp) / name
                    draw(rounds[: n + 1], out)
                    with Image.open(out) as got, Image.open(EXPECTED_DIR / name) as want:
                        self.assertEqual(got.tobytes(), want.tobytes())


class TestThreeRounds(ScenarioTests, unittest.TestCase):
    """3 4 7 -> 0 5 1 -> 2 9 3"""

    SCENARIO = THREE_ROUNDS

    def test_round_3_ignores_round_1(self):
        # These 2 9 3 matches touch round 1 but not round 2, so they are excluded.
        for match in [("R4C8:2", "R4C9:3", "R5C9:9"), ("R15C9:9", "R16C7:2", "R16C8:3")]:
            self.assertIn(parse(match), find_combinations(GRID, [2, 9, 3]))
            self.assertNotIn(match, THREE_ROUNDS["matches"][2])


class TestReuse(ScenarioTests, unittest.TestCase):
    """3 4 7 -> 7 8 9: round 2 may reuse a round-1 7 cell"""

    SCENARIO = REUSE


class TestRules(unittest.TestCase):
    def test_order_does_not_matter(self):
        self.assertEqual(find_combinations(GRID, [7, 4, 3]), find_combinations(GRID, [3, 4, 7]))

    def test_four_rounds_allowed(self):
        self.assertEqual(len(highlight_rounds(GRID, [[3, 4, 7]] * 4)), 4)

    def test_fifth_round_rejected(self):
        with self.assertRaises(ValueError):
            highlight_rounds(GRID, [[3, 4, 7]] * 5)

    def test_empty_round_empties_later_rounds(self):
        # No 0 0 0 combination exists, so round 2 has nothing to touch.
        self.assertEqual(highlight_rounds(GRID, [[0, 0, 0], [3, 4, 7]]), [set(), set()])


if __name__ == "__main__":
    unittest.main(verbosity=2)
