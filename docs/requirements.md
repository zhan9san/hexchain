# HexChain — Core Requirements

## 1. Overview

A fixed honeycomb chart of hexagons holds one digit (0–9) per hexagon. The
user enters up to 4 rounds of 3 digits. For each round, every group of 3
connected cells holding those digits is found; from round 2 on, the search
is limited to the area around the previous round's result.

Separately, a **number filter** (section 8) takes a list of 3-digit numbers
and kills (removes) or keeps the numbers that contain a chosen digit.

This document defines the core behaviour only and is platform independent.
Platform documents add presentation and technology details on top of it:

- Android: `docs/android.md`

A platform document may add details but must not change or contradict a
core rule; if a platform needs different behaviour, this document changes
first. `honeycomb.py` and `number_filter.py`, with the tests in `tests/`,
are the reference implementation of this document.

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

Transcribed from `images/honeycomb.jpg` and verified against it.

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

- The single fixed chart in section 3.2.
- The number filter (section 8), independent of the chart and its rounds.

Out of scope:

- Loading or editing other charts.
- Undoing a single round, editing an earlier round, or undoing a clear
  (see R-10).

## 5. Rules

### 5.1 Input

1. **R-1 Round size**: every round has exactly 3 digits (each 0–9).
   Repeated digits are allowed (e.g. `1 1 5`, `7 7 7`).
2. **R-2 Round count**: at most 4 rounds can be entered, one after another.
   A 5th round is not accepted; the user must clear first.

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
   `3 4 7`, round `7 8 9` may start from a 7 cell of `H(1)` (see 7.5).
4. **R-8 Previous round only**: round `n` is checked against round `n-1`
   only. Rounds `n-2` and earlier are ignored.
5. **R-9 Empty chain**: if round `n-1` has no valid match, `H(n-1)` is empty
   and round `n` (and every later round) has no valid match either. The
   round is still accepted, with an empty result.

### 5.4 Clear

1. **R-10 Clear all**: clearing removes every round and its result; the
   next input becomes round 1 again. Clearing is the only way to change
   entered rounds and cannot be undone.

## 6. Outputs

1. **O-1 Search**: when a round is entered, its valid matches are computed
   (R-3 to R-9). Each match is counted once regardless of the order its
   cells were discovered.
2. **O-2 Result per round**: for every entered round, in order, the result
   is
   - the round's 3 digits,
   - the list of valid matches (each a set of 3 cells),
   - the highlight set `H(n)`, and
   - the number of valid matches (0 means no match).
3. **O-3 Shared cells**: a cell may belong to several highlight sets
   (R-7, R-8); each `H(n)` is kept separately.

## 7. Examples

### 7.1 Single Combination

Input `4 7 8`: cells R1C1 (4), R1C2 (7), R2C1 (8) are mutually adjacent
(triangle), so all three are highlighted.

### 7.2 Round 1: `3 4 7`

5 matches, 13 cells:

| # | Cells               | Digits | Shape    |
| - | ------------------- | ------ | -------- |
| 1 | R1C1, R1C2, R1C3    | 4 7 3  | chain    |
| 2 | R2C6, R3C6, R3C7    | 7 3 4  | triangle |
| 3 | R3C6, R3C7, R4C7    | 3 4 7  | chain    |
| 4 | R8C8, R8C9, R9C8    | 4 3 7  | chain    |
| 5 | R15C7, R15C8, R16C8 | 7 4 3  | chain    |

### 7.3 Round 2: `0 5 1` around round 1

`0 5 1` has 12 matches in the whole grid; 4 of them touch `H(1)`:

| # | Cells             | Digits | Touches round-1 cell(s)      |
| - | ----------------- | ------ | ---------------------------- |
| 1 | R1C7, R1C8, R2C8  | 5 0 1  | R2C6 (7)                     |
| 2 | R2C2, R3C3, R4C2  | 1 5 0  | R1C2 (7), R1C3 (3)           |
| 3 | R3C5, R4C5, R4C6  | 0 5 1  | R3C6 (3), R3C7 (4), R4C7 (7) |
| 4 | R9C6, R9C7, R10C5 | 0 5 1  | R9C8 (7)                     |

### 7.4 Round 3: `2 9 3` around round 2

`2 9 3` has 14 matches in the whole grid; 4 of them touch `H(2)`. Round 1 is
ignored (R-8):

| # | Cells               | Digits | Touches round-2 cell(s)          |
| - | ------------------- | ------ | -------------------------------- |
| 1 | R5C2, R5C3, R6C3    | 2 3 9  | R4C2 (0)                         |
| 2 | R10C6, R10C7, R11C6 | 9 2 3  | R9C6 (0), R9C7 (5), R10C5 (1)    |
| 3 | R10C6, R10C7, R11C8 | 9 2 3  | R9C6 (0), R9C7 (5), R10C5 (1)    |
| 4 | R10C6, R11C6, R12C5 | 9 3 2  | R9C6 (0), R9C7 (5), R10C5 (1)    |

### 7.5 Reuse: round 1 `3 4 7`, round 2 `7 8 9`

`7 8 9` has 10 valid matches. 1 only touches `H(1)`; 9 reuse a 7 cell of
`H(1)` (R-7), e.g.:

| # | Cells               | Digits | Relation to `H(1)`                |
| - | ------------------- | ------ | --------------------------------- |
| 1 | R14C9, R15C9, R16C9 | 7 9 8  | touches R15C8 (4), R16C8 (3)      |
| 2 | R1C2, R2C1, R3C2    | 7 8 9  | overlaps R1C2 (7)                 |
| 3 | R2C5, R2C6, R2C7    | 9 7 8  | overlaps R2C6 (7)                 |

All cell references in this section are 1-based (`R<row>C<column>`). The
full expected results are the scenarios in `tests/test_honeycomb.py`.

## 8. Number Filter

The number filter works on a list of numbers typed by the user. It does not
use the chart and does not change the rounds.

### 8.1 Definitions

- **Number list**: free text holding numbers and separators.
- **Separator**: a space (including the full-width space `　`), a tab, a
  line break, a comma `,`, a full-width comma `，` or an enumeration comma
  `、`. Separators can be mixed and repeated.
- **Token**: a run of characters between separators.
- **Valid number**: a token of exactly 3 digits `0`–`9`, `000` to `999`.
  Leading zeros are kept (`051` stays `051`). Full-width digits such as
  `３４７` are not valid.
- **Filter digit**: one digit, `0` to `9`, chosen by the user.
- **Mode**: **kill** or **keep** (F-4, F-5).

### 8.2 Rules

1. **F-1 Parsing**: the number list is split into tokens at separators.
2. **F-2 Invalid tokens**: any token that is not a valid number (e.g. `34`,
   `3478`, `3a7`) is invalid. Invalid tokens are reported back in their
   input order and are otherwise ignored.
3. **F-3 Duplicates**: a valid number entered more than once counts once,
   at its first position. Every extra copy is a duplicate: `347 347 347`
   has 2 duplicates.
4. **F-4 Kill digit d**: every number containing `d` is removed; the rest
   are kept.
5. **F-5 Keep digit d**: every number not containing `d` is removed; the
   numbers containing `d` are kept.
6. **F-6 Order**: kept and removed numbers stay in input order (by first
   position, F-3).
7. **F-7 Counts**: kept + removed = the number of distinct valid numbers.
   For the same list and digit, kill and keep swap the kept and removed
   numbers.
8. **F-8 Independence**: the filter neither reads nor changes the rounds
   (section 5), and clearing the rounds (R-10) does not clear the filter.

### 8.3 Outputs

1. **O-4 Filter result**: for a number list, filter digit and mode, the
   result is
   - the kept numbers and their count,
   - the removed numbers and their count,
   - the number of duplicates, and each duplicated number with how often
     it was entered (e.g. `347 ×2`), and
   - the invalid tokens.

### 8.4 Example

Number list `347, 468 986，707、123 347 34a`, filter digit `7`.

Distinct valid numbers: `347 468 986 707 123` (5). Duplicates: 1
(`347 ×2`). Invalid: `34a`.

| Mode   | Kept          | Removed       |
| ------ | ------------- | ------------- |
| Kill 7 | `468 986 123` | `347 707`     |
| Keep 7 | `347 707`     | `468 986 123` |

Counts: kill 7 keeps 3 and removes 2; keep 7 keeps 2 and removes 3.
