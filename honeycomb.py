"""Find adjacent 3-number combinations in the honeycomb chart (images/honeycomb.jpg)."""

from itertools import combinations

MAX_ROUNDS = 4

# Odd rows (0-based even) have 12 cells; even rows (0-based odd) have 11 cells
# and are shifted right by half a cell.
GRID = [
    [4, 7, 3, 1, 0, 8, 5, 0, 9, 6, 3, 8],
    [8, 1, 2, 4, 9, 7, 8, 1, 5, 4, 2],
    [3, 9, 5, 8, 0, 3, 4, 8, 0, 4, 5, 9],
    [4, 0, 7, 6, 5, 1, 7, 2, 3, 6, 2],
    [5, 2, 3, 1, 2, 8, 9, 5, 9, 1, 8, 3],
    [1, 8, 9, 5, 6, 0, 8, 4, 7, 5, 0],
    [7, 2, 5, 4, 2, 3, 5, 9, 0, 6, 9, 7],
    [8, 9, 3, 8, 5, 6, 8, 4, 3, 1, 2],
    [9, 2, 4, 0, 4, 0, 5, 7, 9, 6, 8, 5],
    [3, 6, 1, 7, 1, 9, 2, 8, 4, 0, 7],
    [1, 8, 5, 9, 6, 3, 8, 3, 1, 5, 9, 5],
    [4, 0, 2, 7, 2, 4, 2, 6, 2, 4, 0],
    [7, 2, 4, 8, 2, 0, 3, 1, 9, 9, 1, 8],
    [1, 2, 0, 4, 1, 0, 8, 5, 7, 2, 3],
    [6, 8, 3, 5, 9, 6, 7, 4, 9, 5, 9, 1],
    [0, 4, 2, 7, 8, 1, 2, 3, 8, 4, 7],
]


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
