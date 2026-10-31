package ru.apphelper.calls

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.content.ContextCompat
import ru.apphelper.contacts.ContactLookup
import ru.apphelper.journal.EventJournal

class AppCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        // Before Android 10 this callback is used for incoming calls only.
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else true

        val displayName = if (
            number.isNotBlank() &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        ) {
            ContactLookup(this).findDisplayName(number)
        } else {
            null
        }

        val event = CallEvent(
            phoneNumber = number,
            displayName = displayName,
            incoming = isIncoming,
        )

        EventJournal(this).appendCall(event)
        CallEventStore.publish(event)

        if (isIncoming && number.isNotBlank() && displayName == null) {
            UnknownCallerStore(this).save(number)
        }

        if (isIncoming) {
            respondToCall(
                callDetails,
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build(),
            )
        }
    }
}
