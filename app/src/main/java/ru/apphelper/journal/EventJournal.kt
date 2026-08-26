package ru.apphelper.journal

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import ru.apphelper.calls.CallEvent
import ru.apphelper.notifications.NotificationEvent

enum class JournalEventType { NOTIFICATION, CALL }

data class JournalEvent(
    val id: String,
    val type: JournalEventType,
    val title: String,
    val body: String,
    val source: String,
    val timestampMillis: Long,
    val reviewed: Boolean = false,
)

class EventJournal(context: Context) {
    private val prefs = context.getSharedPreferences("event_journal", Context.MODE_PRIVATE)

    fun appendNotification(event: NotificationEvent) {
        append(
            JournalEvent(
                id = "notification:${event.id}:${event.receivedAt}",
                type = JournalEventType.NOTIFICATION,
                title = event.sender.ifBlank { event.appName },
                body = event.text,
                source = event.appName,
                timestampMillis = event.receivedAt,
            ),
        )
    }

    fun appendCall(event: CallEvent) {
        append(
            JournalEvent(
                id = "call:${event.phoneNumber}:${event.timestampMillis}",
                type = JournalEventType.CALL,
                title = event.displayName ?: event.phoneNumber.ifBlank { "Неизвестный номер" },
                body = if (event.incoming) "Входящий звонок" else "Исходящий звонок",
                source = "Телефон",
                timestampMillis = event.timestampMillis,
            ),
        )
    }

    fun unread(limit: Int = 20): List<JournalEvent> =
        loadAll().filterNot { it.reviewed }.sortedByDescending { it.timestampMillis }.take(limit)

    fun markReviewed(ids: Collection<String>) {
        if (ids.isEmpty()) return
        saveAll(loadAll().map { event -> if (event.id in ids) event.copy(reviewed = true) else event })
    }

    fun clear() = prefs.edit().remove(KEY_EVENTS).apply()

    private fun append(event: JournalEvent) {
        val current = loadAll().filterNot { it.id == event.id }.toMutableList()
        current += event
        saveAll(current.sortedByDescending { it.timestampMillis }.take(MAX_EVENTS))
    }

    private fun loadAll(): List<JournalEvent> {
        val raw = prefs.getString(KEY_EVENTS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        JournalEvent(
                            id = item.getString("id"),
                            type = JournalEventType.valueOf(item.getString("type")),
                            title = item.optString("title"),
                            body = item.optString("body"),
                            source = item.optString("source"),
                            timestampMillis = item.optLong("timestampMillis"),
                            reviewed = item.optBoolean("reviewed", false),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveAll(events: List<JournalEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject()
                    .put("id", event.id)
                    .put("type", event.type.name)
                    .put("title", event.title)
                    .put("body", event.body)
                    .put("source", event.source)
                    .put("timestampMillis", event.timestampMillis)
                    .put("reviewed", event.reviewed),
            )
        }
        prefs.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    private companion object {
        const val KEY_EVENTS = "events"
        const val MAX_EVENTS = 100
    }
}
