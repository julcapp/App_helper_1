package ru.apphelper.location

import android.content.Context
import android.content.Intent
import android.net.Uri

object NavigationHelper {
    fun openRoute(context: Context, destination: SafeLocation): Boolean {
        val mapsUrl = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${destination.latitude},${destination.longitude}&dir_action=navigate",
        )
        val intent = Intent(Intent.ACTION_VIEW, mapsUrl).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            false
        }
    }
}
