package ru.apphelper.navigation.transit

import ru.apphelper.data.CityHistoryStore

sealed interface CityContextAction {
    data object AskInitialCity : CityContextAction
    data class ConfirmLocationChange(
        val previousCity: String,
        val detectedCity: String,
    ) : CityContextAction
    data class RevisitKnownCity(
        val cityName: String,
        val previousVisitCount: Int,
    ) : CityContextAction
    data object NoAction : CityContextAction
}

class CityContextManager(
    private val historyStore: CityHistoryStore,
) {
    fun initialAction(): CityContextAction =
        if (historyStore.currentCity().isNullOrBlank()) CityContextAction.AskInitialCity
        else CityContextAction.NoAction

    fun onDetectedCity(candidateCity: String): CityContextAction {
        val detected = candidateCity.trim()
        if (detected.isBlank()) return CityContextAction.NoAction

        val current = historyStore.currentCity()
        if (current.isNullOrBlank()) return CityContextAction.AskInitialCity
        if (current.equals(detected, ignoreCase = true)) return CityContextAction.NoAction

        val previous = historyStore.history().firstOrNull {
            it.cityName.equals(detected, ignoreCase = true)
        }
        return if (previous != null) {
            CityContextAction.RevisitKnownCity(
                cityName = detected,
                previousVisitCount = previous.visitCount,
            )
        } else {
            CityContextAction.ConfirmLocationChange(
                previousCity = current,
                detectedCity = detected,
            )
        }
    }

    fun confirmCity(
        cityName: String,
        regionName: String? = null,
        countryCode: String? = null,
    ) {
        historyStore.confirmCity(cityName, regionName, countryCode)
    }

    fun voicePrompt(action: CityContextAction): String? = when (action) {
        CityContextAction.AskInitialCity -> "Уточните город, в котором вы сейчас находитесь."
        is CityContextAction.ConfirmLocationChange ->
            "Похоже, вы сейчас в городе ${action.detectedCity}. Подтвердить новый город?"
        is CityContextAction.RevisitKnownCity ->
            "Похоже, вы снова в городе ${action.cityName}. Использовать этот город для маршрутов и транспорта?"
        CityContextAction.NoAction -> null
    }
}
