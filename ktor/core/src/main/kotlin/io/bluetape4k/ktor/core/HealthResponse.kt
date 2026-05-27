package io.bluetape4k.ktor.core

import io.bluetape4k.support.requireNotBlank
import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

/**
 * Standard health/readiness response body.
 *
 * ## Contract
 * - [status] is a compact status token such as `UP` or `DOWN`.
 * - [details] contains optional string metadata safe to expose to clients.
 */
@Serializable
data class HealthResponse(
    val status: String = UP,
    val details: Map<String, String> = emptyMap(),
): JavaSerializable {

    init {
        status.requireNotBlank("status")
    }

    companion object {
        private const val serialVersionUID: Long = 1L
        const val UP: String = "UP"
        const val DOWN: String = "DOWN"

        fun up(details: Map<String, String> = emptyMap()): HealthResponse =
            HealthResponse(status = UP, details = details)

        fun down(details: Map<String, String> = emptyMap()): HealthResponse =
            HealthResponse(status = DOWN, details = details)
    }
}
