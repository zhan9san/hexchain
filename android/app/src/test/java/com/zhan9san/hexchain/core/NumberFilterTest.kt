package com.zhan9san.hexchain.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Runs the shared filter scenarios (tests/scenarios.json) and the filter rules (A-45). */
class NumberFilterTest {
    private val root = File(System.getProperty("repoRoot") ?: "../..")
    private val scenarios = FilterScenario.parseAll(root.resolve("tests/scenarios.json").readText())

    @Test
    fun scenariosAreLoaded() {
        assertEquals(4, scenarios.size)
    }

    @Test
    fun scenarioResults() {
        for (sc in scenarios) {
            assertEquals(sc.name, sc.expected, NumberFilter.filter(sc.input, sc.digit, sc.mode))
        }
    }

    @Test
    fun separators() {
        val r = NumberFilter.filter("111 222\t333\n444　555,666，777、888 ,， 999", 0, FilterMode.KILL)
        assertEquals(listOf("111", "222", "333", "444", "555", "666", "777", "888", "999"), r.kept)
        assertEquals(emptyList<String>(), r.invalid)
    }

    @Test
    fun invalidTokens() {
        val r = NumberFilter.filter("12 1234 1a3 ３４７ 007", 7, FilterMode.KEEP)
        assertEquals(listOf("12", "1234", "1a3", "３４７"), r.invalid)
        assertEquals(listOf("007"), r.kept)
    }

    @Test
    fun duplicatesCountExtraCopies() {
        val r = NumberFilter.filter("347 347 347 468 468", 7, FilterMode.KILL)
        assertEquals(3, r.duplicateCount)
        assertEquals(listOf("347" to 3, "468" to 2), r.duplicates)
        assertEquals(listOf("468") to listOf("347"), r.kept to r.removed)
    }

    @Test
    fun killAndKeepSwap() {
        val text = "347 468 986 707 123 347"
        val kill = NumberFilter.filter(text, 7, FilterMode.KILL)
        val keep = NumberFilter.filter(text, 7, FilterMode.KEEP)
        assertEquals(kill.kept to kill.removed, keep.removed to keep.kept)
        assertEquals(5, kill.kept.size + kill.removed.size)
    }

    @Test
    fun badDigit() {
        assertThrows(IllegalArgumentException::class.java) { NumberFilter.filter("347", 10, FilterMode.KILL) }
    }
}
