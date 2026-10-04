package com.zhan9san.hexchain.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhan9san.hexchain.MainViewModel
import com.zhan9san.hexchain.R
import com.zhan9san.hexchain.core.Cell
import com.zhan9san.hexchain.core.Honeycomb
import com.zhan9san.hexchain.core.RoundResult

/** The single screen (A-7): chart, round chips, digit slots + Clear, keypad. */
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val results = viewModel.results

    // A-12: a cell in several highlight sets shows the latest round's colour.
    val highlight = buildMap<Cell, Color> {
        results.forEachIndexed { n, result -> result.highlight.forEach { put(it, RoundColours[n]) } }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HoneycombChart(
                grid = viewModel.honeycomb.grid,
                highlight = highlight,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            RoundChips(results)
            InputRow(
                typed = viewModel.typed,
                nextColour = RoundColours[results.size.coerceAtMost(Honeycomb.MAX_ROUNDS - 1)],
                isFull = viewModel.isFull,
                canClear = results.isNotEmpty(),
                onClear = { confirmClear = true },
            )
            Keypad(enabled = !viewModel.isFull, onDigit = viewModel::type, onBackspace = viewModel::backspace)
        }
    }

    // A-21: clearing is confirmed and cannot be undone.
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clear()
                        confirmClear = false
                    },
                    modifier = Modifier.testTag("clear_ok"),
                ) { Text(stringResource(R.string.clear_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** A-19: one chip per round, up to 4 in a row. */
@Composable
private fun RoundChips(results: List<RoundResult>) {
    Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (n in 0 until Honeycomb.MAX_ROUNDS) {
            val result = results.getOrNull(n)
            if (result == null) {
                Spacer(Modifier.weight(1f))
                continue
            }
            val colour = RoundColours[n]
            val digits = result.digits.joinToString(" ")
            val outcome = if (result.matches.isEmpty()) {
                stringResource(R.string.no_match)
            } else {
                pluralStringResource(R.plurals.match_count, result.matches.size, result.matches.size)
            }
            val label = stringResource(R.string.round_label, n + 1)
            Column(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(colour.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(2.dp, colour, RoundedCornerShape(8.dp))
                    .testTag("chip_$n")
                    .clearAndSetSemantics { contentDescription = "$label: $digits, $outcome" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(digits, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text(outcome, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

/** A-15, A-18, A-20: digit slots (or the round-limit text) and the Clear button. */
@Composable
private fun InputRow(typed: List<Int>, nextColour: Color, isFull: Boolean, canClear: Boolean, onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isFull) {
                Text(
                    stringResource(R.string.round_limit),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("round_limit"),
                )
            } else {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .size(40.dp)
                            .border(2.dp, nextColour, RoundedCornerShape(8.dp))
                            .testTag("slot_$i"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(typed.getOrNull(i)?.toString().orEmpty(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onClear,
            enabled = canClear,
            modifier = Modifier.height(48.dp).testTag("clear"),
        ) { Text(stringResource(R.string.clear)) }
    }
}

/** A-14: 2 rows, 0–4 and 5–9, with backspace at the end of the second row. */
@Composable
private fun Keypad(enabled: Boolean, onDigit: (Int) -> Unit, onBackspace: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (d in 0..4) Key(d.toString(), enabled, "key_$d") { onDigit(d) }
            Spacer(Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (d in 5..9) Key(d.toString(), enabled, "key_$d") { onDigit(d) }
            val backspace = stringResource(R.string.backspace)
            Key("⌫", enabled, "backspace", Modifier.semantics { contentDescription = backspace }, onBackspace)
        }
    }
}

@Composable
private fun RowScope.Key(
    text: String,
    enabled: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.weight(1f).height(48.dp).testTag(tag),
    ) {
        Text(text, fontSize = 20.sp, textAlign = TextAlign.Center)
    }
}
