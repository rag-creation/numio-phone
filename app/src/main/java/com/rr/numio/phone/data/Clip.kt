package com.rr.numio.phone.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import com.rr.numio.phone.R

/** Copy and paste phone numbers through the system clipboard. Nothing leaves the phone. */
object Clip {
    fun copyNumber(context: Context, number: String) {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return
        cm.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.phone_number), number))
        // Android 13+ shows its own "Copied" confirmation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.number_copied, Toast.LENGTH_SHORT).show()
        }
    }

    /** Text on the clipboard, or null when there's nothing usable. */
    fun pasteText(context: Context): String? {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return null
        val item = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0) ?: return null
        return item.coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
    }
}
