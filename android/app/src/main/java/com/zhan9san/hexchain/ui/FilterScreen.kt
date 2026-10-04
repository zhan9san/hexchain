package com.zhan9san.hexchain.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhan9san.hexchain.FilterViewModel
import com.zhan9san.hexchain.R
import com.zhan9san.hexchain.core.FilterMode
import com.zhan9san.hexchain.core.FilterResult
import kotlinx.coroutines.delay

/** The filter view (A-38 to A-43). Scrolls when the result is long. */
@Composable
fun FilterScreen(viewModel: FilterViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // A-38, A-43
        OutlinedTextField(
            value = viewModel.text,
            onValueChange = viewModel::updateText,
            placeholder = { Text(stringResource(R.string.filter_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            minLines = 3,
            maxLines = 6,
            trailingIcon = {
                if (viewModel.text.isNotEmpty()) {
                    val clear = stringResource(R.string.clear)
                    IconButton(
                        onClick = { viewModel.updateText("") },
                        modifier = Modifier.testTag("filter_clear").semantics { contentDescription = clear },
                    ) { Text("✕", fontSize = 18.sp) }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("filter_input"),
        )
        DigitChips(selected = viewModel.digit, onSelect = viewModel::selectDigit)
        ModeSwitch(selected = viewModel.mode, onSelect = viewModel::selectMode)
        viewModel.result?.let { Result(it) }
    }
}

/** A-39: single-choice digits 0–9. */
@Composable
private fun DigitChips(selected: Int?, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.filter_digit), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (d in 0..9) {
                val chosen = d == selected
                val colours = if (chosen) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                }
                FilledTonalButton(
                    onClick = { onSelect(d) },
                    colors = colours,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("digit_$d"),
                ) { Text(d.toString(), fontSize = 18.sp) }
            }
        }
    }
}

/** A-40: kill or keep. */
@Composable
private fun ModeSwitch(selected: FilterMode, onSelect: (FilterMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        val modes = listOf(FilterMode.KILL to R.string.mode_kill, FilterMode.KEEP to R.string.mode_keep)
        modes.forEachIndexed { i, (mode, label) ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = modes.size),
                modifier = Modifier.height(48.dp).testTag("mode_${mode.name.lowercase()}"),
            ) { Text(stringResource(label)) }
        }
    }
}

/** A-41, A-42: counts, kept numbers, duplicates, invalid tokens and copy. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Result(result: FilterResult) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.filter_count, result.kept.size, result.removed.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).testTag("filter_count"),
            )
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("numbers", result.kept.joinToString(" ")))
                    copied = true
                },
                enabled = result.kept.isNotEmpty(),
                modifier = Modifier.height(48.dp).testTag("copy"),
            ) { Text(stringResource(if (copied) R.string.copied else R.string.copy)) }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.testTag("kept"),
        ) {
            for (number in result.kept) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(number, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
        if (result.duplicateCount > 0) {
            Text(
                stringResource(
                    R.string.filter_duplicates,
                    result.duplicateCount,
                    result.duplicates.joinToString("，") { (number, times) -> "$number ×$times" },
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("filter_duplicates"),
            )
        }
        if (result.invalid.isNotEmpty()) {
            Text(
                stringResource(R.string.filter_invalid, result.invalid.joinToString(" ")),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("filter_invalid"),
            )
        }
    }
}
