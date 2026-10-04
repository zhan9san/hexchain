"""Core logic (docs/requirements.md section 8): kill or keep numbers containing a digit."""

import re
from dataclasses import dataclass, field

# Space, tab, line breaks, full-width space, comma, full-width comma, enumeration comma.
SEPARATORS = re.compile("[ \t\r\n　,，、]+")
VALID = re.compile("[0-9]{3}")

KILL = "kill"
KEEP = "keep"


@dataclass
class FilterResult:
    """O-4: kept and removed numbers, duplicates and invalid tokens."""

    kept: list = field(default_factory=list)
    removed: list = field(default_factory=list)
    # (number, times entered) for every number entered more than once.
    duplicates: list = field(default_factory=list)
    # Extra copies beyond the first (F-3).
    duplicate_count: int = 0
    invalid: list = field(default_factory=list)


def filter_numbers(text, digit, mode):
    """Kill (F-4) or keep (F-5) the 3-digit numbers in `text` that contain `digit`."""
    if mode not in (KILL, KEEP):
        raise ValueError(f"mode must be {KILL!r} or {KEEP!r}, got {mode!r}")
    if not (isinstance(digit, int) and 0 <= digit <= 9):
        raise ValueError(f"digit must be 0-9, got {digit!r}")

    counts = {}  # number -> times entered, in order of first appearance (F-3, F-6)
    result = FilterResult()
    for token in SEPARATORS.split(text):
        if not token:
            continue
        if VALID.fullmatch(token):
            counts[token] = counts.get(token, 0) + 1
        else:
            result.invalid.append(token)  # F-2

    for number, times in counts.items():
        contains = str(digit) in number
        kept = not contains if mode == KILL else contains
        (result.kept if kept else result.removed).append(number)
        if times > 1:
            result.duplicates.append((number, times))
            result.duplicate_count += times - 1
    return result
