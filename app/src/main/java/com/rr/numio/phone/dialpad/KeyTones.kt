package com.rr.numio.phone.dialpad

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

/**
 * The short beep each dial key makes (DTMF tone), like a classic phone.
 * Stays quiet when the phone is on silent or vibrate.
 */
class KeyTones(context: Context) {
    private val audio = context.applicationContext.getSystemService(AudioManager::class.java)
    private val generator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_DTMF, 80)
    } catch (e: RuntimeException) {
        null // some phones refuse a tone generator; keys just stay silent
    }

    fun play(key: Char) {
        val tone = toneFor(key) ?: return
        if (audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        generator?.startTone(tone, 120)
    }

    fun release() {
        generator?.release()
    }

    private fun toneFor(key: Char): Int? = when (key) {
        '0' -> ToneGenerator.TONE_DTMF_0
        '1' -> ToneGenerator.TONE_DTMF_1
        '2' -> ToneGenerator.TONE_DTMF_2
        '3' -> ToneGenerator.TONE_DTMF_3
        '4' -> ToneGenerator.TONE_DTMF_4
        '5' -> ToneGenerator.TONE_DTMF_5
        '6' -> ToneGenerator.TONE_DTMF_6
        '7' -> ToneGenerator.TONE_DTMF_7
        '8' -> ToneGenerator.TONE_DTMF_8
        '9' -> ToneGenerator.TONE_DTMF_9
        '*' -> ToneGenerator.TONE_DTMF_S
        '#' -> ToneGenerator.TONE_DTMF_P
        '+' -> ToneGenerator.TONE_DTMF_0
        else -> null
    }
}
