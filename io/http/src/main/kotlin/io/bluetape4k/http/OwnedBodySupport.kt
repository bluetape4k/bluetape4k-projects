package io.bluetape4k.http

import io.bluetape4k.io.ByteLimitExceededException
import io.bluetape4k.io.readAllBytes
import java.io.InputStream

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
            current.addSuppressedIfDistinct(accessorFailure)
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
                current.addSuppressedIfDistinct(closeFailure)
            }
        }
    }
}

private fun Throwable.addSuppressedIfDistinct(secondary: Throwable) {
    if (this !== secondary && suppressed.none { it === secondary }) {
        addSuppressed(secondary)
    }
}
