package com.zhan9san.hexchain

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zhan9san.hexchain.core.Scenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests (A-29): drive the keypad and check which cells are highlighted in
 * which round, using the shared scenarios (tests/scenarios.json).
 */
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val scenarios: List<Scenario> by lazy {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        Scenario.parseAll(assets.open("scenarios.json").bufferedReader().use { it.readText() })
    }

    private val viewModel get() = rule.activity.viewModel

    @Before
    fun clearSavedRounds() {
        rule.activity.getSharedPreferences("rounds", Context.MODE_PRIVATE).edit().clear().commit()
        rule.runOnUiThread {
            viewModel.clear()
            viewModel.selectTab(0)
        }
        rule.waitForIdle()
    }

    private fun typeRound(digits: List<Int>) {
        digits.forEach { rule.onNodeWithTag("key_$it").performClick() }
        rule.waitForIdle()
    }

    @Test
    fun scenariosHighlightExpectedCells() {
        for (scenario in scenarios) {
            rule.runOnUiThread { viewModel.clear() }
            scenario.rounds.forEach(::typeRound)
            val results = viewModel.results
            scenario.matches.forEachIndexed { n, expected ->
                val cells = expected.flatMap { Scenario.parseMatch(it, viewModel.honeycomb) }.toSet()
                assertEquals("${scenario.name} round ${n + 1}", cells, results[n].highlight)
                rule.onNodeWithTag("chip_$n").assertExists()
            }
        }
    }

    @Test
    fun backspaceOnlyChangesTypedDigits() {
        typeRound(listOf(3, 4))
        rule.onNodeWithTag("backspace").performClick()
        rule.waitForIdle()
        typeRound(listOf(4, 7))
        assertEquals(listOf(listOf(3, 4, 7)), viewModel.rounds)
        rule.onNodeWithTag("backspace").performClick()
        rule.waitForIdle()
        assertEquals(listOf(listOf(3, 4, 7)), viewModel.rounds)
    }

    @Test
    fun keypadDisabledAfterFourRounds() {
        repeat(4) { typeRound(listOf(3, 4, 7)) }
        rule.onNodeWithTag("round_limit").assertExists()
        rule.onNodeWithTag("key_3").assertIsNotEnabled()
        assertEquals(4, viewModel.rounds.size)
    }

    @Test
    fun clearAfterConfirmation() {
        typeRound(listOf(3, 4, 7))
        rule.onNodeWithTag("clear").assertIsEnabled().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("clear_ok").performClick()
        rule.waitForIdle()
        assertTrue(viewModel.rounds.isEmpty())
        rule.onNodeWithTag("chip_0").assertDoesNotExist()
        rule.onNodeWithTag("key_3").assertIsEnabled()
    }
}
