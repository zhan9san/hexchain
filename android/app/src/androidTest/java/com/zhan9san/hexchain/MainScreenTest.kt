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

    private fun scenario(name: String) = scenarios.first { it.name == name }

    private fun assertHighlights(name: String) {
        val expected = scenario(name)
        assertEquals(name, expected.rounds, viewModel.rounds)
        expected.matches.forEachIndexed { n, matches ->
            val cells = matches.flatMap { Scenario.parseMatch(it, viewModel.honeycomb) }.toSet()
            assertEquals("$name round ${n + 1}", cells, viewModel.results[n].highlight)
        }
    }

    private fun openChipMenu(n: Int, item: String) {
        rule.onNodeWithTag("chip_$n").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(item).performClick()
        rule.waitForIdle()
    }

    @Test
    fun deleteMiddleRoundMovesLaterRoundsUp() {
        scenario("three_rounds").rounds.forEach(::typeRound)
        openChipMenu(1, "delete_round")
        assertHighlights("delete_round_2")
        rule.onNodeWithTag("chip_2").assertDoesNotExist()
    }

    @Test
    fun editMiddleRoundRecomputesLaterRounds() {
        scenario("three_rounds").rounds.forEach(::typeRound)
        openChipMenu(1, "edit_round")
        rule.onNodeWithTag("editing_round").assertExists()
        rule.onNodeWithTag("cancel_edit").assertExists()
        rule.onNodeWithTag("clear").assertDoesNotExist()
        typeRound(listOf(7, 8, 9))
        assertHighlights("edit_round_2")
        assertEquals(null, viewModel.editing)
        rule.onNodeWithTag("clear").assertExists()
    }

    @Test
    fun editWorksWithFourRounds() {
        repeat(4) { typeRound(listOf(3, 4, 7)) }
        rule.onNodeWithTag("key_1").assertIsNotEnabled()
        openChipMenu(3, "edit_round")
        rule.onNodeWithTag("key_1").assertIsEnabled()
        typeRound(listOf(1, 2, 3))
        assertEquals(listOf(listOf(3, 4, 7), listOf(3, 4, 7), listOf(3, 4, 7), listOf(1, 2, 3)), viewModel.rounds)
        rule.onNodeWithTag("round_limit").assertExists()
    }

    @Test
    fun cancelEditKeepsTheRound() {
        typeRound(listOf(3, 4, 7))
        typeRound(listOf(5))
        openChipMenu(0, "edit_round")
        assertEquals(emptyList<Int>(), viewModel.typed)
        typeRound(listOf(1))
        rule.onNodeWithTag("cancel_edit").performClick()
        rule.waitForIdle()
        assertEquals(listOf(listOf(3, 4, 7)), viewModel.rounds)
        assertEquals(null, viewModel.editing)
        assertEquals(emptyList<Int>(), viewModel.typed)
    }
}
