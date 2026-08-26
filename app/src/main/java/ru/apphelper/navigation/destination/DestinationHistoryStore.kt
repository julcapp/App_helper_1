package ru.apphelper.navigation.destination

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DestinationHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("destination_history", Context.MODE_PRIVATE)

    fun remember(destination: ConfirmedDestination) {
        val current = load().toMutableList()
        current.removeAll { it.candidate.id == destination.candidate.id }
        current.add(0, destination)
        save(current.take(MAX_ITEMS))
    }

    fun load(): List<ConfirmedDestination> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val hintsArray = item.optJSONArray("landmarkHints") ?: JSONArray()
                    val hints = buildList {
                        for (i in 0 until hintsArray.length()) add(hintsArray.getString(i))
                    }
                    add(
                        ConfirmedDestination(
                            candidate = DestinationCandidate(
                                id = item.getString("id"),
                                name = item.getString("name"),
                                address = item.optString("address").takeIf { it.isNotBlank() },
                                category = item.optString("category").takeIf { it.isNotBlank() },
                                landmarkHints = hints,
                                point = null,
                            ),
                            confirmedByUser = true,
                            confirmedAtMillis = item.optLong("confirmedAtMillis"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun save(items: List<ConfirmedDestination>) {
        val array = JSONArray()
        items.forEach { destination ->
            array.put(
                JSONObject().apply {
                    put("id", destination.candidate.id)
                    put("name", destination.candidate.name)
                    put("address", destination.candidate.address.orEmpty())
                    put("category", destination.candidate.category.orEmpty())
                    put("landmarkHints", JSONArray(destination.candidate.landmarkHints))
                    put("confirmedAtMillis", destination.confirmedAtMillis)
                },
            )
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    companion object {
        private const val KEY_ITEMS = "items"
        private const val MAX_ITEMS = 50
    }
}
