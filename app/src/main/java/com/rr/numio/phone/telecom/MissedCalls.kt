package com.rr.numio.phone.telecom

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.TelecomManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rr.numio.phone.MainActivity
import com.rr.numio.phone.R
import com.rr.numio.phone.data.PhoneData

/**
 * Numio's own missed-call notification, with Call back and Message buttons.
 *
 * Android sends the default phone app a "show missed calls" message instead of posting
 * its own notification, but only to an app that has phone-state access. So the
 * receiver below starts switched off and is switched on only once that access is
 * granted ([syncReceiver]); until then Android keeps showing its usual notification
 * and a missed call is never silent.
 */
object MissedCalls {
    private const val CHANNEL = "missed_calls"
    private const val ID = 2
    private const val AMBER = 0xFFFFC21A.toInt()

    const val ACTION_CALL_BACK = "com.rr.numio.phone.CALL_BACK"
    const val ACTION_SHOW_RECENTS = "com.rr.numio.phone.SHOW_RECENTS"
    const val EXTRA_NUMBER = "number"

    fun syncReceiver(context: Context) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
        val state = if (granted) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        try {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, Receiver::class.java),
                state,
                PackageManager.DONT_KILL_APP,
            )
        } catch (e: Exception) {
            // Leave it as it was
        }
    }

    /** Marks missed calls as seen (Android then asks us to clear the notification). */
    fun markSeen(context: Context) {
        try {
            context.getSystemService(TelecomManager::class.java).cancelMissedCallsNotification()
        } catch (e: Exception) {
            // Not the default phone app: nothing to clear
        }
        NotificationManagerCompat.from(context).cancel(ID)
    }

    fun show(context: Context, count: Int, number: String?) {
        val nm = NotificationManagerCompat.from(context)
        if (count <= 0) {
            nm.cancel(ID)
            return
        }
        if (!CallNotifications.canPost(context)) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_missed), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val who = number?.takeIf { it.isNotBlank() }?.let { n ->
            val name = PhoneData(context).nameFor(n)
            val country = PhoneData.countryLabel(context, n)
            listOfNotNull(name ?: PhoneData.format(context, n), country).joinToString(" · ")
        }
        val title = if (count == 1) {
            context.getString(R.string.missed_call)
        } else {
            context.getString(R.string.missed_calls, count)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_call_missed)
            .setContentTitle(title)
            .setContentText(who ?: context.getString(R.string.missed_calls_open))
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setColor(AMBER)
            .setAutoCancel(true)
            .setContentIntent(activity(context, 10, Intent(ACTION_SHOW_RECENTS)))
        // Buttons only when there's one number to call back
        if (count == 1 && !number.isNullOrBlank()) {
            builder.addAction(
                R.drawable.ic_call,
                context.getString(R.string.call_back),
                activity(context, 11, Intent(ACTION_CALL_BACK).putExtra(EXTRA_NUMBER, number)),
            )
            builder.addAction(
                R.drawable.ic_message,
                context.getString(R.string.message),
                PendingIntent.getActivity(
                    context, 12,
                    Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
        try {
            nm.notify(ID, builder.build())
        } catch (e: SecurityException) {
            // Notifications were turned off
        }
    }

    private fun activity(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode,
            intent.setClass(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Android's "show missed calls" message to the default phone app. */
    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION) return
            val count = intent.getIntExtra(TelecomManager.EXTRA_NOTIFICATION_COUNT, 0)
            val number = intent.getStringExtra(TelecomManager.EXTRA_NOTIFICATION_PHONE_NUMBER)
            show(context, count, number)
        }
    }
}
