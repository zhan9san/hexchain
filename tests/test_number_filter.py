"""Number filter tests (docs/requirements.md section 8).

The filter scenarios live in tests/scenarios.json ("filter_scenarios"), shared
with the Android tests (docs/android.md A-45).

Run: python3 -m unittest discover -s tests
"""

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from number_filter import KEEP, KILL, filter_numbers  # noqa: E402

FILTER_SCENARIOS = json.loads((ROOT / "tests" / "scenarios.json").read_text())["filter_scenarios"]


class TestFilterScenarios(unittest.TestCase):
    def test_scenarios(self):
        for sc in FILTER_SCENARIOS:
            with self.subTest(sc["name"]):
                r = filter_numbers(sc["input"], sc["digit"], sc["mode"])
                self.assertEqual(r.kept, sc["kept"])
                self.assertEqual(r.removed, sc["removed"])
                self.assertEqual([list(d) for d in r.duplicates], sc["duplicates"])
                self.assertEqual(r.duplicate_count, sc["duplicate_count"])
                self.assertEqual(r.invalid, sc["invalid"])


class TestFilterRules(unittest.TestCase):
    def test_separators(self):
        # F-1: space, tab, line break, full-width space, all three commas, mixed and repeated.
        r = filter_numbers("111 222\t333\n444　555,666，777、888 ,， 999", 0, KILL)
        self.assertEqual(r.kept, ["111", "222", "333", "444", "555", "666", "777", "888", "999"])
        self.assertEqual(r.invalid, [])

    def test_invalid_tokens(self):
        # F-2: only exactly 3 ASCII digits are valid, reported in input order.
        r = filter_numbers("12 1234 1a3 ３４７ 007", 7, KEEP)
        self.assertEqual(r.invalid, ["12", "1234", "1a3", "３４７"])
        self.assertEqual(r.kept, ["007"])

    def test_duplicates_count_extra_copies(self):
        # F-3: 347 x3 and 468 x2 give 3 duplicates; each number counts once.
        r = filter_numbers("347 347 347 468 468", 7, KILL)
        self.assertEqual(r.duplicate_count, 3)
        self.assertEqual(r.duplicates, [("347", 3), ("468", 2)])
        self.assertEqual((r.kept, r.removed), (["468"], ["347"]))

    def test_kill_and_keep_swap(self):
        # F-7: kept + removed = distinct numbers; kill and keep swap them.
        text = "347 468 986 707 123 347"
        kill, keep = filter_numbers(text, 7, KILL), filter_numbers(text, 7, KEEP)
        self.assertEqual((kill.kept, kill.removed), (keep.removed, keep.kept))
        self.assertEqual(len(kill.kept) + len(kill.removed), 5)

    def test_bad_arguments(self):
        with self.assertRaises(ValueError):
            filter_numbers("347", 10, KILL)
        with self.assertRaises(ValueError):
            filter_numbers("347", 7, "remove")


if __name__ == "__main__":
    unittest.main(verbosity=2)
