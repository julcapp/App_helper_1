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

enum class ScheduleSeason {
    SUMMER,
    WINTER,
    ALL_YEAR,
    PROVIDER_DEFINED,
    UNKNOWN,
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

data class ServiceCalendarInfo(
    /** Человеко-читаемое описание от провайдера: например «летнее расписание» или «действует до 30 сентября». */
    val label: String? = null,
    /** Категория сезона используется только если провайдер явно её сообщает. */
    val season: ScheduleSeason = ScheduleSeason.UNKNOWN,
    /** Дата начала действия расписания в формате ISO-8601, если передана провайдером. */
    val validFrom: String? = null,
    /** Дата окончания действия расписания в формате ISO-8601, если передана провайдером. */
    val validUntil: String? = null,
    /** Признак, что конкретная дата поездки проверена по календарю/исключениям провайдера. */
    val serviceDateVerified: Boolean = false,
)

data class TransitLeg(
    val mode: TransitMode,
    val from: TransitStop? = null,
    val to: TransitStop? = null,
    /** Номер маршрута, например 24, 7, М3. Только из данных транспортного провайдера. */
    val routeNumber: String? = null,
    /** Название линии/маршрута, если провайдер его передаёт отдельно от номера. */
    val routeName: String? = null,
    /** Направление движения, например «в сторону Москвы», «в сторону Тулы», «до Чехова». */
    val headsign: String? = null,
    /** Опорный вокзал/станция отправления, если это полезно для ориентации пользователя. */
    val originHubName: String? = null,
    /** Конечная станция конкретного рейса, если провайдер её сообщает. */
    val tripTerminalName: String? = null,
    /** Информация о календаре действия расписания конкретного рейса/сервиса. */
    val serviceCalendar: ServiceCalendarInfo? = null,
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
