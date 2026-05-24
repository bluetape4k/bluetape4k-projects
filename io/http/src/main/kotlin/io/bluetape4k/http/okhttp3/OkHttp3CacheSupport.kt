package io.bluetape4k.http.okhttp3

import io.bluetape4k.logging.debug
import io.bluetape4k.support.requirePositiveNumber
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.slf4j.Logger
import java.io.File
import java.io.Serializable

/**
 * Snapshot of OkHttp disk-cache hit/miss counters for a single observation point.
 *
 * @property requestCount total number of HTTP requests made since the cache was created
 * @property hitCount number of requests served from the cache
 * @property networkCount number of requests that required a network round-trip
 * @property hitRate fraction of requests served from the cache (`hitCount / requestCount`, or 0 when no requests)
 */
data class OkHttp3CacheMetrics(
    val requestCount: Int,
    val hitCount: Int,
    val networkCount: Int,
    val hitRate: Double,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Returns a snapshot of hit/miss counters for this [Cache].
 *
 * ```kotlin
 * val metrics = client.cache?.metrics()
 * log.debug { "cache hit rate: ${metrics?.hitRate}" }
 * ```
 */
fun Cache.metrics(): OkHttp3CacheMetrics {
    val requests = requestCount()
    val hits = hitCount()
    val network = networkCount()
    return OkHttp3CacheMetrics(
        requestCount = requests,
        hitCount = hits,
        networkCount = network,
        hitRate = if (requests == 0) 0.0 else hits.toDouble() / requests,
    )
}

/**
 * Logs current cache hit/miss counters at DEBUG level.
 *
 * ```kotlin
 * client.cache?.logMetrics(log, "my-service")
 * // [my-service] OkHttp cache: requests=100, hits=80, network=20, hitRate=0.80
 * ```
 *
 * @param logger SLF4J logger to write to
 * @param label optional label prepended to the log line (default: empty)
 */
fun Cache.logMetrics(logger: Logger, label: String = "") {
    val m = metrics()
    val prefix = if (label.isBlank()) "" else "[$label] "
    logger.debug {
        "${prefix}OkHttp cache: requests=${m.requestCount}, hits=${m.hitCount}, " +
                "network=${m.networkCount}, hitRate=${"%.2f".format(m.hitRate)}"
    }
}

/**
 * Creates an [OkHttpClient] with an OkHttp disk [Cache] pre-configured.
 *
 * ## Defaults
 * - `maxCacheMb` — 50 MB
 * - All other settings: inherit from [okhttp3ClientBuilderOf]
 *
 * ```kotlin
 * val client = okhttp3ClientWithCache(
 *     cacheDir = File("/var/cache/okhttp"),
 *     maxCacheMb = 100L,
 * )
 *
 * // Check metrics after use
 * client.cache?.logMetrics(log, "payment-api")
 * ```
 *
 * @param cacheDir directory for cache files
 * @param maxCacheMb maximum cache size in megabytes (default: 50 MB)
 * @param builder optional [OkHttpClient.Builder] customisation applied last
 * @return [OkHttpClient] with disk cache enabled
 */
fun okhttp3ClientWithCache(
    cacheDir: File,
    maxCacheMb: Long = 50L,
    builder: OkHttpClient.Builder.() -> Unit = {},
): OkHttpClient {
    maxCacheMb.requirePositiveNumber("maxCacheMb")
    val cache = Cache(cacheDir, maxCacheMb * 1024 * 1024)
    return okhttp3ClientBuilderOf {
        cache(cache)
        builder()
    }.build()
}
