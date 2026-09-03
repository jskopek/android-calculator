package ca.skopek.calculator.data

import android.content.Context
import ca.skopek.calculator.ui.theme.ThemeMode

data class ConverterSelection(val categoryId: String, val fromUnitId: String, val toUnitId: String)

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

    var converterSelection: ConverterSelection?
        get() {
            val category = prefs.getString(KEY_CONVERTER_CATEGORY, null) ?: return null
            val from = prefs.getString(KEY_CONVERTER_FROM, null) ?: return null
            val to = prefs.getString(KEY_CONVERTER_TO, null) ?: return null
            return ConverterSelection(category, from, to)
        }
        set(value) {
            prefs.edit()
                .putString(KEY_CONVERTER_CATEGORY, value?.categoryId)
                .putString(KEY_CONVERTER_FROM, value?.fromUnitId)
                .putString(KEY_CONVERTER_TO, value?.toUnitId)
                .apply()
        }

    /** Whether the converter was open when the app was last used. */
    var converterOpen: Boolean
        get() = prefs.getBoolean(KEY_CONVERTER_OPEN, false)
        set(value) = prefs.edit().putBoolean(KEY_CONVERTER_OPEN, value).apply()

    /** Raw text last typed into the converter. */
    var converterInput: String?
        get() = prefs.getString(KEY_CONVERTER_INPUT, null)
        set(value) = prefs.edit().putString(KEY_CONVERTER_INPUT, value).apply()

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_CONVERTER_OPEN = "converter_open"
        const val KEY_CONVERTER_INPUT = "converter_input"
        const val KEY_CONVERTER_CATEGORY = "converter_category"
        const val KEY_CONVERTER_FROM = "converter_from"
        const val KEY_CONVERTER_TO = "converter_to"
    }
}
