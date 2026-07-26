package io.bluetape4k.redis.lettuce.coordination.internal

import java.nio.charset.StandardCharsets

internal enum class CoordinationFailureClassification {
    BACKEND,
    INTEGRITY,
}

internal class CoordinationProtocolException(
    val classification: CoordinationFailureClassification,
    message: String,
    cause: Throwable? = null,
): IllegalStateException(message, cause)

internal data class CoordinationFrame(
    val tag: String,
    private val fields: List<String>,
) {
    fun field(index: Int): String =
        fields.getOrNull(index)
            ?: throw CoordinationProtocol.integrityFailure("response field is missing")

    fun nonNegativeLong(index: Int): Long {
        val value = field(index).toLongOrNull()
            ?: throw CoordinationProtocol.integrityFailure("response number is malformed")
        if (value < 0L) {
            throw CoordinationProtocol.integrityFailure("response number must be non-negative")
        }
        return value
    }
}

internal object CoordinationProtocol {
    const val MAX_ITEMS: Int = 16
    const val MAX_BYTES: Int = 256

    fun decode(
        raw: Any?,
        expectedArities: Map<String, Int>,
    ): CoordinationFrame {
        val items = raw as? List<*>
            ?: throw integrityFailure("response must be a tagged list")
        if (items.isEmpty() || items.size > MAX_ITEMS) {
            throw integrityFailure("response item count is out of bounds")
        }

        val values = items.map(::boundedString)
        val responseBytes = values.sumOf { it.toByteArray(StandardCharsets.UTF_8).size }
        if (responseBytes > MAX_BYTES) {
            throw integrityFailure("response byte size is out of bounds")
        }

        val tag = values.first()
        val expectedArity = expectedArities[tag]
            ?: throw integrityFailure("response tag is unknown")
        if (values.size != expectedArity) {
            throw integrityFailure("response arity is invalid")
        }
        return CoordinationFrame(tag, values.drop(1))
    }

    fun backendFailure(operation: String, cause: Throwable): CoordinationProtocolException =
        CoordinationProtocolException(
            classification = CoordinationFailureClassification.BACKEND,
            message = "coordination backend operation failed: $operation",
            cause = cause,
        )

    internal fun integrityFailure(message: String): CoordinationProtocolException =
        CoordinationProtocolException(CoordinationFailureClassification.INTEGRITY, message)

    private fun boundedString(value: Any?): String =
        when (value) {
            is String -> value
            is ByteArray -> value.toString(StandardCharsets.UTF_8)
            is Byte -> value.toString()
            is Short -> value.toString()
            is Int -> value.toString()
            is Long -> value.toString()
            else -> throw integrityFailure("response value type is unsupported")
        }
}
