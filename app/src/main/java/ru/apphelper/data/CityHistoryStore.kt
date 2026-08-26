package ru.apphelper.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CityVisit(
    val cityName: String,
    val regionName: String? = null,
    val countryCode: String? = null,
    val firstSeenAtMillis: Long,
    val lastSeenAtMillis: Long,
    val visitCount: Int = 1,
)

class CityHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_helper_city_history", Context.MODE_PRIVATE)

    fun currentCity(): String? = prefs.getString(KEY_CURRENT_CITY, null)

    fun history(): List<CityVisit> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        CityVisit(
                            cityName = item.getString("cityName"),
                            regionName = item.optString("regionName").takeIf { it.isNotBlank() },
                            countryCode = item.optString("countryCode").takeIf { it.isNotBlank() },
                            firstSeenAtMillis = item.getLong("firstSeenAtMillis"),
                            lastSeenAtMillis = item.getLong("lastSeenAtMillis"),
                            visitCount = item.optInt("visitCount", 1),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun confirmCity(
        cityName: String,
        regionName: String? = null,
        countryCode: String? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val normalized = cityName.trim()
        if (normalized.isBlank()) return

        val visits = history().toMutableList()
        val existingIndex = visits.indexOfFirst {
            it.cityName.equals(normalized, ignoreCase = true) &&
                (countryCode == null || it.countryCode == null || it.countryCode.equals(countryCode, ignoreCase = true))
        }

        if (existingIndex >= 0) {
            val existing = visits[existingIndex]
            visits[existingIndex] = existing.copy(
                cityName = normalized,
                regionName = regionName ?: existing.regionName,
                countryCode = countryCode ?: existing.countryCode,
                lastSeenAtMillis = nowMillis,
                visitCount = existing.visitCount + 1,
            )
        } else {
            visits += CityVisit(
                cityName = normalized,
                regionName = regionName,
                countryCode = countryCode,
                firstSeenAtMillis = nowMillis,
                lastSeenAtMillis = nowMillis,
            )
        }

        saveHistory(visits.sortedByDescending { it.lastSeenAtMillis }.take(MAX_HISTORY))
        prefs.edit().putString(KEY_CURRENT_CITY, normalized).apply()
    }

    private fun saveHistory(visits: List<CityVisit>) {
        val array = JSONArray()
        visits.forEach { visit ->
            array.put(
                JSONObject()
                    .put("cityName", visit.cityName)
                    .put("regionName", visit.regionName.orEmpty())
                    .put("countryCode", visit.countryCode.orEmpty())
                    .put("firstSeenAtMillis", visit.firstSeenAtMillis)
                    .put("lastSeenAtMillis", visit.lastSeenAtMillis)
                    .put("visitCount", visit.visitCount),
            )
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    private companion object {
        const val KEY_CURRENT_CITY = "current_city"
        const val KEY_HISTORY = "history"
        const val MAX_HISTORY = 50
    }
}
