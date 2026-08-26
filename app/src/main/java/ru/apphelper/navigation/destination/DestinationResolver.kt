package ru.apphelper.navigation.destination

import ru.apphelper.navigation.transit.CityContext

interface DestinationSearchProvider {
    suspend fun search(
        cityContext: CityContext,
        spokenQuery: String,
    ): Result<List<DestinationCandidate>>
}

class DestinationResolver(
    private val provider: DestinationSearchProvider,
) {
    suspend fun resolve(
        cityContext: CityContext,
        spokenQuery: String,
    ): Result<DestinationResolution> = provider.search(cityContext, spokenQuery).map { candidates ->
        when {
            candidates.isEmpty() -> DestinationResolution.NotFound
            candidates.size == 1 -> DestinationResolution.Unique(candidates.first())
            else -> DestinationResolution.Ambiguous(candidates)
        }
    }

    fun clarificationPrompt(
        spokenQuery: String,
        candidates: List<DestinationCandidate>,
    ): String {
        if (candidates.isEmpty()) {
            return "Я не нашёл подходящее место. Назовите его точнее или скажите адрес или ориентир."
        }

        val examples = candidates.take(3).joinToString("; ") { candidate ->
            buildString {
                append(candidate.name)
                candidate.address?.takeIf { it.isNotBlank() }?.let { append(", $it") }
                candidate.landmarkHints.firstOrNull()?.takeIf { it.isNotBlank() }?.let { append(", рядом $it") }
            }
        }
        return "По запросу $spokenQuery найдено несколько мест. Уточните, какое вы имеете в виду: $examples."
    }

    fun confirmationPrompt(candidate: DestinationCandidate): String = buildString {
        append("Подтвердите место назначения: ${candidate.name}")
        candidate.address?.takeIf { it.isNotBlank() }?.let { append(", $it") }
        candidate.landmarkHints.firstOrNull()?.takeIf { it.isNotBlank() }?.let { append(", ориентир: $it") }
        append(". Это нужное место?")
    }
}
