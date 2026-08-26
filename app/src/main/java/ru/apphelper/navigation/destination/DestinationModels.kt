package ru.apphelper.navigation.destination

import ru.apphelper.navigation.transit.GeoPoint

data class DestinationCandidate(
    val id: String,
    val name: String,
    val address: String? = null,
    val category: String? = null,
    val landmarkHints: List<String> = emptyList(),
    val point: GeoPoint? = null,
)

data class ConfirmedDestination(
    val candidate: DestinationCandidate,
    val confirmedByUser: Boolean = true,
    val confirmedAtMillis: Long = System.currentTimeMillis(),
)

sealed interface DestinationResolution {
    data class Unique(val destination: DestinationCandidate) : DestinationResolution
    data class Ambiguous(val candidates: List<DestinationCandidate>) : DestinationResolution
    data object NotFound : DestinationResolution
}
