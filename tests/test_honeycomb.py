"""Honeycomb tests, written as scenarios.

The scenarios live in tests/scenarios.json, shared with the Android tests
(docs/android.md A-27). Each scenario lists the rounds the user enters and,
for every round,
- the expected matches, with cells written `R<row>C<col>:<digit>` (1-based,
  as in docs/requirements.md; the digit is checked against the grid too),
- matches that exist in the grid but must be excluded, and
- the expected highlight image after that round (tests/expected/*.jpg).

Run: python3 tests/test_honeycomb.py
"""

import json
import math
import sys
import tempfile
import unittest
from itertools import combinations
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EXPECTED_DIR = ROOT / "tests" / "expected"
sys.path.insert(0, str(ROOT))

from honeycomb import GRID, delete_round, edit_round, find_combinations, highlight_rounds  # noqa: E402

try:
    from PIL import Image

    from draw_highlights import draw
except ImportError:  # Pillow not installed: image tests are skipped
    Image = None


SCENARIOS = {
    sc["name"]: sc
    for sc in json.loads((ROOT / "tests" / "scenarios.json").read_text())["scenarios"]
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

    def test_excluded(self):
        # Matches that exist in the grid but are not valid in this round
        # (e.g. they only touch a round older than the previous one).
        rounds = self.SCENARIO["rounds"]
        for n, excluded in enumerate(self.SCENARIO["excluded"]):
            with self.subTest(label(rounds, n)):
                valid = {parse(m) for m in self.SCENARIO["matches"][n]}
                for match in excluded:
                    self.assertIn(parse(match), find_combinations(GRID, rounds[n]))
                    self.assertNotIn(parse(match), valid)

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

    SCENARIO = SCENARIOS["three_rounds"]


class TestReuse(ScenarioTests, unittest.TestCase):
    """3 4 7 -> 7 8 9: round 2 may reuse a round-1 7 cell"""

    SCENARIO = SCENARIOS["reuse"]


class TestDeleteRound2(ScenarioTests, unittest.TestCase):
    """docs/requirements.md 7.6: 3 4 7 -> 0 5 1 -> 2 9 3, then delete round 2"""

    SCENARIO = SCENARIOS["delete_round_2"]


class TestEditRound2(ScenarioTests, unittest.TestCase):
    """docs/requirements.md 7.6: 3 4 7 -> 0 5 1 -> 2 9 3, then edit round 2 to 7 8 9"""

    SCENARIO = SCENARIOS["edit_round_2"]


class TestEditDelete(unittest.TestCase):
    """R-11, R-12: the list operations; O-5 is covered by the scenarios above."""

    BASE = SCENARIOS["three_rounds"]["rounds"]

    def test_delete_moves_later_rounds_up(self):
        self.assertEqual(delete_round(self.BASE, 1), SCENARIOS["delete_round_2"]["rounds"])

    def test_edit_replaces_digits_only(self):
        self.assertEqual(edit_round(self.BASE, 1, [7, 8, 9]), SCENARIOS["edit_round_2"]["rounds"])

    def test_inputs_are_not_changed(self):
        before = [list(r) for r in self.BASE]
        edit_round(self.BASE, 0, [1, 2, 3])
        delete_round(self.BASE, 0)
        self.assertEqual(self.BASE, before)

    def test_bad_arguments(self):
        with self.assertRaises(IndexError):
            delete_round(self.BASE, 3)
        with self.assertRaises(IndexError):
            edit_round(self.BASE, -1, [1, 2, 3])
        with self.assertRaises(ValueError):
            edit_round(self.BASE, 0, [1, 2])


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
