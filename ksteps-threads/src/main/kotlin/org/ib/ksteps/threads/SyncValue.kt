package org.ib.ksteps.threads

import mu.KotlinLogging
import java.time.Clock
import java.util.*
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

private val logger = KotlinLogging.logger {}

class SyncValue<T>(initValue: T,
                   override val name: String = "") : SyncValueImmutable<T> {

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val listeners = mutableListOf<ValueChangedListener<T>>()

    override var value: T = initValue
        get() = lock.withLock { field }
        set(value) = lock.withLock {
            val old = field
            field = value

            condition.signalAll()

            if (old != value) {
                fireValueChanged(old = old,
                                 new = value)
            }
        }

    private fun fireValueChanged(old: T,
                                 new: T) = synchronized(lock = listeners) {
        listeners.toTypedArray()
    }.forEach { listener ->
        listener.changed(old = old,
                         new = new)
    }

    override fun addListener(fireCurrentState: Boolean,
                             listener: ValueChangedListener<T>): ValueChangedListener<T> {
        synchronized(lock = listeners) {
            listeners += listener
        }

        if (fireCurrentState) {
            fireValueChanged(value,
                             value)
        }

        return listener
    }

    override fun removeListener(listener: ValueChangedListener<T>) = synchronized(lock = listeners) {
        listeners -= listener
    }

    override fun waitFor(message: String,
                         predicate: (T) -> Boolean): T {
        logger.info { message }

        lock.withLock {
            while (!predicate.invoke(value)) {
                condition.await()
            }

            return value
        }
    }

    override fun waitFor(duration: Duration,
                         message: String,
                         predicate: (T) -> Boolean): T {
        val deadline = Date(Clock.systemUTC()
                                .millis() + duration.inWholeMilliseconds)

        logger.info { message }

        lock.withLock {
            while (!predicate.invoke(value)) {
                val elapsed = condition.awaitUntil(deadline)
                if (!elapsed) {
                    throw TimeoutException("Wait for $name expired in $duration and condition not met!")
                }
            }

            return value
        }
    }

    override fun waitFor(duration: java.time.Duration,
                         message: String,
                         predicate: (T) -> Boolean): T = waitFor(duration = duration.toKotlinDuration(),
                                                                 message = message,
                                                                 predicate = predicate)
}

fun waitFor(values: Array<SyncValueImmutable<out Any>>,
            message: String = "Wait for: ${
                values.joinToString(separator = ", ",
                                    prefix = "[",
                                    postfix = "]") { it.name }
            }...",
            predicate: (Array<out Any>) -> Boolean) {
    val lock = ReentrantLock()
    val condition = lock.newCondition()

    val listener = ValueChangedListener<Any> { _, _ ->
        lock.withLock {
            condition.signalAll()
        }
    }

    values.forEach { value ->
        value.addListener(fireCurrentState = false,
                          listener = listener)
    }

    logger.info { message }

    try {
        lock.withLock {
            while (!predicate.invoke(values.map { it.value }
                                         .toTypedArray())) {
                condition.await()
            }
        }
    } finally {
        values.forEach { value ->
            value.removeListener(listener = listener)
        }
    }
}

fun waitFor(duration: Duration,
            values: Array<SyncValueImmutable<out Any>>,
            message: String = "Wait for: ${
                values.joinToString(separator = ", ",
                                    prefix = "[",
                                    postfix = "]") { it.name }
            }...",
            predicate: (Array<out Any>) -> Boolean) {
    val deadline = Date(Clock.systemUTC()
                            .millis() + duration.inWholeMilliseconds)

    val lock = ReentrantLock()
    val condition = lock.newCondition()

    val listener = ValueChangedListener<Any> { _, _ ->
        lock.withLock {
            condition.signalAll()
        }
    }

    values.forEach { value ->
        value.addListener(fireCurrentState = false,
                          listener = listener)
    }

    logger.info { message }

    try {
        lock.withLock {
            while (!predicate.invoke(values.map { it.value }
                                         .toTypedArray())) {
                val elapsed = condition.awaitUntil(deadline)
                if (!elapsed) {
                    throw TimeoutException("Wait for ${
                        values.joinToString(separator = ", ",
                                            prefix = "[",
                                            postfix = "]") { it.name }
                    } expired in $duration and condition not met!")
                }
            }
        }
    } finally {
        values.forEach { value ->
            value.removeListener(listener = listener)
        }
    }
}
