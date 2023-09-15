package org.ib.ksteps.threads

import kotlin.time.Duration

interface SyncValueImmutable<T> {

    val name: String
    val value: T

    fun addListener(fireCurrentState: Boolean = true,
                    listener: ValueChangedListener<T>): ValueChangedListener<T>

    fun removeListener(listener: ValueChangedListener<T>)

    fun waitFor(message: String = "Wait for: $name...",
                predicate: (T) -> Boolean): T

    fun waitFor(duration: Duration,
                message: String = "Wait for: $name...",
                predicate: (T) -> Boolean): T

    fun waitFor(duration: java.time.Duration,
                message: String = "Wait for: $name...",
                predicate: (T) -> Boolean): T
}