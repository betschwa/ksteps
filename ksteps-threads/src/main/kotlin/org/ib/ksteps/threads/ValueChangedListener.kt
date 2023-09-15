package org.ib.ksteps.threads

fun interface ValueChangedListener<in T> {

    fun changed(old: T,
                new: T)
}