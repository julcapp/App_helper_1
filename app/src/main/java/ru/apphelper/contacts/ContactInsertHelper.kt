package ru.apphelper.contacts

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract

object ContactInsertHelper {
    fun buildInsertIntent(phoneNumber: String, displayName: String): Intent {
        return Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)
            if (displayName.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.NAME, displayName.trim())
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun launch(context: Context, phoneNumber: String, displayName: String) {
        val intent = buildInsertIntent(phoneNumber, displayName)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }
}
