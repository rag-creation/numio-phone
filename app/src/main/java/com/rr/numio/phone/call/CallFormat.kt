package com.rr.numio.phone.call

// Plain Kotlin (no Android), so it can be unit tested on the computer.

/** "03:42" or "1:03:42". */
fun formatDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}
