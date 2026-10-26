package com.rr.numio.phone.data

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts

/**
 * Saves contacts into Android's own contacts database (the same one every contacts app
 * uses). New contacts go into the phone's local storage, not an online account.
 * Call these from a background thread; each returns false if Android refused.
 */
class ContactWriter(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun save(draft: ContactDraft): Boolean = try {
        if (draft.id == null) insert(draft) else update(draft.id, draft)
        true
    } catch (e: Exception) {
        false
    }

    fun delete(contactId: Long): Boolean = try {
        resolver.delete(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId), null, null) > 0
    } catch (e: Exception) {
        false
    }

    private fun insert(draft: ContactDraft) {
        val ops = ArrayList<ContentProviderOperation>()
        ops += ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
            .withValue(RawContacts.ACCOUNT_TYPE, null)
            .withValue(RawContacts.ACCOUNT_NAME, null)
            .withValue(RawContacts.STARRED, if (draft.starred) 1 else 0)
            .build()
        ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
            .withValueBackReference(Data.RAW_CONTACT_ID, 0)
            .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
            .withValue(StructuredName.DISPLAY_NAME, draft.savedName)
            .build()
        for (n in draft.cleanNumbers) {
            ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, n.number)
                .withValue(Phone.TYPE, n.type)
                .build()
        }
        resolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    private fun update(contactId: Long, draft: ContactDraft) {
        val rawIds = rawContactIds(contactId)
        require(rawIds.isNotEmpty()) { "contact not found" }
        val main = rawIds.first()
        val inRaws = "${Data.RAW_CONTACT_ID} IN (${rawIds.joinToString(",")})"
        val ops = ArrayList<ContentProviderOperation>()

        // Name: replace on every linked entry, clearing the parts so Android re-splits it
        val hasName = hasRow(rawIds, StructuredName.CONTENT_ITEM_TYPE)
        if (hasName) {
            ops += ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection("$inRaws AND ${Data.MIMETYPE} = ?", arrayOf(StructuredName.CONTENT_ITEM_TYPE))
                .withValue(StructuredName.DISPLAY_NAME, draft.savedName)
                .withValue(StructuredName.GIVEN_NAME, null)
                .withValue(StructuredName.FAMILY_NAME, null)
                .withValue(StructuredName.MIDDLE_NAME, null)
                .withValue(StructuredName.PREFIX, null)
                .withValue(StructuredName.SUFFIX, null)
                .build()
        } else {
            ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValue(Data.RAW_CONTACT_ID, main)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, draft.savedName)
                .build()
        }

        // Numbers: replace the whole set with what the editor shows
        ops += ContentProviderOperation.newDelete(Data.CONTENT_URI)
            .withSelection("$inRaws AND ${Data.MIMETYPE} = ?", arrayOf(Phone.CONTENT_ITEM_TYPE))
            .build()
        for (n in draft.cleanNumbers) {
            ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValue(Data.RAW_CONTACT_ID, main)
                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, n.number)
                .withValue(Phone.TYPE, n.type)
                .build()
        }

        // Favourite
        ops += ContentProviderOperation.newUpdate(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId))
            .withValue(ContactsContract.Contacts.STARRED, if (draft.starred) 1 else 0)
            .build()

        resolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    private fun rawContactIds(contactId: Long): List<Long> {
        val ids = mutableListOf<Long>()
        resolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts._ID),
            "${RawContacts.CONTACT_ID} = ? AND ${RawContacts.DELETED} = 0",
            arrayOf(contactId.toString()),
            null,
        )?.use { c -> while (c.moveToNext()) ids += c.getLong(0) }
        return ids
    }

    private fun hasRow(rawIds: List<Long>, mime: String): Boolean =
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(Data._ID),
            "${Data.RAW_CONTACT_ID} IN (${rawIds.joinToString(",")}) AND ${Data.MIMETYPE} = ?",
            arrayOf(mime),
            null,
        )?.use { it.count > 0 } ?: false
}
