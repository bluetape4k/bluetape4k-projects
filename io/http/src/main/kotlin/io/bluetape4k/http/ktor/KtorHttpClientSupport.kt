package io.bluetape4k.http.ktor

import io.bluetape4k.support.requirePositiveNumber
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.Serializable
import java.time.Duration

/**
 * Default JSON configuration for Ktor Client helpers.
 *
 * ## Behavior / Contract
 * - Unknown JSON fields are ignored so clients tolerate additive server fields.
 * - Default values are encoded for stable DTO round trips in tests and examples.
 * - Explicit `null` values are omitted to match common HTTP API payloads.
 */
val defaultKtorClientJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Timeout settings installed by [ktorJsonHttpClientOf].
 *
 * ## Behavior / Contract
 * - All durations must be positive.
 * - Values are converted to Ktor's millisecond timeout plugin settings.
 * - Callers that need per-request timeouts should still use Ktor request-level configuration.
 */
data class KtorClientTimeouts(
    val requestTimeout: Duration = Duration.ofSeconds(30),
    val connectTimeout: Duration = Duration.ofSeconds(10),
    val socketTimeout: Duration = Duration.ofSeconds(30),
): Serializable {

    init {
        requestTimeout.toMillis().requirePositiveNumber("requestTimeoutMillis")
        connectTimeout.toMillis().requirePositiveNumber("connectTimeoutMillis")
        socketTimeout.toMillis().requirePositiveNumber("socketTimeoutMillis")
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Creates an [HttpClient] with the given [engineFactory] and optional [block] configuration.
 *
 * ## Behavior / Contract
 * - The caller owns the returned [HttpClient] and is responsible for calling [HttpClient.close].
 * - No plugins, defaults, or policies are installed; configuration is entirely up to the caller.
 * - `ktor-client-core` and the chosen engine artifact are declared `compileOnly` in this module.
 *   Consumers must add the required Ktor artifacts to their own compile classpath.
 *
 * ```kotlin
 * val client = ktorHttpClientOf(CIO) {
 *     engine {
 *         requestTimeout = 10_000
 *     }
 * }
 * client.use { /* requests */ }
 * ```
 *
 * @param engineFactory the Ktor [HttpClientEngineFactory] to use (e.g. [CIO], OkHttp, Java)
 * @param block optional [HttpClientConfig] DSL block applied after engine selection
 * @return a new [HttpClient] instance
 */
fun <T : HttpClientEngineConfig> ktorHttpClientOf(
    engineFactory: HttpClientEngineFactory<T>,
    block: HttpClientConfig<T>.() -> Unit = {},
): HttpClient = HttpClient(engineFactory, block)

/**
 * Creates a Ktor [HttpClient] with JSON content negotiation and timeout defaults.
 *
 * ## Behavior / Contract
 * - Engine selection remains explicit through [engineFactory]; this helper does not choose CIO, Java, Apache, or mock.
 * - Installs only Ktor's `ContentNegotiation` JSON plugin and `HttpTimeout` plugin.
 * - Does not install retry, resilience, logging, metrics, authentication, or tracing policies.
 * - The caller owns the returned [HttpClient] and is responsible for calling [HttpClient.close].
 *
 * ```kotlin
 * val client = ktorJsonHttpClientOf(CIO) {
 *     engine {
 *         requestTimeout = 10_000
 *     }
 * }
 * ```
 *
 * @param engineFactory the Ktor [HttpClientEngineFactory] to use
 * @param json JSON configuration for request/response serialization
 * @param timeouts client-level timeout defaults
 * @param block optional [HttpClientConfig] DSL block applied after shared plugins
 * @return a new [HttpClient] instance
 */
fun <T : HttpClientEngineConfig> ktorJsonHttpClientOf(
    engineFactory: HttpClientEngineFactory<T>,
    json: Json = defaultKtorClientJson,
    timeouts: KtorClientTimeouts = KtorClientTimeouts(),
    block: HttpClientConfig<T>.() -> Unit = {},
): HttpClient =
    HttpClient(engineFactory) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeouts.requestTimeout.toMillis()
            connectTimeoutMillis = timeouts.connectTimeout.toMillis()
            socketTimeoutMillis = timeouts.socketTimeout.toMillis()
        }
        block()
    }

/**
 * Creates an [HttpClient] backed by the Ktor CIO engine.
 *
 * ## Behavior / Contract
 * - CIO is suspend-native and supports HTTP/1.x only. It does not support HTTP/2.
 * - For HTTP/2 use cases prefer HC5 Async, JDK, or OkHttp engines.
 * - The caller owns the returned [HttpClient] and is responsible for calling [HttpClient.close].
 *
 * ```kotlin
 * val client = ktorCioHttpClientOf {
 *     engine {
 *         requestTimeout = 10_000
 *     }
 * }
 * client.use { /* requests */ }
 * ```
 *
 * @param block optional [HttpClientConfig] DSL block for CIO-specific configuration
 * @return a new [HttpClient] backed by the CIO engine
 */
fun ktorCioHttpClientOf(
    block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {},
): HttpClient = HttpClient(CIO, block)

/**
 * Creates a CIO-backed Ktor [HttpClient] with JSON content negotiation and timeout defaults.
 *
 * ## Behavior / Contract
 * - CIO remains HTTP/1.x only; prefer HC5 Async, JDK, or OkHttp engines for HTTP/2 use cases.
 * - Only JSON and timeout plugins are installed. Retry/resilience policy stays outside this helper.
 * - The caller owns the returned [HttpClient] and is responsible for calling [HttpClient.close].
 *
 * ```kotlin
 * val client = ktorCioJsonHttpClientOf(
 *     timeouts = KtorClientTimeouts(requestTimeout = Duration.ofSeconds(5))
 * )
 * ```
 *
 * @param json JSON configuration for request/response serialization
 * @param timeouts client-level timeout defaults
 * @param block optional [HttpClientConfig] DSL block for CIO-specific configuration
 * @return a new [HttpClient] backed by the CIO engine
 */
fun ktorCioJsonHttpClientOf(
    json: Json = defaultKtorClientJson,
    timeouts: KtorClientTimeouts = KtorClientTimeouts(),
    block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {},
): HttpClient =
    ktorJsonHttpClientOf(
        engineFactory = CIO,
        json = json,
        timeouts = timeouts,
        block = block,
    )
