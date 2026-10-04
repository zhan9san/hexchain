package com.zhan9san.hexchain

import android.app.Application
import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.zhan9san.hexchain.core.Honeycomb
import com.zhan9san.hexchain.core.RoundResult

/**
 * Screen state: submitted rounds, the round being typed and their results.
 * Submitted rounds are saved so they survive process death and restarts (A-22).
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    val honeycomb: Honeycomb =
        Honeycomb.fromJson(app.assets.open("grid.json").bufferedReader().use { it.readText() })

    private val prefs = app.getSharedPreferences("rounds", Context.MODE_PRIVATE)
    private val appPrefs = app.getSharedPreferences("app", Context.MODE_PRIVATE)

    /** A-37: the selected view (0 = chart, 1 = filter); the app opens on the one used last. */
    var tab: Int by mutableIntStateOf(appPrefs.getInt(KEY_TAB, 0).coerceIn(0, 1))
        private set

    fun selectTab(index: Int) {
        tab = index
        appPrefs.edit().putInt(KEY_TAB, index).apply()
    }

    /** Submitted rounds, oldest first. */
    var rounds: List<List<Int>> by mutableStateOf(load())
        private set

    /** Digits typed so far for the next round (not persisted). */
    var typed: List<Int> by mutableStateOf(emptyList())
        private set

    val results: List<RoundResult> by derivedStateOf { honeycomb.highlightRounds(rounds) }

    /** R-2: no more input after the last round. */
    val isFull: Boolean get() = rounds.size >= Honeycomb.MAX_ROUNDS

    /** A-16: the 3rd digit submits the round. */
    fun type(digit: Int) {
        if (isFull) return
        val next = typed + digit
        if (next.size == 3) {
            rounds = rounds + listOf(next)
            typed = emptyList()
            save()
        } else {
            typed = next
        }
    }

    /** A-17: only the round being typed can be changed. */
    fun backspace() {
        typed = typed.dropLast(1)
    }

    /** R-10 */
    fun clear() {
        rounds = emptyList()
        typed = emptyList()
        save()
    }

    private fun save() {
        prefs.edit().putString(KEY, rounds.joinToString(",") { it.joinToString("") }).apply()
    }

    private fun load(): List<List<Int>> =
        prefs.getString(KEY, "").orEmpty()
            .split(",")
            .filter { it.length == 3 && it.all(Char::isDigit) }
            .map { round -> round.map { it.digitToInt() } }
            .take(Honeycomb.MAX_ROUNDS)

    private companion object {
        const val KEY = "rounds"
        const val KEY_TAB = "tab"
    }
}
