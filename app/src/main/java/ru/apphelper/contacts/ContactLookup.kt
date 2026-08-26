package ru.apphelper.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

class ContactLookup(private val context: Context) {
    fun findDisplayName(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber),
        )

        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (index >= 0) {
                    return cursor.getString(index)?.takeIf { it.isNotBlank() }
                }
            }
        }

        return null
    }
}
