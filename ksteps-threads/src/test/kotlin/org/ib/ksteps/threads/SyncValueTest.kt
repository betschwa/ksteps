package org.ib.ksteps.threads

import mu.KotlinLogging
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

class SyncValueTest {

    @Test
    fun setValue() {
        val value = SyncValue(initValue = false,
                              name = "TV")
        assertFalse(actual = value.value,
                    message = "Value not initialised")

        value.value = true

        assertTrue(actual = value.value,
                   message = "Value not changed")
    }

    @Test
    fun waitFor() {
        val value = SyncValue(initValue = false,
                              name = "TV")
        assertFalse(actual = value.value,
                    message = "Value not initialised")

        kotlin.concurrent.timer(name = "Set value timer",
                                daemon = true,
                                initialDelay = 10_000L,
                                period = Long.MAX_VALUE) {
            value.value = true
        }

        assertTrue(actual = value.waitFor { it },
                   message = "Value not changed")
    }

    @Test
    fun waitForWithDuration() {
        val value = SyncValue(initValue = false,
                              name = "TV")
        assertFalse(actual = value.value,
                    message = "Value not initialised")

        kotlin.concurrent.timer(name = "Set value timer",
                                daemon = true,
                                initialDelay = 10_000L,
                                period = Long.MAX_VALUE) {
            value.value = true
        }

        assertTrue(actual = value.waitFor(duration = 30.seconds) { it },
                   message = "Value not changed")
    }

    @Test
    fun timeout() {
        val value = SyncValue(initValue = false,
                              name = "TV")
        assertFalse(actual = value.value,
                    message = "Value not initialised")

        assertThrows<TimeoutException> {
            value.waitFor(duration = 2.seconds) { it }
        }
    }

    @Test
    fun waitForMany() {
        val initial1 = false
        val initial2 = 0

        val expected1 = true
        val expected2 = 1

        val value1 = SyncValue(initValue = initial1,
                               name = "TV 1")
        val value2 = SyncValue(initValue = initial2,
                               name = "TV 2")

        assertEquals(expected = initial1,
                     actual = value1.value,
                     message = "Value 1 not initialised")
        assertEquals(expected = initial2,
                     actual = value2.value,
                     message = "Value 2 not initialised")

        kotlin.concurrent.timer(name = "Set TV 1 Timer",
                                daemon = true,
                                initialDelay = 2_000L,
                                period = Long.MAX_VALUE) {
            value1.value = expected1
        }
        kotlin.concurrent.timer(name = "Set TV 2 Timer",
                                daemon = true,
                                initialDelay = 4_000L,
                                period = Long.MAX_VALUE) {
            value2.value = expected2
        }

        waitFor(values = arrayOf(value1,
                                 value2)) { values ->
            values[0] == expected1 && values[1] == expected2
        }

        assertEquals(expected = expected1,
                     actual = value1.value,
                     message = "TV 1 not updated")
        assertEquals(expected = expected2,
                     actual = value2.value,
                     message = "TV 2 not updated")
    }

    @Test
    fun timeoutMany() {
        val initial1 = false
        val initial2 = 0

        val expected1 = true
        val expected2 = 1

        val value1 = SyncValue(initValue = initial1,
                               name = "TV 1")
        val value2 = SyncValue(initValue = initial2,
                               name = "TV 2")

        assertEquals(expected = initial1,
                     actual = value1.value,
                     message = "Value 1 not initialised")
        assertEquals(expected = initial2,
                     actual = value2.value,
                     message = "Value 2 not initialised")

        assertThrows<TimeoutException>(message = "No timeout") {
            waitFor(duration = 5.seconds,
                    values = arrayOf(value1,
                                     value2)) { values ->
                values[0] == expected1 && values[1] == expected2
            }
        }
    }
}