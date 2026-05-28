package io.bluetape4k.ktor.observability

import io.bluetape4k.support.requireNotBlank
import org.slf4j.event.Level
import java.io.Serializable

/**
 * Call logging settings for the bluetape4k Ktor observability baseline.
 */
data class CallLoggingSettings(
    val correlationId: CorrelationIdSettings = CorrelationIdSettings(),
    val level: Level = Level.INFO,
    val excludedPaths: Set<String> = setOf("/healthz", "/readyz", "/metrics"),
): Serializable {

    init {
        excludedPaths.forEach { it.requireNotBlank("excludedPaths") }
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
