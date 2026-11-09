package com.rr.numio.phone.data

import android.content.ContentValues
import android.content.Context
import android.provider.BlockedNumberContract
import android.provider.BlockedNumberContract.BlockedNumbers

/** A number on Android's blocked list. */
data class BlockedNumber(val id: Long, val number: String)

/**
 * Android's own blocked-numbers list: the same one every phone and SMS app uses, so a
 * number blocked here is stopped for calls and texts. Only the default phone app (or SMS
 * app) may read or change it; everything returns empty/false otherwise.
 */
class Blocking(context: Context) {
    private val app = context.applicationContext

    fun canBlock(): Boolean = try {
        BlockedNumberContract.canCurrentUserBlockNumbers(app)
    } catch (e: Exception) {
        false
    }

    fun list(): List<BlockedNumber> = try {
        val out = mutableListOf<BlockedNumber>()
        app.contentResolver.query(
            BlockedNumbers.CONTENT_URI,
            arrayOf(BlockedNumbers.COLUMN_ID, BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val number = c.getString(1) ?: continue
                out += BlockedNumber(c.getLong(0), number)
            }
        }
        out
    } catch (e: Exception) {
        emptyList()
    }

    fun block(number: String): Boolean = try {
        val values = ContentValues().apply { put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number) }
        app.contentResolver.insert(BlockedNumbers.CONTENT_URI, values) != null
    } catch (e: Exception) {
        false
    }

    /** Unblocks every saved form of this number (with or without the country code). */
    fun unblock(number: String): Boolean = try {
        var any = false
        for (b in list()) {
            if (b.number == number || PhoneNumbers.same(b.number, number)) {
                any = BlockedNumberContract.unblock(app, b.number) > 0 || any
            }
        }
        any
    } catch (e: Exception) {
        false
    }
}
