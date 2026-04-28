package com.securedoc.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.securedoc.ai.presentation.scanner.ScannerScreen
import com.example.securedocai.ui.theme.SecureDocAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureDocAITheme {
                ScannerScreen()
            }
        }
    }
}
