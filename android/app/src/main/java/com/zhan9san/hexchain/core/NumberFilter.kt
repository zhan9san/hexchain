package com.zhan9san.hexchain.core

/** Kill (F-4) or keep (F-5) the numbers containing the filter digit. */
enum class FilterMode { KILL, KEEP }

/** O-4: kept and removed numbers, duplicates and invalid tokens. */
data class FilterResult(
    val kept: List<String>,
    val removed: List<String>,
    /** (number, times entered) for every number entered more than once. */
    val duplicates: List<Pair<String, Int>>,
    /** Extra copies beyond the first (F-3). */
    val duplicateCount: Int,
    val invalid: List<String>,
)

/** Core section 8 (docs/requirements.md); mirrors number_filter.py. */
object NumberFilter {
    // Space, tab, line breaks, full-width space, comma, full-width comma, enumeration comma.
    private val SEPARATORS = Regex("[ \t\r\n　,，、]+")
    private val VALID = Regex("[0-9]{3}")

    fun filter(text: String, digit: Int, mode: FilterMode): FilterResult {
        require(digit in 0..9) { "digit must be 0-9, got $digit" }
        val counts = linkedMapOf<String, Int>() // first-appearance order (F-3, F-6)
        val invalid = mutableListOf<String>()
        for (token in text.split(SEPARATORS)) {
            when {
                token.isEmpty() -> Unit
                VALID.matches(token) -> counts[token] = (counts[token] ?: 0) + 1
                else -> invalid += token // F-2
            }
        }
        val kept = mutableListOf<String>()
        val removed = mutableListOf<String>()
        for (number in counts.keys) {
            val contains = digit.digitToChar() in number
            if (if (mode == FilterMode.KILL) !contains else contains) kept += number else removed += number
        }
        val duplicates = counts.filterValues { it > 1 }.map { (number, times) -> number to times }
        return FilterResult(kept, removed, duplicates, duplicates.sumOf { it.second - 1 }, invalid)
    }
}
