package com.zhan9san.hexchain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.zhan9san.hexchain.ui.AppScreen
import com.zhan9san.hexchain.ui.HexChainTheme

class MainActivity : ComponentActivity() {

    val viewModel: MainViewModel by viewModels()
    val filterViewModel: FilterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HexChainTheme {
                AppScreen(viewModel, filterViewModel)
            }
        }
    }
}
