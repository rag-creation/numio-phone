package com.rr.numio.phone.telecom

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.rr.numio.phone.R
import com.rr.numio.phone.call.CallActivity

/**
 * The incoming-call notification (full screen over the lock screen, a card at the top
 * when unlocked) and the ongoing-call notification. Android draws them; we give the
 * text, buttons and Numio amber.
 */
object CallNotifications {
    private const val CHANNEL_INCOMING = "incoming_calls"
    private const val CHANNEL_ONGOING = "ongoing_calls"
    private const val ID = 1
    private const val AMBER = 0xFFFFC21A.toInt()

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Android 14+ lets people turn off full-screen call notifications per app. */
    fun canFullScreen(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 34) {
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        } else {
            true
        }

    /** The system page where full-screen notifications can be allowed for this app. */
    fun fullScreenSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 34) {
            Intent(
                android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                android.net.Uri.parse("package:" + context.packageName),
            )
        } else {
            Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

    private fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_INCOMING,
                context.getString(R.string.channel_incoming),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setSound(null, null) // Android's own ringer plays the ringtone
                enableVibration(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                context.getString(R.string.channel_ongoing),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    /** False when the last incoming-call notification couldn't be shown. */
    var lastPostOk = false
        private set

    /** True while our ringing / call screen is on display. */
    @Volatile
    var screenVisible = false

    // Whether the posted notification is the pop-up kind, so we know to replace it
    private var poppedUp = false

    fun update(context: Context, snapshot: CallSnapshot) {
        lastPostOk = false
        if (!canPost(context)) return
        val call = snapshot.primary
        if (call == null) {
            cancelAll(context)
            return
        }
        ensureChannels(context)
        // Pop-up card only when you're somewhere else. While our own ringing screen is
        // showing, a second card on top of it is just noise, so the notification stays quiet.
        val popUp = call.isRinging && !screenVisible
        if (poppedUp && !popUp) NotificationManagerCompat.from(context).cancel(ID)
        poppedUp = popUp
        val notification = if (popUp) incoming(context, call) else ongoing(context, call)
        try {
            NotificationManagerCompat.from(context).notify(ID, notification)
            lastPostOk = true
        } catch (e: SecurityException) {
            // Permission was taken away while ringing; the call screen still opens
        } catch (e: IllegalArgumentException) {
            // A phone that rejects the call style; the call screen still opens
        }
    }

    fun cancelAll(context: Context) {
        poppedUp = false
        NotificationManagerCompat.from(context).cancel(ID)
    }

    private fun person(call: CallInfo) = Person.Builder()
        .setName(call.title)
        .setImportant(true)
        .build()

    private fun openScreen(context: Context, requestCode: Int, answer: Boolean): PendingIntent {
        val intent = Intent(context, CallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (answer) intent.action = CallActivity.ACTION_ANSWER
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun broadcast(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun incoming(context: Context, call: CallInfo) =
        NotificationCompat.Builder(context, CHANNEL_INCOMING)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle(call.title)
            .setContentText(subtitle(context, call))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setColor(AMBER)
            .setContentIntent(openScreen(context, 1, answer = false))
            .setFullScreenIntent(openScreen(context, 2, answer = false), true)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    person(call),
                    broadcast(context, CallActionReceiver.ACTION_DECLINE, 3),
                    openScreen(context, 4, answer = true),
                ),
            )
            .build()

    private fun ongoing(context: Context, call: CallInfo) =
        NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle(call.title)
            .setContentText(subtitle(context, call))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(AMBER)
            .apply {
                if (call.isActive && call.connectTimeMillis > 0) {
                    setUsesChronometer(true)
                    setWhen(call.connectTimeMillis)
                }
            }
            .setContentIntent(openScreen(context, 5, answer = false))
            // Plain notification: Android only accepts CallStyle for an ongoing call
            // from a foreground service, which this app doesn't need.
            .addAction(
                R.drawable.ic_call_end,
                context.getString(R.string.hang_up),
                broadcast(context, CallActionReceiver.ACTION_HANG_UP, 6),
            )
            .build()

    private fun subtitle(context: Context, call: CallInfo): String {
        val what = when {
            call.isRinging -> context.getString(R.string.incoming_call)
            call.isOutgoingStarting -> context.getString(R.string.calling)
            call.isHolding -> context.getString(R.string.on_hold)
            else -> context.getString(R.string.on_call)
        }
        val sim = call.simLabel?.let { " · $it" }.orEmpty()
        val number = if (call.name.isNullOrBlank()) "" else " · ${call.number}"
        return what + sim + number
    }
}
