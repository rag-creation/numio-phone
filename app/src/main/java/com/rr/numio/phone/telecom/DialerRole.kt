package com.rr.numio.phone.telecom

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager

/** Default phone app role and placing calls. */
object DialerRole {
    fun isDefault(context: Context): Boolean {
        val roles = context.getSystemService(RoleManager::class.java) ?: return false
        return roles.isRoleAvailable(RoleManager.ROLE_DIALER) && roles.isRoleHeld(RoleManager.ROLE_DIALER)
    }

    /** Android's own "Set as default phone app?" dialog. */
    fun requestIntent(context: Context): Intent? {
        val roles = context.getSystemService(RoleManager::class.java) ?: return null
        if (!roles.isRoleAvailable(RoleManager.ROLE_DIALER)) return null
        return roles.createRequestRoleIntent(RoleManager.ROLE_DIALER)
    }

    /** Android's Default apps page, to switch to another phone app. */
    fun defaultAppsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Hands the number to Android's Telecom, which places the call through the carrier. */
    @SuppressLint("MissingPermission")
    fun placeCall(context: Context, number: String): Boolean = try {
        val telecom = context.getSystemService(TelecomManager::class.java)
        telecom.placeCall(Uri.fromParts("tel", number, null), Bundle())
        true
    } catch (e: SecurityException) {
        false
    }
}
