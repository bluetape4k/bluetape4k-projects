package io.bluetape4k.ktor.core

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing

/**
 * Installs the bluetape4k baseline Ktor server features.
 *
 * ## Contract
 * - Installation is explicit and application-owned.
 * - JSON, default error responses, and health routes can be enabled independently.
 * - The installer does not register any application routes beyond health/readiness.
 *
 * ```kotlin
 * fun Application.module() {
 *     installBluetape4kKtorCore()
 * }
 * ```
 */
fun Application.installBluetape4kKtorCore(
    config: Bluetape4kKtorCoreConfig = Bluetape4kKtorCoreConfig(),
) {
    if (config.installContentNegotiation) {
        install(ContentNegotiation) {
            json(config.json)
        }
    }

    if (config.installStatusPages) {
        install(StatusPages) {
            bluetape4kErrorResponses()
        }
    }

    if (config.installHealthRoutes) {
        routing {
            bluetape4kHealthRoutes(
                healthPath = config.healthPath,
                readinessPath = config.readinessPath
            )
        }
    }
}
