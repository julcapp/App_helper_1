package ru.apphelper.calls

import android.Manifest
import android.content.pm.PackageManager
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.content.ContextCompat
import ru.apphelper.contacts.ContactLookup

class AppCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        val isIncoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING

        val displayName = if (
            number.isNotBlank() &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        ) {
            ContactLookup(this).findDisplayName(number)
        } else {
            null
        }

        CallEventStore.publish(
            CallEvent(
                phoneNumber = number,
                displayName = displayName,
                incoming = isIncoming,
            ),
        )

        if (isIncoming) {
            respondToCall(
                callDetails,
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSilenceCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build(),
            )
        }
    }
}
