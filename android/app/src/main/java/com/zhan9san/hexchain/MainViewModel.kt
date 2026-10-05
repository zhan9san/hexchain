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

    /** A-50: the round being edited (0-based), or null; not persisted (A-53). */
    var editing: Int? by mutableStateOf(null)
        private set

    val results: List<RoundResult> by derivedStateOf { honeycomb.highlightRounds(rounds) }

    /** R-2: no more input after the last round. */
    val isFull: Boolean get() = rounds.size >= Honeycomb.MAX_ROUNDS

    /** A-50, R-13: the keypad works for a new round, or for the round being edited. */
    val canType: Boolean get() = editing != null || !isFull

    /** A-16, A-51: the 3rd digit submits a new round, or replaces the edited one. */
    fun type(digit: Int) {
        if (!canType) return
        val next = typed + digit
        if (next.size < 3) {
            typed = next
            return
        }
        val n = editing
        rounds = if (n != null) Honeycomb.editRound(rounds, n, next) else rounds + listOf(next)
        editing = null
        typed = emptyList()
        save()
    }

    /** A-50: digits typed for a new round are discarded. */
    fun startEdit(n: Int) {
        if (n !in rounds.indices) return
        editing = n
        typed = emptyList()
    }

    /** A-51 */
    fun cancelEdit() {
        editing = null
        typed = emptyList()
    }

    /** A-52, R-12: later rounds move up; editing follows its round or ends. */
    fun deleteRound(n: Int) {
        if (n !in rounds.indices) return
        rounds = Honeycomb.deleteRound(rounds, n)
        editing = editing?.let { e ->
            when {
                e == n -> null
                e > n -> e - 1
                else -> e
            }
        }
        if (editing == null) typed = emptyList()
        save()
    }

    /** A-17: only the round being typed can be changed. */
    fun backspace() {
        typed = typed.dropLast(1)
    }

    /** R-10 */
    fun clear() {
        rounds = emptyList()
        editing = null
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
