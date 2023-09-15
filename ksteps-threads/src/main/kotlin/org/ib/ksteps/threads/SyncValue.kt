package org.ib.ksteps.threads

import mu.KotlinLogging
import java.time.Clock
import java.util.*
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

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
                         predicate: (T) -> Boolean): T {
        val deadline = Date(Clock.systemUTC()
                                .millis() + duration.toMillis())

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
}