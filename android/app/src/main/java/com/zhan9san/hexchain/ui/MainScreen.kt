package com.zhan9san.hexchain.ui

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.remember
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

/** The chart view (A-7): chart, round chips, digit slots + Clear, keypad. */
@Composable
fun ChartScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val results = viewModel.results

    // A-12: a cell in several highlight sets shows the latest round's colour.
    val highlight = buildMap<Cell, Color> {
        results.forEachIndexed { n, result -> result.highlight.forEach { put(it, RoundColours[n]) } }
    }

    Column(
        modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HoneycombChart(
            grid = viewModel.honeycomb.grid,
            highlight = highlight,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        val editing = viewModel.editing
        RoundChips(results, editing, onEdit = viewModel::startEdit, onDelete = viewModel::deleteRound)
        InputRow(
            typed = viewModel.typed,
            slotColour = RoundColours[editing ?: results.size.coerceAtMost(Honeycomb.MAX_ROUNDS - 1)],
            editing = editing,
            isFull = viewModel.isFull,
            canClear = results.isNotEmpty(),
            onClear = { confirmClear = true },
            onCancelEdit = viewModel::cancelEdit,
        )
        Keypad(enabled = viewModel.canType, onDigit = viewModel::type, onBackspace = viewModel::backspace)
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

/** A-19, A-49: one chip per round, up to 4 in a row; tapping a chip opens its menu. */
@Composable
private fun RoundChips(results: List<RoundResult>, editing: Int?, onEdit: (Int) -> Unit, onDelete: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (n in 0 until Honeycomb.MAX_ROUNDS) {
            val result = results.getOrNull(n)
            if (result == null) {
                Spacer(Modifier.weight(1f))
                continue
            }
            var menuOpen by remember { mutableStateOf(false) }
            val colour = RoundColours[n]
            val digits = result.digits.joinToString(" ")
            val outcome = if (result.matches.isEmpty()) {
                stringResource(R.string.no_match)
            } else {
                pluralStringResource(R.plurals.match_count, result.matches.size, result.matches.size)
            }
            val label = stringResource(R.string.round_label, n + 1)
            Box(Modifier.weight(1f)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(colour.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        // A-50: the chip being edited gets a thicker outline.
                        .border(if (editing == n) 4.dp else 2.dp, colour, RoundedCornerShape(8.dp))
                        .clickable { menuOpen = true }
                        .testTag("chip_$n")
                        // A-54: a button whose label says what it is.
                        .clearAndSetSemantics {
                            contentDescription = "$label: $digits, $outcome"
                            role = Role.Button
                            onClick {
                                menuOpen = true
                                true
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(digits, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                    Text(outcome, fontSize = 11.sp, maxLines = 1)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_round, n + 1)) },
                        onClick = {
                            menuOpen = false
                            onEdit(n)
                        },
                        modifier = Modifier.testTag("edit_round"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_round, n + 1)) },
                        onClick = {
                            menuOpen = false
                            onDelete(n)
                        },
                        modifier = Modifier.testTag("delete_round"),
                    )
                }
            }
        }
    }
}

/**
 * A-15, A-18, A-20, A-50: digit slots (or the round-limit text) and the Clear
 * button; while editing, a label, the edited round's slots and Cancel.
 */
@Composable
private fun InputRow(
    typed: List<Int>,
    slotColour: Color,
    editing: Int?,
    isFull: Boolean,
    canClear: Boolean,
    onClear: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editing != null) {
                Text(
                    stringResource(R.string.editing_round, editing + 1),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("editing_round"),
                )
            }
            if (isFull && editing == null) {
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
                            .border(2.dp, slotColour, RoundedCornerShape(8.dp))
                            .testTag("slot_$i"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(typed.getOrNull(i)?.toString().orEmpty(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (editing != null) {
            OutlinedButton(
                onClick = onCancelEdit,
                modifier = Modifier.height(48.dp).testTag("cancel_edit"),
            ) { Text(stringResource(R.string.cancel)) }
        } else {
            OutlinedButton(
                onClick = onClear,
                enabled = canClear,
                modifier = Modifier.height(48.dp).testTag("clear"),
            ) { Text(stringResource(R.string.clear)) }
        }
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
