package io.bluetape4k.http.hc5.cache

import io.bluetape4k.support.requirePositiveNumber
import org.apache.hc.client5.http.impl.cache.CacheConfig

/**
 * Creates a [CacheConfig] via a DSL builder.
 *
 * ```kotlin
 * val config = cacheConfig {
 *     setMaxCacheEntries(500)
 *     setMaxObjectSize(32 * 1024)
 * }
 * ```
 *
 * @param builder [CacheConfig.Builder] configuration block
 * @return configured [CacheConfig]
 */
inline fun cacheConfig(
    builder: CacheConfig.Builder.() -> Unit,
): CacheConfig = CacheConfig.custom().apply(builder).build()

/**
 * Creates a [CacheConfig] suitable for in-memory caching.
 *
 * ## Defaults
 * - `maxCacheEntries` — 1,000 entries
 * - `maxObjectSizeBytes` — 64 KB
 *
 * ```kotlin
 * val config = memoryCacheConfigOf()
 * val storage = InMemoryHttpCacheStorage.createObjectCache(config)
 * val client = memoryCachingHttpClientOf(config = config)
 * ```
 *
 * @param maxEntries maximum number of cache entries (default: 1_000)
 * @param maxObjectSizeBytes maximum size of a single cacheable response body in bytes (default: 64 KB)
 * @return [CacheConfig] for in-memory use
 */
fun memoryCacheConfigOf(
    maxEntries: Int = 1_000,
    maxObjectSizeBytes: Long = 64 * 1024L,
): CacheConfig {
    maxEntries.requirePositiveNumber("maxEntries")
    maxObjectSizeBytes.requirePositiveNumber("maxObjectSizeBytes")
    return cacheConfig {
        setMaxCacheEntries(maxEntries)
        setMaxObjectSize(maxObjectSizeBytes)
    }
}

/**
 * Creates a [CacheConfig] suitable for file-backed caching.
 *
 * ## Defaults
 * - `maxCacheEntries` — 10,000 entries
 * - `maxObjectSizeBytes` — 1 MB
 *
 * ```kotlin
 * val config = fileCacheConfigOf()
 * val client = fileCachingHttpClientOf(cacheDir, config = config)
 * ```
 *
 * @param maxEntries maximum number of cache entries (default: 10_000)
 * @param maxObjectSizeBytes maximum size of a single cacheable response body in bytes (default: 1 MB)
 * @return [CacheConfig] for file-backed use
 */
fun fileCacheConfigOf(
    maxEntries: Int = 10_000,
    maxObjectSizeBytes: Long = 1024 * 1024L,
): CacheConfig {
    maxEntries.requirePositiveNumber("maxEntries")
    maxObjectSizeBytes.requirePositiveNumber("maxObjectSizeBytes")
    return cacheConfig {
        setMaxCacheEntries(maxEntries)
        setMaxObjectSize(maxObjectSizeBytes)
    }
}
