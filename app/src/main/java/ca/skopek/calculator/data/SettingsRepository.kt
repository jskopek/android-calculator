package ca.skopek.calculator.data

import android.content.Context
import ca.skopek.calculator.ui.theme.ThemeMode

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() {
            val stored = prefs.getString(KEY_THEME_MODE, null)
            return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
