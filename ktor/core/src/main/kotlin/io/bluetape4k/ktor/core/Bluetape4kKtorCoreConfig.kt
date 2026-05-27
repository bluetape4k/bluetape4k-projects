package io.bluetape4k.ktor.core

import io.bluetape4k.support.requireNotBlank
import kotlinx.serialization.json.Json

/**
 * Explicit opt-in configuration for [installBluetape4kKtorCore].
 *
 * ## Contract
 * - No Spring Boot auto-configuration is involved.
 * - Applications can disable each installed Ktor feature independently.
 * - Health and readiness paths must be absolute Ktor route paths.
 */
class Bluetape4kKtorCoreConfig(
    val json: Json = Bluetape4kKtorJson.defaultJson(),
    val installContentNegotiation: Boolean = true,
    val installStatusPages: Boolean = true,
    val installHealthRoutes: Boolean = true,
    val healthPath: String = DEFAULT_HEALTH_PATH,
    val readinessPath: String = DEFAULT_READINESS_PATH,
) {

    init {
        healthPath.requireAbsoluteKtorPath("healthPath")
        readinessPath.requireAbsoluteKtorPath("readinessPath")
    }

    companion object {
        const val DEFAULT_HEALTH_PATH: String = "/healthz"
        const val DEFAULT_READINESS_PATH: String = "/readyz"
    }
}

internal fun String.requireAbsoluteKtorPath(parameterName: String): String {
    requireNotBlank(parameterName)
    require(startsWith("/")) { "$parameterName[$this] must start with '/'." }
    return this
}
