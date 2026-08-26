package ru.apphelper.calls

import android.content.Context

/**
 * Хранит последний неизвестный входящий номер между запусками приложения.
 * Это не означает, что разговор состоялся: CallScreeningService не сообщает
 * надёжное событие завершения разговора. Номер предлагается сохранить при
 * следующем активном взаимодействии пользователя с помощником.
 */
class UnknownCallerStore(context: Context) {
    private val prefs = context.getSharedPreferences("unknown_caller", Context.MODE_PRIVATE)

    fun save(number: String) {
        if (number.isBlank()) return
        prefs.edit()
            .putString(KEY_NUMBER, number)
            .putLong(KEY_TIME, System.currentTimeMillis())
            .apply()
    }

    fun load(): PendingUnknownCaller? {
        val number = prefs.getString(KEY_NUMBER, null)?.takeIf { it.isNotBlank() } ?: return null
        return PendingUnknownCaller(
            phoneNumber = number,
            detectedAt = prefs.getLong(KEY_TIME, 0L),
        )
    }

    fun clear() {
        prefs.edit().remove(KEY_NUMBER).remove(KEY_TIME).apply()
    }

    companion object {
        private const val KEY_NUMBER = "number"
        private const val KEY_TIME = "time"
    }
}

data class PendingUnknownCaller(
    val phoneNumber: String,
    val detectedAt: Long,
)
