package io.bluetape4k.http.ktor

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig

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
