package ru.apphelper.navigation.transit

object TransitVoiceGuide {
    fun describeLeg(leg: TransitLeg): String = when (leg.mode) {
        TransitMode.WALK -> buildString {
            append("Идите пешком")
            leg.distanceMeters?.let { append(" примерно $it метров") }
            leg.to?.name?.takeIf { it.isNotBlank() }?.let { append(" до $it") }
            append(".")
        }

        TransitMode.SUBURBAN_TRAIN,
        TransitMode.TRAIN,
        TransitMode.AEROEXPRESS -> railLeg(leg)

        TransitMode.BUS,
        TransitMode.TROLLEYBUS,
        TransitMode.TRAM,
        TransitMode.METRO,
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

    fun stopsRemainingAlert(leg: TransitLeg, stopsRemaining: Int): String? {
        val destination = leg.to?.name?.takeIf { it.isNotBlank() } ?: "нужной остановке"
        return when {
            stopsRemaining == 2 -> "Через две остановки нужно выходить на $destination."
            stopsRemaining == 1 -> "Следующая остановка — ваша: $destination. Приготовьтесь выходить."
            stopsRemaining == 0 -> "Выходите на остановке $destination."
            else -> null
        }
    }

    private fun vehicleLeg(leg: TransitLeg): String = buildString {
        append(modeName(leg.mode))
        leg.routeNumber?.takeIf { it.isNotBlank() }?.let { append(" номер $it") }
            ?: leg.routeName?.takeIf { it.isNotBlank() }?.let { append(" $it") }
        leg.headsign?.takeIf { it.isNotBlank() }?.let { append(" в направлении $it") }
        leg.from?.name?.takeIf { it.isNotBlank() }?.let { append(". Дождитесь транспорта и садитесь на остановке $it") }
        leg.from?.platform?.takeIf { it.isNotBlank() }?.let { append(", платформа $it") }
        leg.stopCount?.takeIf { it > 0 }?.let { append(". Нужно проехать $it остановок") }
        leg.to?.name?.takeIf { it.isNotBlank() }?.let { append(" и выйти на $it") }
        leg.to?.platform?.takeIf { it.isNotBlank() }?.let { append(", прибытие к платформе $it") }
        append(".")
    }

    private fun railLeg(leg: TransitLeg): String = buildString {
        append(modeName(leg.mode))
        leg.routeNumber?.takeIf { it.isNotBlank() }?.let { append(" номер $it") }
        leg.tripTerminalName?.takeIf { it.isNotBlank() }?.let { append(" до конечной $it") }
        leg.headsign?.takeIf { it.isNotBlank() }?.let { append(". Направление: $it") }
        leg.originHubName?.takeIf { it.isNotBlank() }?.let { append(". Отправление через $it") }
        leg.from?.name?.takeIf { it.isNotBlank() }?.let { append(". Садитесь на станции $it") }
        leg.from?.platform?.takeIf { it.isNotBlank() }?.let { append(", платформа $it") }
        leg.departureTimeText?.takeIf { it.isNotBlank() }?.let { append(". Отправление $it") }
        leg.stopCount?.takeIf { it > 0 }?.let { append(". Нужно проехать $it остановок") }
        leg.to?.name?.takeIf { it.isNotBlank() }?.let { append(" и выйти на станции $it") }
        leg.to?.platform?.takeIf { it.isNotBlank() }?.let { append(", прибытие к платформе $it") }
        leg.serviceCalendar?.let { calendar ->
            calendar.label?.takeIf { it.isNotBlank() }?.let { append(". По данным перевозчика действует $it") }
            if (!calendar.serviceDateVerified) {
                append(". Дата поездки не подтверждена календарём перевозчика, расписание нужно перепроверить")
            }
        }
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
