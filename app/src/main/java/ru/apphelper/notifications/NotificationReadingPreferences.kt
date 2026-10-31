package ru.apphelper.notifications

import android.content.Context

/** Local, per-device source choices; no message text is persisted here. */
class NotificationReadingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("notification_reading", Context.MODE_PRIVATE)
    fun sources(): Map<String, String> = prefs.all.filterKeys { it.startsWith("source:") }
        .mapKeys { it.key.removePrefix("source:") }.mapValues { it.value.toString() }.toSortedMap()
    fun recordSource(packageName: String, label: String) {
        prefs.edit().putString("source:$packageName", label).apply()
    }
    fun isEnabled(packageName: String): Boolean = packageName !in disabled()
    fun setEnabled(packageName: String, enabled: Boolean) {
        val values = disabled().toMutableSet()
        if (enabled) values.remove(packageName) else values.add(packageName)
        prefs.edit().putStringSet("disabled_packages", values).apply()
    }
    private fun disabled(): Set<String> = prefs.getStringSet("disabled_packages", emptySet()).orEmpty()
}
