package com.zhan9san.hexchain

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zhan9san.hexchain.core.FilterMode
import com.zhan9san.hexchain.core.FilterScenario
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the filter view (A-46): type a list, pick a digit and switch
 * modes, using the shared filter scenarios (tests/scenarios.json).
 */
@RunWith(AndroidJUnit4::class)
class FilterScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val scenarios: List<FilterScenario> by lazy {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        FilterScenario.parseAll(assets.open("scenarios.json").bufferedReader().use { it.readText() })
    }

    private val filter get() = rule.activity.filterViewModel
    private fun string(id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    @Before
    fun openFilterView() {
        rule.runOnUiThread {
            filter.updateText("")
            filter.selectMode(FilterMode.KILL)
            rule.activity.viewModel.selectTab(1)
        }
        rule.waitForIdle()
    }

    private fun enter(text: String, digit: Int, mode: FilterMode) {
        rule.onNodeWithTag("filter_input").performTextClearance()
        if (text.isNotEmpty()) rule.onNodeWithTag("filter_input").performTextInput(text)
        rule.onNodeWithTag("digit_$digit").performClick()
        rule.onNodeWithTag("mode_${mode.name.lowercase()}").performClick()
        rule.waitForIdle()
    }

    @Test
    fun scenariosShowExpectedResult() {
        for (sc in scenarios) {
            enter(sc.input, sc.digit, sc.mode)
            assertEquals(sc.name, sc.expected, filter.result)
            val e = sc.expected
            rule.onNodeWithTag("filter_count").assertTextEquals(string(R.string.filter_count, e.kept.size, e.removed.size))
            if (e.duplicateCount > 0) {
                val list = e.duplicates.joinToString("，") { (n, t) -> "$n ×$t" }
                rule.onNodeWithTag("filter_duplicates")
                    .assertTextEquals(string(R.string.filter_duplicates, e.duplicateCount, list))
            } else {
                rule.onNodeWithTag("filter_duplicates").assertDoesNotExist()
            }
            if (e.invalid.isNotEmpty()) {
                rule.onNodeWithTag("filter_invalid")
                    .assertTextEquals(string(R.string.filter_invalid, e.invalid.joinToString(" ")))
            } else {
                rule.onNodeWithTag("filter_invalid").assertDoesNotExist()
            }
        }
    }

    @Test
    fun switchingModeSwapsKeptAndRemoved() {
        enter("347 468 986 707 123", 7, FilterMode.KILL)
        val kill = filter.result!!
        rule.onNodeWithTag("mode_keep").performClick()
        rule.waitForIdle()
        val keep = filter.result!!
        assertEquals(kill.kept to kill.removed, keep.removed to keep.kept)
    }

    @Test
    fun clearButtonEmptiesTheListOnly() {
        rule.runOnUiThread { rule.activity.viewModel.type(3); rule.activity.viewModel.type(4); rule.activity.viewModel.type(7) }
        enter("347 468", 7, FilterMode.KILL)
        rule.onNodeWithTag("copy").assertIsEnabled()
        rule.onNodeWithTag("filter_clear").performClick()
        rule.waitForIdle()
        assertEquals("", filter.text)
        rule.onNodeWithTag("copy").assertIsNotEnabled()
        // F-8: the rounds are untouched.
        assertEquals(listOf(listOf(3, 4, 7)), rule.activity.viewModel.rounds)
        rule.runOnUiThread { rule.activity.viewModel.clear() }
    }
}
