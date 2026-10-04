package com.zhan9san.hexchain

import android.app.Application
import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.zhan9san.hexchain.core.FilterMode
import com.zhan9san.hexchain.core.FilterResult
import com.zhan9san.hexchain.core.NumberFilter

/** Filter view state (A-38 to A-44): number list, digit and mode, saved like the rounds. */
class FilterViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("filter", Context.MODE_PRIVATE)

    var text: String by mutableStateOf(prefs.getString(KEY_TEXT, "").orEmpty())
        private set

    /** A-39: no digit at first; no result until one is chosen. */
    var digit: Int? by mutableStateOf(prefs.getInt(KEY_DIGIT, -1).takeIf { it in 0..9 })
        private set

    /** A-40: kill at first. */
    var mode: FilterMode by mutableStateOf(
        FilterMode.entries.firstOrNull { it.name == prefs.getString(KEY_MODE, null) } ?: FilterMode.KILL,
    )
        private set

    /** A-41: recomputed on every change. */
    val result: FilterResult? by derivedStateOf { digit?.let { NumberFilter.filter(text, it, mode) } }

    fun updateText(value: String) {
        text = value
        prefs.edit().putString(KEY_TEXT, value).apply()
    }

    fun selectDigit(value: Int) {
        digit = value
        prefs.edit().putInt(KEY_DIGIT, value).apply()
    }

    fun selectMode(value: FilterMode) {
        mode = value
        prefs.edit().putString(KEY_MODE, value.name).apply()
    }

    private companion object {
        const val KEY_TEXT = "text"
        const val KEY_DIGIT = "digit"
        const val KEY_MODE = "mode"
    }
}
