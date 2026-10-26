package com.rr.numio.phone

import com.rr.numio.phone.call.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class CallFormatTest {
    @Test
    fun minutesAndSeconds() {
        assertEquals("00:00", formatDuration(0))
        assertEquals("03:42", formatDuration(222))
        assertEquals("59:59", formatDuration(3599))
    }

    @Test
    fun hoursWhenLong() {
        assertEquals("1:00:00", formatDuration(3600))
        assertEquals("2:03:04", formatDuration(7384))
    }

    @Test
    fun negativeIsZero() {
        assertEquals("00:00", formatDuration(-5))
    }
}
