package io.bluetape4k.io

import java.nio.BufferOverflowException
import java.util.*
import java.util.concurrent.CancellationException

internal object BufferFailurePolicy {

    private const val KRYO_OVERFLOW_CLASS = "com.esotericsoftware.kryo.io.KryoBufferOverflowException"

    /** Returns only graph-nested control failures, preserving Error-before-cancellation priority. */
    fun findControlFailure(failure: Throwable?): Throwable? =
        find(failure) { it is Error }
            ?: find(failure) { it is CancellationException }

    fun classify(operationFailure: Throwable?, cleanupFailure: Throwable?): Throwable? {
        find(operationFailure) { it is Error }?.let { fatal ->
            fatal.attachSuppressed(cleanupFailure)
            return fatal
        }
        find(cleanupFailure) { it is Error }?.let { fatal ->
            fatal.attachSuppressed(operationFailure)
            return fatal
        }

        // Cancellation is control flow and must not be translated into serialization failure.
        find(operationFailure) { it is CancellationException }?.let { cancellation ->
            cancellation.attachSuppressed(cleanupFailure)
            return cancellation
        }
        find(cleanupFailure) { it is CancellationException }?.let { cancellation ->
            cancellation.attachSuppressed(operationFailure)
            return cancellation
        }

        if (operationFailure is BufferOverflowException) {
            operationFailure.attachSuppressed(cleanupFailure)
            return operationFailure
        }
        if (operationFailure != null && operationFailure.containsOverflow()) {
            return publicOverflow(operationFailure).also { overflow ->
                overflow.attachSuppressed(cleanupFailure)
            }
        }

        if (operationFailure != null) {
            if (cleanupFailure != null && cleanupFailure.containsOverflow()) {
                return publicOverflow(operationFailure).also { overflow ->
                    overflow.attachSuppressed(cleanupFailure)
                }
            }
            operationFailure.attachSuppressed(cleanupFailure)
            return operationFailure
        }

        if (cleanupFailure is BufferOverflowException) return cleanupFailure
        if (cleanupFailure != null && cleanupFailure.containsOverflow()) return publicOverflow(cleanupFailure)
        return cleanupFailure
    }

    private fun Throwable.containsOverflow(): Boolean =
        find(this) { failure ->
            failure is BufferOverflowException || failure.javaClass.name == KRYO_OVERFLOW_CLASS
        } != null

    private fun publicOverflow(cause: Throwable): BufferOverflowException =
        BufferOverflowException().apply { initCause(cause) }

    private fun Throwable.attachSuppressed(secondary: Throwable?) {
        if (secondary == null || this === secondary) return
        if (reaches(this, secondary) || reaches(secondary, this)) return
        if (suppressed.any { it === secondary }) return
        addSuppressed(secondary)
    }

    private fun reaches(root: Throwable, target: Throwable): Boolean =
        find(root) { it === target } != null

    private inline fun find(root: Throwable?, predicate: (Throwable) -> Boolean): Throwable? {
        if (root == null) return null
        val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        val pending = ArrayDeque<Throwable>()
        pending.add(root)

        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (!visited.add(current)) continue
            if (predicate(current)) return current
            current.cause?.let(pending::addLast)
            current.suppressed.forEach(pending::addLast)
        }
        return null
    }
}
