package io.bluetape4k.ktor.testing

import io.bluetape4k.ktor.core.Bluetape4kKtorCoreConfig
import io.bluetape4k.ktor.core.Bluetape4kKtorJson
import io.bluetape4k.ktor.core.installBluetape4kKtorCore
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json

/**
 * Installs the bluetape4k Ktor core module in a `testApplication` block.
 *
 * ## Contract
 * - The caller still owns the surrounding Ktor `testApplication` lifecycle.
 * - The helper installs the same core defaults used by production modules.
 * - Additional test routes are registered in the same application setup phase.
 *
 * ```kotlin
 * testApplication {
 *     installBluetape4kKtorCoreForTest {
 *         get("/ping") { call.respondText("pong") }
 *     }
 * }
 * ```
 */
fun ApplicationTestBuilder.installBluetape4kKtorCoreForTest(
    config: Bluetape4kKtorCoreConfig = Bluetape4kKtorCoreConfig(),
    routes: Route.() -> Unit = {},
) {
    application {
        installBluetape4kKtorCore(config)
        routing(routes)
    }
}

/**
 * Creates a Ktor test client with bluetape4k JSON defaults.
 *
 * ## Contract
 * - Uses [Bluetape4kKtorJson.defaultJson] unless a custom [jsonFormat] is supplied.
 * - The caller can still install additional client plugins through [configure].
 */
fun ApplicationTestBuilder.bluetape4kJsonClient(
    jsonFormat: Json = Bluetape4kKtorJson.defaultJson(),
    configure: HttpClientConfig<*>.() -> Unit = {},
): HttpClient =
    createClient {
        install(ContentNegotiation) {
            json(jsonFormat)
        }
        configure()
    }
