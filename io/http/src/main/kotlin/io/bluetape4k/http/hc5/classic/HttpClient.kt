package io.bluetape4k.http.hc5.classic

import io.bluetape4k.http.hc5.http.defaultKeepAliveStrategy
import io.bluetape4k.http.hc5.http.defaultRetryStrategy
import io.bluetape4k.http.hc5.http.productionRequestConfigOf
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy
import org.apache.hc.client5.http.HttpRequestRetryStrategy
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.io.HttpClientConnectionManager
import org.apache.hc.core5.util.TimeValue

/**
 * Builds a [CloseableHttpClient] via DSL.
 *
 * ```kotlin
 * val cm = httpClientConnectionManager {
 *     setMaxConnPerRoute(5)
 *     setMaxConnTotal(5)
 * }
 * val httpClient = httpClient { setConnectionManager(cm) }
 * ```
 *
 * @param builder [HttpClientBuilder] configuration block
 * @return [CloseableHttpClient]
 */
inline fun httpClient(
    builder: HttpClientBuilder.() -> Unit,
): CloseableHttpClient =
    HttpClientBuilder.create().apply(builder).build()

/**
 * Creates a default [CloseableHttpClient].
 *
 * @return [CloseableHttpClient]
 */
fun defaultHttpClient(): CloseableHttpClient = HttpClients.createDefault()

/**
 * Creates a [CloseableHttpClient] with default settings.
 *
 * ```kotlin
 * val client = httpClientOf()
 * val response = client.execute(HttpGet("https://example.com"))
 * // response.code == 200
 * ```
 *
 * @return [CloseableHttpClient]
 */
fun httpClientOf(): CloseableHttpClient = HttpClients.createDefault()

/**
 * Creates a [CloseableHttpClient] with the given [connectionManager].
 *
 * ```kotlin
 * val cm = httpClientConnectionManager {
 *     setMaxConnPerRoute(5)
 *     setMaxConnTotal(5)
 * }
 * val httpClient = httpClientOf(cm) {
 *    setDefaultRequestConfig(requestConfig)
 * }
 * ```
 *
 * @param connectionManager [HttpClientConnectionManager] instance
 * @param builder [HttpClientBuilder] configuration block
 * @return [CloseableHttpClient]
 */
inline fun httpClientOf(
    connectionManager: HttpClientConnectionManager,
    builder: HttpClientBuilder.() -> Unit = {},
): CloseableHttpClient = httpClient {
    setConnectionManager(connectionManager)
    builder()
}

/**
 * Creates a system-property-aware [CloseableHttpClient] (respects `http.proxyHost`, etc.).
 *
 * @return [CloseableHttpClient]
 */
fun systemHttpClientOf(): CloseableHttpClient = HttpClients.createSystem()

/**
 * Creates a production-ready [CloseableHttpClient] with all recommended tuning defaults applied:
 * pooled connections, eviction of expired/idle connections, keep-alive fallback, retry strategy,
 * and conservative request timeouts.
 *
 * ## Defaults
 * - Connection pool: 200 total / 100 per route
 * - Evicts expired connections automatically
 * - Evicts connections idle longer than [maxIdleTime] (default: 60 s)
 * - Keep-alive fallback: 60 s when server omits `Keep-Alive` header
 * - Retry: up to 3 times with 1 s interval on transient failures
 * - Request timeouts: pool-wait 5 s, connect 10 s, response 30 s
 *
 * ```kotlin
 * // All defaults
 * val client = productionHttpClientOf()
 *
 * // Custom pool size + longer response timeout
 * val client = productionHttpClientOf(
 *     maxConnTotal = 500,
 *     maxConnPerRoute = 200,
 *     requestConfig = productionRequestConfigOf(responseTimeout = Timeout.ofSeconds(60)),
 * )
 * ```
 *
 * @param maxConnTotal total maximum pooled connections (default: 200)
 * @param maxConnPerRoute maximum pooled connections per route (default: 100)
 * @param requestConfig request timeout config (default: [productionRequestConfigOf])
 * @param keepAliveStrategy keep-alive fallback strategy (default: [defaultKeepAliveStrategy])
 * @param retryStrategy retry strategy (default: [defaultRetryStrategy])
 * @param maxIdleTime maximum idle time before a pooled connection is evicted (default: 60 s)
 * @param builder optional [HttpClientBuilder] customisation applied last
 * @return production-tuned [CloseableHttpClient]
 */
fun productionHttpClientOf(
    maxConnTotal: Int = 200,
    maxConnPerRoute: Int = 100,
    requestConfig: RequestConfig = productionRequestConfigOf(),
    keepAliveStrategy: ConnectionKeepAliveStrategy = defaultKeepAliveStrategy(),
    retryStrategy: HttpRequestRetryStrategy = defaultRetryStrategy(),
    maxIdleTime: TimeValue = TimeValue.ofSeconds(60),
    builder: HttpClientBuilder.() -> Unit = {},
): CloseableHttpClient {
    val connManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(maxConnTotal)
        .setMaxConnPerRoute(maxConnPerRoute)
        .build()
    return httpClient {
        setConnectionManager(connManager)
        setDefaultRequestConfig(requestConfig)
        setKeepAliveStrategy(keepAliveStrategy)
        setRetryStrategy(retryStrategy)
        evictExpiredConnections()
        evictIdleConnections(maxIdleTime)
        builder()
    }
}
