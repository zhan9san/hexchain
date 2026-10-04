package com.zhan9san.hexchain.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs the shared scenarios (tests/scenarios.json) and the core rules (A-28). */
class HoneycombTest {
    private val root = File(System.getProperty("repoRoot") ?: "../..")
    private val honeycomb = Honeycomb.fromJson(root.resolve("data/grid.json").readText())
    private val scenarios = Scenario.parseAll(root.resolve("tests/scenarios.json").readText())

    @Test
    fun gridHas184Cells() {
        assertEquals(16, honeycomb.grid.size)
        assertEquals(184, honeycomb.cells.size)
    }

    @Test
    fun scenariosAreLoaded() {
        assertEquals(listOf("three_rounds", "reuse"), scenarios.map { it.name })
    }

    @Test
    fun scenarioMatches() {
        for (scenario in scenarios) {
            val results = honeycomb.highlightRounds(scenario.rounds)
            scenario.matches.forEachIndexed { n, expected ->
                val label = "${scenario.name} round ${n + 1} ${scenario.rounds[n]}"
                val want = expected.map { Scenario.parseMatch(it, honeycomb) }.sortedWith(matchOrder)
                assertEquals(label, want, results[n].matches)
                assertEquals(label, want.flatten().toSet(), results[n].highlight)
            }
        }
    }

    @Test
    fun scenarioExcluded() {
        for (scenario in scenarios) {
            val results = honeycomb.highlightRounds(scenario.rounds)
            scenario.excluded.forEachIndexed { n, excluded ->
                for (match in excluded.map { Scenario.parseMatch(it, honeycomb) }) {
                    val label = "${scenario.name} round ${n + 1}: $match"
                    assertTrue(label, match in honeycomb.findMatches(scenario.rounds[n]))
                    assertFalse(label, match in results[n].matches)
                }
            }
        }
    }

    @Test
    fun orderDoesNotMatter() {
        assertEquals(honeycomb.findMatches(listOf(3, 4, 7)), honeycomb.findMatches(listOf(7, 4, 3)))
    }

    @Test
    fun fourRoundsAllowed() {
        assertEquals(4, honeycomb.highlightRounds(List(4) { listOf(3, 4, 7) }).size)
    }

    @Test
    fun fifthRoundRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            honeycomb.highlightRounds(List(5) { listOf(3, 4, 7) })
        }
    }

    @Test
    fun emptyRoundEmptiesLaterRounds() {
        // No 0 0 0 combination exists, so round 2 has nothing to touch.
        val results = honeycomb.highlightRounds(listOf(listOf(0, 0, 0), listOf(3, 4, 7)))
        assertEquals(listOf(emptySet<Cell>(), emptySet()), results.map { it.highlight })
    }

    private val matchOrder = Comparator<Match> { a, b ->
        a.zip(b).map { (x, y) -> x.compareTo(y) }.firstOrNull { it != 0 } ?: 0
    }
}
