// app/src/test/java/com/bikeputer/ride/SessionTimerTest.kt
package com.bikeputer.ride

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionTimerTest {
    @Test fun accumulates_only_while_running() {
        val t = SessionTimer()
        t.start(1000L)                               // started at 1000
        assertEquals(3000L, t.currentElapsed(4000L)) // ran 3s
        t.pause(4000L)                               // paused at 4000, banked 3000
        assertEquals(3000L, t.currentElapsed(9000L)) // frozen while paused
        t.start(9000L)                               // resume at 9000
        assertEquals(5000L, t.currentElapsed(11000L)) // 3000 + 2000
    }

    @Test fun reset_zeroes_elapsed() {
        val t = SessionTimer()
        t.start(1000L); t.pause(2000L); t.reset()
        assertEquals(0L, t.currentElapsed(123456L))
    }
}
