package ru.apphelper.navigation.transit

enum class TransitMode {
    WALK,
    BUS,
    TROLLEYBUS,
    TRAM,
    METRO,
    SUBURBAN_TRAIN,
    TRAIN,
    AEROEXPRESS,
    TRANSFER,
    OTHER,
}

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

data class TransitStop(
    val name: String,
    val point: GeoPoint? = null,
    val platform: String? = null,
)

data class TransitLeg(
    val mode: TransitMode,
    val from: TransitStop? = null,
    val to: TransitStop? = null,
    /** Номер маршрута, например 24, 7, М3. Только из данных транспортного провайдера. */
    val routeNumber: String? = null,
    /** Название линии/маршрута, если провайдер его передаёт отдельно от номера. */
    val routeName: String? = null,
    val headsign: String? = null,
    val departureTimeText: String? = null,
    val arrivalTimeText: String? = null,
    val stopCount: Int? = null,
    val distanceMeters: Int? = null,
    val durationSeconds: Int? = null,
    val instructions: List<String> = emptyList(),
)

data class TransitRoute(
    val provider: String,
    val origin: GeoPoint,
    val destination: GeoPoint,
    val legs: List<TransitLeg>,
    val totalDurationSeconds: Int? = null,
    val generatedAtMillis: Long = System.currentTimeMillis(),
)
