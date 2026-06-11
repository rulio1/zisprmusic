package com.example

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // CanvasBasedWindow automatically hooks into the #ComposeTarget element
    // defined in our index.html layout and renders the shared Jetpack Compose App content
    CanvasBasedWindow(title = "Zispr Player") {
        // App() representaria o composable de ponto de entrada comum contido em commonMain
        // App()
    }
}
