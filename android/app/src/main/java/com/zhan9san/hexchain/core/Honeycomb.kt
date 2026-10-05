package com.zhan9san.hexchain.core

import kotlinx.serialization.json.Json

/** One hexagon of the chart, 0-based (docs/requirements.md section 3). */
data class Cell(val row: Int, val col: Int) : Comparable<Cell> {
    override fun compareTo(other: Cell): Int =
        compareValuesBy(this, other, Cell::row, Cell::col)
}

/** A valid match: 3 cells, sorted. */
typealias Match = List<Cell>

/** Result of one round (O-2). */
data class RoundResult(val digits: List<Int>, val matches: List<Match>) {
    /** Highlight set H(n). */
    val highlight: Set<Cell> = matches.flatten().toSet()
}

/** Core rules of docs/requirements.md; mirrors honeycomb.py. */
class Honeycomb(val grid: List<List<Int>>) {

    val cells: List<Cell> = grid.indices.flatMap { r -> grid[r].indices.map { c -> Cell(r, c) } }

    fun digit(cell: Cell): Int = grid[cell.row][cell.col]

    /** Every cell sharing a side with [cell] (section 3.1). */
    fun neighbours(cell: Cell): List<Cell> {
        val (r, c) = cell
        val upDown = if (r % 2 == 0) listOf(c - 1, c) else listOf(c, c + 1)
        val candidates = listOf(Cell(r, c - 1), Cell(r, c + 1)) +
            listOf(-1, 1).flatMap { dr -> upDown.map { col -> Cell(r + dr, col) } }
        return candidates.filter { it.row in grid.indices && it.col in grid[it.row].indices }
    }

    /** True if any of [match] shares a side with any cell in [previous]. */
    fun touches(match: Match, previous: Set<Cell>): Boolean =
        match.any { cell -> neighbours(cell).any { it in previous } }

    /**
     * Every connected group of 3 cells whose digits equal [digits] in any order
     * (R-3, R-4). With [previous], keep only groups that touch or overlap it (R-6, R-7).
     */
    fun findMatches(digits: List<Int>, previous: Set<Cell>? = null): List<Match> {
        val target = digits.sorted()
        val found = mutableSetOf<Match>()
        for (centre in cells) {
            // A connected triple always has a cell adjacent to the other two.
            val near = neighbours(centre)
            for (i in near.indices) for (j in i + 1 until near.size) {
                val match = listOf(near[i], centre, near[j]).sorted()
                if (match.map(::digit).sorted() == target) found += match
            }
        }
        val valid = if (previous == null) found else found.filter { match ->
            match.any { it in previous } || touches(match, previous)
        }
        return valid.sortedWith(MATCH_ORDER)
    }

    /** Apply rounds in order; round n is checked against round n-1 only (R-5 to R-9). */
    fun highlightRounds(rounds: List<List<Int>>): List<RoundResult> {
        require(rounds.size <= MAX_ROUNDS) { "at most $MAX_ROUNDS rounds, got ${rounds.size}" }
        var previous: Set<Cell>? = null
        return rounds.map { digits ->
            require(digits.size == 3 && digits.all { it in 0..9 }) { "a round is 3 digits: $digits" }
            RoundResult(digits, findMatches(digits, previous)).also { previous = it.highlight }
        }
    }

    companion object {
        const val MAX_ROUNDS = 4

        /** R-11: replace the digits of round [n] (0-based); results are recomputed (O-5). */
        fun editRound(rounds: List<List<Int>>, n: Int, digits: List<Int>): List<List<Int>> {
            require(n in rounds.indices) { "no round ${n + 1}" }
            require(digits.size == 3 && digits.all { it in 0..9 }) { "a round is 3 digits: $digits" }
            return rounds.toMutableList().also { it[n] = digits }
        }

        /** R-12: remove round [n] (0-based); later rounds move up one place. */
        fun deleteRound(rounds: List<List<Int>>, n: Int): List<List<Int>> {
            require(n in rounds.indices) { "no round ${n + 1}" }
            return rounds.filterIndexed { i, _ -> i != n }
        }

        private val MATCH_ORDER = Comparator<Match> { a, b ->
            a.zip(b).map { (x, y) -> x.compareTo(y) }.firstOrNull { it != 0 } ?: 0
        }

        /** Load from data/grid.json (an array of rows). */
        fun fromJson(text: String): Honeycomb = Honeycomb(Json.decodeFromString<List<List<Int>>>(text))
    }
}
