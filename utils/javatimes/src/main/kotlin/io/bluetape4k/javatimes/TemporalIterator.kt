package io.bluetape4k.javatimes

import java.time.temporal.Temporal

/**
 * An iterator over a buildSequence of values of type [Temporal]
 */
abstract class TemporalIterator<out T: Temporal>: Iterator<T> {

    /**
     *  Returns the next value in the buildSequence without boxing.
     */
    abstract fun nextTemporal(): T

    /**
     * Returns the next element in the iteration.
     */
    final override fun next(): T = nextTemporal()
}
