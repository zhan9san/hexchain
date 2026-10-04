package com.zhan9san.hexchain.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.zhan9san.hexchain.FilterViewModel
import com.zhan9san.hexchain.MainViewModel
import com.zhan9san.hexchain.R

/** A-37: a tab row switches between the chart view and the filter view. */
@Composable
fun AppScreen(main: MainViewModel, filter: FilterViewModel) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding()) {
            PrimaryTabRow(selectedTabIndex = main.tab) {
                listOf(R.string.tab_chart, R.string.tab_filter).forEachIndexed { i, label ->
                    Tab(
                        selected = main.tab == i,
                        onClick = { main.selectTab(i) },
                        text = { Text(stringResource(label)) },
                        modifier = Modifier.testTag("tab_$i"),
                    )
                }
            }
            val content = Modifier.weight(1f).fillMaxWidth()
            when (main.tab) {
                0 -> ChartScreen(main, content)
                else -> FilterScreen(filter, content)
            }
        }
    }
}
