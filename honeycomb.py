"""Core logic (docs/requirements.md): find adjacent 3-number combinations in the honeycomb chart."""

import json
from itertools import combinations
from pathlib import Path

MAX_ROUNDS = 4

# Shared with the Android app (docs/android.md A-27).
# Odd rows (0-based even) have 12 cells; even rows (0-based odd) have 11 cells
# and are shifted right by half a cell.
GRID = json.loads((Path(__file__).resolve().parent / "data" / "grid.json").read_text())


def neighbours(grid, r, c):
    """Return (row, col) of every cell sharing a side with (r, c)."""
    up_down = (c - 1, c) if r % 2 == 0 else (c, c + 1)
    candidates = [(r, c - 1), (r, c + 1)]
    candidates += [(r + dr, col) for dr in (-1, 1) for col in up_down]
    return [
        (nr, nc)
        for nr, nc in candidates
        if 0 <= nr < len(grid) and 0 <= nc < len(grid[nr])
    ]


def touches(grid, cells, previous):
    """True if any of `cells` shares a side with any cell in `previous`."""
    return any(n in previous for cell in cells for n in neighbours(grid, *cell))


def find_combinations(grid, digits, previous=None):
    """Return every connected group of 3 cells whose digits match `digits` in any order.

    If `previous` (a set of highlighted cells) is given, keep only groups that
    share at least one side with it or reuse at least one of its cells.
    Each result is a tuple of 3 (row, col) cells, sorted; results are sorted too.
    """
    target = sorted(digits)
    found = set()
    for r, row in enumerate(grid):
        for c in range(len(row)):
            # A connected triple always has a cell adjacent to the other two.
            for a, b in combinations(neighbours(grid, r, c), 2):
                cells = tuple(sorted([a, (r, c), b]))
                if sorted(grid[x][y] for x, y in cells) == target:
                    found.add(cells)
    if previous is not None:
        found = {
            cells for cells in found
            if previous.intersection(cells) or touches(grid, cells, previous)
        }
    return sorted(found)


def highlighted_cells(grid, digits, previous=None):
    """Return the set of cells to highlight for `digits`."""
    return {cell for combo in find_combinations(grid, digits, previous) for cell in combo}


def highlight_rounds(grid, rounds):
    """Apply each round of digits in turn; round N is limited to cells touching
    or reusing round N-1 (earlier rounds are ignored).

    Returns one set of highlighted cells per round. Once a round has no match,
    every later round is empty too.
    """
    if len(rounds) > MAX_ROUNDS:
        raise ValueError(f"at most {MAX_ROUNDS} rounds, got {len(rounds)}")
    result, previous = [], None
    for digits in rounds:
        previous = highlighted_cells(grid, digits, previous)
        result.append(previous)
    return result


def edit_round(rounds, n, digits):
    """R-11: replace the digits of round n (0-based); later rounds are recomputed
    by highlight_rounds (O-5)."""
    if not 0 <= n < len(rounds):
        raise IndexError(f"no round {n + 1}")
    if len(digits) != 3 or any(not 0 <= d <= 9 for d in digits):
        raise ValueError(f"a round is 3 digits: {digits}")
    return rounds[:n] + [list(digits)] + rounds[n + 1:]


def delete_round(rounds, n):
    """R-12: remove round n (0-based); later rounds move up one place."""
    if not 0 <= n < len(rounds):
        raise IndexError(f"no round {n + 1}")
    return rounds[:n] + rounds[n + 1:]
