package io.bluetape4k.ktor.core

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * Adds standard health and readiness endpoints to a Ktor [Route].
 *
 * ## Contract
 * - Both endpoints return [HealthResponse.up] by default.
 * - Custom paths must be absolute route paths, for example `/healthz`.
 *
 * ```kotlin
 * routing {
 *     bluetape4kHealthRoutes()
 * }
 * ```
 */
fun Route.bluetape4kHealthRoutes(
    healthPath: String = Bluetape4kKtorCoreConfig.DEFAULT_HEALTH_PATH,
    readinessPath: String = Bluetape4kKtorCoreConfig.DEFAULT_READINESS_PATH,
) {
    healthPath.requireAbsoluteKtorPath("healthPath")
    readinessPath.requireAbsoluteKtorPath("readinessPath")

    get(healthPath) {
        call.respond(HealthResponse.up())
    }
    get(readinessPath) {
        call.respond(HealthResponse.up())
    }
}
