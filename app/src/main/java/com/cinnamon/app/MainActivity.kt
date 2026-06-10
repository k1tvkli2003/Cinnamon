package com.cinnamon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinnamon.app.ui.navigation.AppNavigation
import com.cinnamon.app.ui.theme.CinnamonTheme
import com.cinnamon.app.viewmodel.UserProgressViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val progressViewModel: UserProgressViewModel = viewModel()
            val selectedTheme by progressViewModel.selectedTheme.collectAsState()

            CinnamonTheme(themeName = selectedTheme) {
                AppNavigation(progressViewModel = progressViewModel)
            }
        }
    }
}
