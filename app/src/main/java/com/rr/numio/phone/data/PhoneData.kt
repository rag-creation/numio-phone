package com.rr.numio.phone.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import java.util.Locale

/**
 * Reads Android's own call log and contacts. Nothing is copied or stored by the app:
 * the lists are read fresh from the phone whenever they change.
 */
class PhoneData(context: Context) {
    private val app = context.applicationContext
    private val resolver = app.contentResolver

    fun hasCallLog() = granted(Manifest.permission.READ_CALL_LOG)
    fun hasContacts() = granted(Manifest.permission.READ_CONTACTS)

    private fun granted(p: String) = ContextCompat.checkSelfPermission(app, p) == PackageManager.PERMISSION_GRANTED

    /** Emits once now and again whenever [uri] changes. */
    private fun changes(uri: Uri): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        try {
            resolver.registerContentObserver(uri, true, observer)
        } catch (e: SecurityException) {
            // Access was taken away; the list just won't refresh by itself
        }
        trySend(Unit)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    fun callLog(): Flow<List<CallEntry>> =
        if (!hasCallLog()) flowOf(emptyList())
        // Also re-read when contacts change, so a number saved as a contact (even from
        // WhatsApp) gets its name in Recents straight away, not only after the next call
        else merge(
            changes(CallLog.Calls.CONTENT_URI),
            if (hasContacts()) changes(ContactsContract.Contacts.CONTENT_URI) else emptyFlow(),
        ).conflate().map { readCallLog() }.flowOn(Dispatchers.IO)

    fun contacts(): Flow<List<Contact>> =
        if (!hasContacts()) flowOf(emptyList())
        else changes(ContactsContract.Contacts.CONTENT_URI).map { readContacts() }.flowOn(Dispatchers.IO)

    private fun readCallLog(limit: Int = 500): List<CallEntry> {
        if (!hasCallLog()) return emptyList()
        val names = if (hasContacts()) nameIndex() else emptyMap()
        val sims = simLabels()
        val out = mutableListOf<CallEntry>()
        try {
            resolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE,
                    CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.PHONE_ACCOUNT_ID,
                ),
                null, null,
                "${CallLog.Calls.DATE} DESC",
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(CallLog.Calls._ID)
                val iNum = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val iName = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val iType = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val iDate = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val iDur = c.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val iAcc = c.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID)
                while (c.moveToNext() && out.size < limit) {
                    val number = c.getString(iNum).orEmpty()
                    val kind = when (c.getInt(iType)) {
                        CallLog.Calls.OUTGOING_TYPE -> CallKind.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallKind.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallKind.REJECTED
                        CallLog.Calls.BLOCKED_TYPE -> CallKind.BLOCKED
                        else -> CallKind.INCOMING
                    }
                    out += CallEntry(
                        id = c.getLong(iId),
                        number = number,
                        name = names[PhoneNumbers.matchKey(number)] ?: c.getString(iName),
                        kind = kind,
                        timeMillis = c.getLong(iDate),
                        durationSec = c.getLong(iDur),
                        simLabel = c.getString(iAcc)?.let { sims[it] },
                    )
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return out
    }

    /** Number key → contact name, for naming calls in the log and on the call screen. */
    private fun nameIndex(): Map<String, String> {
        val map = HashMap<String, String>()
        try {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val key = PhoneNumbers.matchKey(c.getString(0).orEmpty())
                    val name = c.getString(1)
                    if (key.isNotEmpty() && name != null) map.putIfAbsent(key, name)
                }
            }
        } catch (e: SecurityException) {
            // no contacts access
        }
        return map
    }

    private fun readContacts(): List<Contact> {
        if (!hasContacts()) return emptyList()
        data class Row(val name: String, val starred: Boolean, val lookup: String, val photo: String?)
        val rows = LinkedHashMap<Long, Row>()
        val numbers = HashMap<Long, MutableList<ContactNumber>>()
        try {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL,
                    ContactsContract.CommonDataKinds.Phone.STARRED,
                    ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: continue
                    val number = c.getString(2) ?: continue
                    val label = ContactsContract.CommonDataKinds.Phone
                        .getTypeLabel(app.resources, c.getInt(3), c.getString(4)).toString()
                    rows.getOrPut(id) { Row(name, c.getInt(5) == 1, c.getString(6).orEmpty(), c.getString(7)) }
                    val list = numbers.getOrPut(id) { mutableListOf() }
                    // Same number saved twice (e.g. with and without +971): keep one
                    if (list.none { PhoneNumbers.same(it.number, number) }) list += ContactNumber(number, label, c.getInt(3))
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return ContactSearch.sorted(
            rows.map { (id, r) -> Contact(id, r.lookup, r.name, numbers[id].orEmpty(), r.starred, r.photo) },
        )
    }

    /** Contact name for one number, used by the call screen. */
    fun nameFor(number: String): String? {
        if (!hasContacts() || number.isBlank()) return null
        return try {
            resolver.query(
                Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null,
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (e: Exception) {
            null
        }
    }

    /** Phone account id → "SIM 1" / carrier label. */
    private fun simLabels(): Map<String, String> = try {
        val telecom = app.getSystemService(TelecomManager::class.java)
        @Suppress("MissingPermission")
        val handles: List<PhoneAccountHandle> = telecom.callCapablePhoneAccounts
        if (handles.size < 2) {
            emptyMap() // one SIM: no need to label every call
        } else {
            handles.associate { h -> h.id to (telecom.getPhoneAccount(h)?.label?.toString() ?: h.id) }
        }
    } catch (e: SecurityException) {
        emptyMap()
    }

    fun contactUri(contact: Contact): Uri =
        ContactsContract.Contacts.getLookupUri(contact.id, contact.lookupKey)
            ?: ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.id)

    companion object {
        private fun countryIso(context: Context): String {
            val tm = context.getSystemService(TelephonyManager::class.java)
            return (tm?.networkCountryIso?.takeIf { it.isNotBlank() }
                ?: tm?.simCountryIso?.takeIf { it.isNotBlank() }
                ?: Locale.getDefault().country).uppercase(Locale.ROOT)
        }

        /** "🇮🇳 India" when the number is from another country than the phone is in, else null. */
        fun countryLabel(context: Context, number: String): String? =
            CallerCountry.label(number, countryIso(context))

        /** Just the flag, same rule as [countryLabel]. */
        fun countryFlag(context: Context, number: String): String? =
            CallerCountry.flagFor(number, countryIso(context))

        /** "050 123 4567" style, using the phone's country. Falls back to the raw number. */
        fun format(context: Context, number: String): String =
            PhoneNumberUtils.formatNumber(number, countryIso(context)) ?: number
    }
}
