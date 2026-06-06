package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.UserProgressViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val progressViewModel: UserProgressViewModel = viewModel()
      val selectedTheme by progressViewModel.selectedTheme.collectAsState()

      MyApplicationTheme(themeName = selectedTheme) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          AppNavigation(progressViewModel = progressViewModel)
        }
      }
    }
  }
}
