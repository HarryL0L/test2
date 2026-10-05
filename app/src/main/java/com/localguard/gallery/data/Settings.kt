package com.localguard.gallery.data

import android.content.Context

enum class Sensitivity(val threshold: Float, val label: String) {
    Strict(0.25f, "Strict"),
    Balanced(0.5f, "Balanced"),
    Relaxed(0.75f, "Relaxed"),
}

data class GuardSettings(
    val sensitivity: Sensitivity = Sensitivity.Balanced,
    val blurSuggestive: Boolean = true,
    val blurUntilScanned: Boolean = true,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("guard_settings", Context.MODE_PRIVATE)

    fun load() = GuardSettings(
        sensitivity = prefs.getString("sensitivity", null)
            ?.let { name -> Sensitivity.entries.firstOrNull { it.name == name } }
            ?: Sensitivity.Balanced,
        blurSuggestive = prefs.getBoolean("blur_suggestive", true),
        blurUntilScanned = prefs.getBoolean("blur_until_scanned", true),
    )

    fun save(s: GuardSettings) {
        prefs.edit()
            .putString("sensitivity", s.sensitivity.name)
            .putBoolean("blur_suggestive", s.blurSuggestive)
            .putBoolean("blur_until_scanned", s.blurUntilScanned)
            .apply()
    }
}
