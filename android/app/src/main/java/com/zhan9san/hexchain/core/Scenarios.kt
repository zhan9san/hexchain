package com.zhan9san.hexchain.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A test scenario from tests/scenarios.json (docs/android.md A-27), shared by the
 * Python tests, the JVM unit tests and the UI tests.
 */
data class Scenario(
    val name: String,
    val rounds: List<List<Int>>,
    /** Expected valid matches per round, cells written "R<row>C<col>:<digit>". */
    val matches: List<List<List<String>>>,
    /** Matches that exist in the grid but must not be valid, per round. */
    val excluded: List<List<List<String>>>,
) {
    companion object {
        fun parseAll(text: String): List<Scenario> =
            Json.parseToJsonElement(text).jsonObject.getValue("scenarios").jsonArray.map { element ->
                val o = element.jsonObject
                fun matches(key: String) = o.getValue(key).jsonArray.map { round ->
                    round.jsonArray.map { match -> match.jsonArray.map { it.jsonPrimitive.content } }
                }
                Scenario(
                    name = o.getValue("name").jsonPrimitive.content,
                    rounds = o.getValue("rounds").jsonArray.map { r -> r.jsonArray.map { it.jsonPrimitive.int } },
                    matches = matches("matches"),
                    excluded = matches("excluded"),
                )
            }

        /** "R1C2:7" -> Cell(0, 1), checking the digit against [honeycomb]. */
        fun parseCell(text: String, honeycomb: Honeycomb): Cell {
            val (pos, digit) = text.split(":")
            val (r, c) = pos.removePrefix("R").split("C").map { it.toInt() - 1 }
            val cell = Cell(r, c)
            check(honeycomb.digit(cell) == digit.toInt()) { "$text: grid has ${honeycomb.digit(cell)}" }
            return cell
        }

        fun parseMatch(match: List<String>, honeycomb: Honeycomb): Match =
            match.map { parseCell(it, honeycomb) }.sorted()
    }
}
