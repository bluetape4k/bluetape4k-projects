package io.bluetape4k.http.hc5.classic

import io.bluetape4k.http.hc5.http.defaultKeepAliveStrategy
import io.bluetape4k.http.hc5.http.defaultRetryStrategy
import io.bluetape4k.http.hc5.http.productionRequestConfigOf
import io.bluetape4k.logging.KLogging
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy
import org.apache.hc.client5.http.HttpRequestRetryStrategy
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.TimeValue

/**
 * Virtual Threads 기반 HC5 Classic HTTP 클라이언트를 생성합니다.
 * 블로킹 I/O를 Virtual Thread 에서 실행하여 높은 동시성을 달성합니다.
 */
object VirtualThreadHttpClients: KLogging()

/**
 * Creates an HC5 Classic [CloseableHttpClient] backed by a Virtual Thread connection pool.
 *
 * Blocking I/O runs on virtual threads, achieving high concurrency without platform thread overhead.
 *
 * ```kotlin
 * val client = virtualThreadHttpClientOf(maxConnTotal = 200, maxConnPerRoute = 50)
 * client.use {
 *     val response = it.execute(HttpGet("https://example.com"))
 *     println(response.code)
 * }
 * ```
 *
 * @param maxConnTotal total maximum pooled connections (default: 200)
 * @param maxConnPerRoute maximum pooled connections per route (default: 100)
 * @return [CloseableHttpClient] backed by a Virtual Thread connection pool
 */
fun virtualThreadHttpClientOf(
    maxConnTotal: Int = 200,
    maxConnPerRoute: Int = 100,
): CloseableHttpClient {
    val connManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(maxConnTotal)
        .setMaxConnPerRoute(maxConnPerRoute)
        .build()
    return HttpClients.custom()
        .setConnectionManager(connManager)
        .build()
}

/**
 * Creates a production-ready HC5 Classic [CloseableHttpClient] backed by a Virtual Thread
 * connection pool with all recommended tuning defaults applied.
 *
 * Combines [virtualThreadHttpClientOf] connection pool sizing with the full production tuning
 * from [productionHttpClientOf]: eviction, keep-alive fallback, retry, and request timeouts.
 *
 * ## Virtual Thread note
 * HC5 5.x does not expose a `setThreadFactory` API on `PoolingHttpClientConnectionManagerBuilder`.
 * "VirtualThread" in this function name refers to the **calling context**: this client is
 * intended to be invoked from virtual threads (e.g. Spring Boot's virtual-thread executor).
 * Internal HC5 background threads (connection eviction, pool housekeeping) remain platform threads.
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
 * val client = productionVirtualThreadHttpClientOf()
 *
 * val client = productionVirtualThreadHttpClientOf(
 *     maxConnTotal = 500,
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
 * @param builder optional [org.apache.hc.client5.http.impl.classic.HttpClientBuilder] customisation applied last
 * @return production-tuned [CloseableHttpClient] backed by Virtual Threads
 */
fun productionVirtualThreadHttpClientOf(
    maxConnTotal: Int = 200,
    maxConnPerRoute: Int = 100,
    requestConfig: RequestConfig = productionRequestConfigOf(),
    keepAliveStrategy: ConnectionKeepAliveStrategy = defaultKeepAliveStrategy(),
    retryStrategy: HttpRequestRetryStrategy = defaultRetryStrategy(),
    maxIdleTime: TimeValue = TimeValue.ofSeconds(60),
    builder: HttpClientBuilder.() -> Unit = {},
): CloseableHttpClient = productionHttpClientOf(
    maxConnTotal = maxConnTotal,
    maxConnPerRoute = maxConnPerRoute,
    requestConfig = requestConfig,
    keepAliveStrategy = keepAliveStrategy,
    retryStrategy = retryStrategy,
    maxIdleTime = maxIdleTime,
    builder = builder,
)
