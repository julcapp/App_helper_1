package ru.apphelper.navigation.transit

object TransitVoiceGuide {
    fun describeLeg(leg: TransitLeg): String = when (leg.mode) {
        TransitMode.WALK -> buildString {
            append("Идите пешком")
            leg.distanceMeters?.let { append(" примерно $it метров") }
            leg.to?.name?.takeIf { it.isNotBlank() }?.let { append(" до $it") }
            append(".")
        }

        TransitMode.BUS,
        TransitMode.TROLLEYBUS,
        TransitMode.TRAM,
        TransitMode.METRO,
        TransitMode.SUBURBAN_TRAIN,
        TransitMode.TRAIN,
        TransitMode.AEROEXPRESS,
        TransitMode.OTHER -> vehicleLeg(leg)

        TransitMode.TRANSFER -> buildString {
            append("Нужна пересадка")
            leg.to?.name?.takeIf { it.isNotBlank() }?.let { append(" к $it") }
            append(".")
            leg.instructions.firstOrNull()?.takeIf { it.isNotBlank() }?.let { append(" $it") }
        }
    }

    fun upcomingExit(leg: TransitLeg): String? {
        val stop = leg.to?.name?.takeIf { it.isNotBlank() } ?: return null
        return when (leg.mode) {
            TransitMode.BUS,
            TransitMode.TROLLEYBUS,
            TransitMode.TRAM,
            TransitMode.METRO,
            TransitMode.SUBURBAN_TRAIN,
            TransitMode.TRAIN,
            TransitMode.AEROEXPRESS -> "Следующая важная точка — $stop. Приготовьтесь выходить."
            else -> null
        }
    }

    private fun vehicleLeg(leg: TransitLeg): String = buildString {
        append(modeName(leg.mode))
        leg.routeName?.takeIf { it.isNotBlank() }?.let { append(" $it") }
        leg.headsign?.takeIf { it.isNotBlank() }?.let { append(" в направлении $it") }
        leg.from?.name?.takeIf { it.isNotBlank() }?.let { append(". Посадка: $it") }
        leg.from?.platform?.takeIf { it.isNotBlank() }?.let { append(", платформа $it") }
        leg.stopCount?.takeIf { it > 0 }?.let { append(". Проедьте $it остановок") }
        leg.to?.name?.takeIf { it.isNotBlank() }?.let { append(" до $it") }
        leg.to?.platform?.takeIf { it.isNotBlank() }?.let { append(", прибытие к платформе $it") }
        append(".")
    }

    private fun modeName(mode: TransitMode): String = when (mode) {
        TransitMode.BUS -> "Автобус"
        TransitMode.TROLLEYBUS -> "Троллейбус"
        TransitMode.TRAM -> "Трамвай"
        TransitMode.METRO -> "Метро"
        TransitMode.SUBURBAN_TRAIN -> "Электричка"
        TransitMode.TRAIN -> "Поезд"
        TransitMode.AEROEXPRESS -> "Аэроэкспресс"
        TransitMode.OTHER -> "Транспорт"
        TransitMode.WALK -> "Пешком"
        TransitMode.TRANSFER -> "Пересадка"
    }
}
