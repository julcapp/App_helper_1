package ru.apphelper.navigation.transit

interface TransitRouteProvider {
    val providerName: String

    suspend fun buildRoute(
        origin: GeoPoint,
        destination: GeoPoint,
    ): Result<TransitRoute>
}

class TransitProviderNotConfiguredException(provider: String) :
    IllegalStateException("Транспортный провайдер $provider не настроен: требуется API-ключ и реальная сетевой адаптер")
