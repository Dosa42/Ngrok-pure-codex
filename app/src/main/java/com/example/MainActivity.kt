package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ngrokforwarder.ForwarderScreen
import com.example.ngrokforwarder.ForwarderViewModel
import com.example.ngrokforwarder.SamsungDexManager
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ForwarderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SamsungDexManager.initialize(applicationContext)
        setContent {
            MyApplicationTheme {
                ForwarderScreen(viewModel = viewModel)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        SamsungDexManager.updateState(this)
    }
}
