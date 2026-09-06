package io.bluetape4k.http

import io.bluetape4k.io.ByteLimitExceededException
import io.bluetape4k.io.readAllBytes
import java.io.InputStream
import java.util.concurrent.CancellationException

internal fun readOwnedBodyBytes(
    maxBytes: Int,
    knownOversize: Boolean,
    acquireBody: () -> InputStream?,
): ByteArray {
    var primary: Throwable? =
        if (knownOversize) ByteLimitExceededException(maxBytes) else null
    var ownedStream: InputStream? = null

    try {
        try {
            ownedStream = acquireBody()
        } catch (accessorFailure: Throwable) {
            val current = primary
            if (current == null) {
                primary = accessorFailure
                throw accessorFailure
            }
            val selected = selectPrimary(current, accessorFailure)
            primary = selected
            if (selected !== current) throw selected
        }

        primary?.let { throw it }
        val stream = ownedStream ?: return byteArrayOf()

        return try {
            stream.readAllBytes(maxBytes)
        } catch (readFailure: Throwable) {
            primary = readFailure
            throw readFailure
        }
    } finally {
        val stream = ownedStream
        if (stream != null) {
            try {
                stream.close()
            } catch (closeFailure: Throwable) {
                val current = primary
                if (current == null) throw closeFailure
                val selected = selectPrimary(current, closeFailure)
                primary = selected
                if (selected !== current) throw selected
            }
        }
    }
}

private fun selectPrimary(current: Throwable, next: Throwable): Throwable {
    if (current === next) return current

    val primary = when {
        current is Error -> current
        next is Error -> next
        current is CancellationException -> current
        next is CancellationException -> next
        else -> current
    }
    val secondary = if (primary === current) next else current
    if (primary.suppressed.none { it === secondary }) primary.addSuppressed(secondary)
    return primary
}
