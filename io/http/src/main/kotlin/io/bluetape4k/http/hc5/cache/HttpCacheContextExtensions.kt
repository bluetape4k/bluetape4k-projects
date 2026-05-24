package io.bluetape4k.http.hc5.cache

import org.slf4j.Logger
import io.bluetape4k.logging.debug
import org.apache.hc.client5.http.cache.CacheResponseStatus
import org.apache.hc.client5.http.cache.HttpCacheContext

/**
 * Returns `true` when the response was served directly from the cache without network contact.
 */
val HttpCacheContext.isHit: Boolean
    get() = cacheResponseStatus == CacheResponseStatus.CACHE_HIT

/**
 * Returns `true` when the response required a full network round-trip (cache miss).
 */
val HttpCacheContext.isMiss: Boolean
    get() = cacheResponseStatus == CacheResponseStatus.CACHE_MISS

/**
 * Returns `true` when a cached response was revalidated with the origin server.
 */
val HttpCacheContext.isValidated: Boolean
    get() = cacheResponseStatus == CacheResponseStatus.VALIDATED

/**
 * Returns `true` when the cache module produced a synthetic response (e.g. 504 Gateway Timeout
 * when the origin is unreachable and no suitable cached entry was found).
 */
val HttpCacheContext.isCacheModuleResponse: Boolean
    get() = cacheResponseStatus == CacheResponseStatus.CACHE_MODULE_RESPONSE

/**
 * Returns a human-readable one-line description of the cache outcome for this request.
 *
 * ```kotlin
 * val context = HttpCacheContext.create()
 * client.execute(request, context) { response -> ... }
 * log.debug { context.cacheStatusDescription() }
 * // e.g. "CACHE_HIT" / "CACHE_MISS" / "VALIDATED" / "CACHE_MODULE_RESPONSE" / "unknown"
 * ```
 */
fun HttpCacheContext.cacheStatusDescription(): String =
    cacheResponseStatus?.name ?: "unknown"

/**
 * Logs the cache outcome for the current request at DEBUG level using the supplied [logger].
 *
 * ```kotlin
 * val context = HttpCacheContext.create()
 * client.execute(request, context) { it }
 * context.logCacheStatus(log)
 * ```
 *
 * @param logger the [KLogger] to write to
 * @param label optional label prepended to the log message (default: empty)
 */
fun HttpCacheContext.logCacheStatus(logger: Logger, label: String = "") {
    val prefix = if (label.isBlank()) "" else "[$label] "
    logger.debug { "${prefix}cache status=${cacheStatusDescription()}" }
}
