# Honeycomb Number Finder — Requirements

## 1. Background

`images/honeycomb.jpg` is a honeycomb chart of hexagons, each holding a
single digit (0–9). The user enters rounds of 3 digits; the app finds and
highlights connected cells holding those digits, each round searching around
the previous one.

## 2. Definitions

- **Cell**: one complete hexagon containing a digit. Partial (cut-off)
  hexagons on the left/right edges have no digit and are ignored.
- **Adjacent**: two cells are adjacent if they share at least one side.
  Each cell has up to 6 neighbours.
- **Combination**: three distinct cells that form one connected shape
  through adjacency (allowed shapes: R-3).
- **Round**: one input of exactly 3 digits. Rounds are numbered 1 to 4 in
  the order they are entered.
- **Match**: a combination whose digits equal the round's digits (R-4).
- **Highlight set `H(n)`**: all cells of all valid matches of round `n`.
- **Touches**: a combination touches a set of cells if at least one of its
  cells shares a side with at least one cell of that set.
- **Overlaps**: a combination overlaps a set of cells if at least one of its
  cells is in that set.

## 3. Grid Layout

- 16 rows, pointy-top hexagons in offset rows.
- Odd rows (1, 3, 5 …) have 12 cells; even rows (2, 4, 6 …) have 11 cells and
  are shifted right by half a cell.
- Total: 8 × 12 + 8 × 11 = 184 cells.

### 3.1 Neighbour Rule (0-based row `r`, column `c`)

| Direction   | Long row (`r` even, 12 cells) | Short row (`r` odd, 11 cells) |
| ----------- | ----------------------------- | ----------------------------- |
| Left        | `(r, c-1)`                    | `(r, c-1)`                    |
| Right       | `(r, c+1)`                    | `(r, c+1)`                    |
| Upper-left  | `(r-1, c-1)`                  | `(r-1, c)`                    |
| Upper-right | `(r-1, c)`                    | `(r-1, c+1)`                  |
| Lower-left  | `(r+1, c-1)`                  | `(r+1, c)`                    |
| Lower-right | `(r+1, c)`                    | `(r+1, c+1)`                  |

Coordinates outside the grid are discarded.

### 3.2 Initial Data

Transcribed from `images/honeycomb.jpg` and verified against the image.

```text
R01: 4 7 3 1 0 8 5 0 9 6 3 8
R02:  8 1 2 4 9 7 8 1 5 4 2
R03: 3 9 5 8 0 3 4 8 0 4 5 9
R04:  4 0 7 6 5 1 7 2 3 6 2
R05: 5 2 3 1 2 8 9 5 9 1 8 3
R06:  1 8 9 5 6 0 8 4 7 5 0
R07: 7 2 5 4 2 3 5 9 0 6 9 7
R08:  8 9 3 8 5 6 8 4 3 1 2
R09: 9 2 4 0 4 0 5 7 9 6 8 5
R10:  3 6 1 7 1 9 2 8 4 0 7
R11: 1 8 5 9 6 3 8 3 1 5 9 5
R12:  4 0 2 7 2 4 2 6 2 4 0
R13: 7 2 4 8 2 0 3 1 9 9 1 8
R14:  1 2 0 4 1 0 8 5 7 2 3
R15: 6 8 3 5 9 6 7 4 9 5 9 1
R16:  0 4 2 7 8 1 2 3 8 4 7
```

## 4. Scope

In scope:

- Android app.
- The single chart in `images/honeycomb.jpg` (section 3.2).

Out of scope:

- Loading or editing other charts.
- Undoing a single round or editing an earlier round (see R-10).

## 5. Rules

### 5.1 Input

1. **R-1 Round size**: every round has exactly 3 digits (each 0–9).
   Repeated digits are allowed (e.g. `1 1 5`, `7 7 7`).
2. **R-2 Round count**: the user enters 1 to 4 rounds, one after another.
   A 5th round is rejected with a message; the user must clear first.

### 5.2 Matching

1. **R-3 Shape**: a combination is any 3 connected cells, either
   - a chain `A–B–C` (B adjacent to both A and C), or
   - a triangle (all three mutually adjacent).
2. **R-4 Order**: digits match in any order. Input `3 4 7` matches cells
   reading 3-4-7, 7-3-4, 4-7-3, etc.

### 5.3 Round Chaining

1. **R-5 Round 1**: valid matches are all matches anywhere in the grid.
2. **R-6 Round n (n = 2 … 4)**: valid matches are the matches that touch
   **or** overlap `H(n-1)`. Any cell of `H(n-1)` counts; the user does not
   pick a specific previous match.
3. **R-7 Reuse**: a round-n match may reuse cells of `H(n-1)`. E.g. after
   `3 4 7`, round `7 8 9` may start from a 7 cell already highlighted by
   round 1 (see 7.5).
4. **R-8 Previous round only**: round `n` is checked against round `n-1`
   only. Rounds `n-2` and earlier are ignored.
5. **R-9 Empty chain**: if round `n-1` has no valid match, `H(n-1)` is empty
   and round `n` (and every later round) has no valid match either. The
   input is still accepted and shown as "no match".

### 5.4 Clear

1. **R-10 Clear all**: the user can clear all numbers. Clearing removes
   every round and every highlight, returning the diagram to its original
   state; the next input becomes round 1 again. Clearing is the only way to
   change entered rounds.

## 6. Functional Requirements

1. **FR-1 Display**: show the chart from `images/honeycomb.jpg` with highlights
   drawn over its hexagons.
2. **FR-2 Input**: input 3 digits per round (R-1, R-2). Show the rounds
   already entered, in order.
3. **FR-3 Search**: when a round is entered, find its valid matches
   (R-3 to R-9). Each match is counted once regardless of the order its
   cells were discovered.
4. **FR-4 Highlight**: highlight `H(n)` for every entered round. Each round
   has one colour for all its matches, and all rounds stay visible
   together. When a cell belongs to more than one round (R-7, R-8), the
   later round's colour is shown on top.
5. **FR-5 Result info**: per round, show the number of valid matches, or
   "no match" when there are none.
6. **FR-6 Clear**: provide a clear action that implements R-10.

## 7. Examples

### 7.1 Single Combination

Input `4 7 8`: cells R01C1 (4), R01C2 (7), R02C1 (8) are mutually adjacent
(triangle), so all three are highlighted.

### 7.2 Round 1: `3 4 7`

5 matches, 13 cells (red in `tests/expected/347.jpg`):

| # | Cells               | Digits | Shape    |
| - | ------------------- | ------ | -------- |
| 1 | R1C1, R1C2, R1C3    | 4 7 3  | chain    |
| 2 | R2C6, R3C6, R3C7    | 7 3 4  | triangle |
| 3 | R3C6, R3C7, R4C7    | 3 4 7  | chain    |
| 4 | R8C8, R8C9, R9C8    | 4 3 7  | chain    |
| 5 | R15C7, R15C8, R16C8 | 7 4 3  | chain    |

### 7.3 Round 2: `0 5 1` around round 1

`0 5 1` has 12 matches in the whole grid; 4 of them touch `H(1)` (blue in `tests/expected/347_051.jpg`):

| # | Cells             | Digits | Touches round-1 cell(s)      |
| - | ----------------- | ------ | ---------------------------- |
| 1 | R1C7, R1C8, R2C8  | 5 0 1  | R2C6 (7)                     |
| 2 | R2C2, R3C3, R4C2  | 1 5 0  | R1C2 (7), R1C3 (3)           |
| 3 | R3C5, R4C5, R4C6  | 0 5 1  | R3C6 (3), R3C7 (4), R4C7 (7) |
| 4 | R9C6, R9C7, R10C5 | 0 5 1  | R9C8 (7)                     |

### 7.4 Round 3: `2 9 3` around round 2

`2 9 3` has 14 matches in the whole grid; 4 of them touch `H(2)` (green in
`tests/expected/347_051_293.jpg`). Round 1 is ignored (R-8):

| # | Cells               | Digits | Touches round-2 cell(s)          |
| - | ------------------- | ------ | -------------------------------- |
| 1 | R5C2, R5C3, R6C3    | 2 3 9  | R4C2 (0)                         |
| 2 | R10C6, R10C7, R11C6 | 9 2 3  | R9C6 (0), R9C7 (5), R10C5 (1)    |
| 3 | R10C6, R10C7, R11C8 | 9 2 3  | R9C6 (0), R9C7 (5), R10C5 (1)    |
| 4 | R10C6, R11C6, R12C5 | 9 3 2  | R9C6 (0), R9C7 (5), R10C5 (1)    |

### 7.5 Reuse: round 1 `3 4 7`, round 2 `7 8 9`

`7 8 9` has 10 valid matches (blue in `tests/expected/347_789.jpg`).
1 only touches `H(1)`; 9 reuse a red 7 of round 1, and those reused cells
show blue (FR-4), e.g.:

| # | Cells               | Digits | Relation to `H(1)`                |
| - | ------------------- | ------ | --------------------------------- |
| 1 | R14C9, R15C9, R16C9 | 7 9 8  | touches R15C8 (4), R16C8 (3)      |
| 2 | R1C2, R2C1, R3C2    | 7 8 9  | overlaps R1C2 (7)                 |
| 3 | R2C5, R2C6, R2C7    | 9 7 8  | overlaps R2C6 (7)                 |

All cell references in this section are 1-based (`R<row>C<column>`).
