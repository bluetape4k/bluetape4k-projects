package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.core5.util.Timeout

/**
 * Creates a new [RequestConfig] via a DSL builder.
 *
 * ```kotlin
 * val requestConfig = requestConfig {
 *    setConnectionRequestTimeout(Timeout.ofSeconds(5))
 *    setConnectTimeout(Timeout.ofSeconds(10))
 *    setResponseTimeout(Timeout.ofSeconds(30))
 * }
 * ```
 *
 * @param builder [RequestConfig.Builder] configuration block
 * @return configured [RequestConfig] instance
 * @see RequestConfig.Builder
 */
inline fun requestConfig(
    builder: RequestConfig.Builder.() -> Unit,
): RequestConfig {
    return RequestConfig.custom().apply(builder).build()
}

/** Creates a default [RequestConfig] with no overrides applied. */
fun requestConfigOf(): RequestConfig = requestConfig {}

/**
 * Creates a production-ready [RequestConfig] with sensible timeout defaults for HTTP clients
 * running in server-side or service-mesh environments.
 *
 * ## Defaults
 * - `connectionRequestTimeout` — 5 s: time to wait for a connection from the pool before failing
 * - `connectTimeout`           — 10 s: time to establish a TCP connection
 * - `responseTimeout`          — 30 s: time to wait for the first response byte after the request is sent
 *
 * Request-scoped timeouts can be overridden via the corresponding parameters.
 *
 * ```kotlin
 * // Use defaults
 * val config = productionRequestConfigOf()
 *
 * // Override response timeout only
 * val config = productionRequestConfigOf(responseTimeout = Timeout.ofSeconds(60))
 * ```
 *
 * @param connectionRequestTimeout timeout for acquiring a connection from the pool (default: 5 s)
 * @param connectTimeout timeout for establishing a TCP connection (default: 10 s)
 * @param responseTimeout timeout for the first response byte (default: 30 s)
 * @return production-tuned [RequestConfig]
 */
@Suppress("DEPRECATION")
fun productionRequestConfigOf(
    connectionRequestTimeout: Timeout = Timeout.ofSeconds(5),
    connectTimeout: Timeout = Timeout.ofSeconds(10),
    responseTimeout: Timeout = Timeout.ofSeconds(30),
): RequestConfig = requestConfig {
    setConnectionRequestTimeout(connectionRequestTimeout)
    setConnectTimeout(connectTimeout)
    setResponseTimeout(responseTimeout)
}
