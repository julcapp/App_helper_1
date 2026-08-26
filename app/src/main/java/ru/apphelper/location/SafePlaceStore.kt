package ru.apphelper.location

import android.content.Context

data class SafeLocation(
    val title: String,
    val latitude: Double,
    val longitude: Double,
)

class SafePlaceStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_helper_safe_places", Context.MODE_PRIVATE)

    fun loadHome(): SafeLocation? {
        if (!prefs.contains("home_latitude") || !prefs.contains("home_longitude")) return null
        return SafeLocation(
            title = prefs.getString("home_title", "Дом") ?: "Дом",
            latitude = java.lang.Double.longBitsToDouble(prefs.getLong("home_latitude", 0L)),
            longitude = java.lang.Double.longBitsToDouble(prefs.getLong("home_longitude", 0L)),
        )
    }

    fun saveHome(location: DeviceLocation) {
        prefs.edit()
            .putString("home_title", "Дом")
            .putLong("home_latitude", java.lang.Double.doubleToRawLongBits(location.latitude))
            .putLong("home_longitude", java.lang.Double.doubleToRawLongBits(location.longitude))
            .apply()
    }
}
