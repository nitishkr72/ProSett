package com.prosett

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prosett.ui.AppListScreen
import com.prosett.ui.AppViewModel
import com.prosett.ui.theme.MyApplicationTheme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.prosett.ui.ThemeMode

import com.prosett.ui.ProSettApp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val appViewModel: AppViewModel = viewModel()
      val uiState by appViewModel.uiState.collectAsState()
      
      val isDarkTheme = when (uiState.themeMode) {
          ThemeMode.LIGHT -> false
          ThemeMode.DARK_NEUTRAL, ThemeMode.DARK_TINTED -> true
          ThemeMode.SYSTEM -> isSystemInDarkTheme()
      }

      MyApplicationTheme(
          darkTheme = isDarkTheme,
          themeMode = uiState.themeMode
      ) {
        Surface(modifier = Modifier.fillMaxSize()) {
          ProSettApp(viewModel = appViewModel)
        }
      }
    }
  }
}

