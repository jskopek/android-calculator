package ca.skopek.calculator

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.skopek.calculator.ui.CalculatorApp
import ca.skopek.calculator.ui.theme.CalculatorTheme
import ca.skopek.calculator.ui.theme.isDark

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CalculatorViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = uiState.themeMode.isDark()

            // Keep status/navigation bar icons readable when the theme is forced away from the system default.
            LaunchedEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            CalculatorTheme(darkTheme = darkTheme) {
                CalculatorApp(
                    uiState = uiState,
                    windowSizeClass = calculateWindowSizeClass(this),
                    decimalSeparator = viewModel.decimalSeparator,
                    onKey = viewModel::onKey,
                    onHistorySelect = viewModel::useHistoryEntry,
                    onHistoryDelete = viewModel::deleteHistoryEntry,
                    onClearHistory = viewModel::clearHistory,
                    onThemeChange = viewModel::setThemeMode,
                    onConverterOpenChange = viewModel::setConverterOpen,
                )
            }
        }
    }
}
