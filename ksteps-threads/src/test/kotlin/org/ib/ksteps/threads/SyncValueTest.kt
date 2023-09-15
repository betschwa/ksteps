package org.ib.ksteps.threads

import mu.KotlinLogging
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeoutException
import kotlin.test.Test
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
                                period = 10_000L) {
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
                                period = 10_000L) {
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
}