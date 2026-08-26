package ru.apphelper.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
)

class LocationAssistant(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun getCurrentLocation(
        onSuccess: (DeviceLocation) -> Unit,
        onError: (String) -> Unit,
    ) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            onError("Нет разрешения на геолокацию")
            return
        }

        val priority = if (fine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val token = CancellationTokenSource()

        client.getCurrentLocation(priority, token.token)
            .addOnSuccessListener { location ->
                if (location == null) {
                    onError("Не удалось определить текущее местоположение")
                } else {
                    onSuccess(
                        DeviceLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                        ),
                    )
                }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Ошибка определения местоположения")
            }
    }
}
