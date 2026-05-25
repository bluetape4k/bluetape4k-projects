package io.bluetape4k.http.hc5.cache

import io.bluetape4k.support.requirePositiveNumber
import org.apache.hc.client5.http.cache.HttpCacheStorage
import org.apache.hc.client5.http.impl.cache.CachingHttpClientBuilder
import org.apache.hc.client5.http.impl.cache.CachingHttpClients
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import java.io.File

/**
 * Creates a caching [CloseableHttpClient] using the supplied builder customisation.
 *
 * ```kotlin
 * val client = cachingHttpClient {
 *     setCacheConfig(cacheConfig)
 *     setHttpCacheStorage(cacheStorage)
 * }
 * ```
 */
inline fun cachingHttpClient(
    builder: CachingHttpClientBuilder.() -> Unit,
): CloseableHttpClient =
    CachingHttpClientBuilder.create()
        .apply(builder)
        .build()

/**
 * Creates a caching [CloseableHttpClient] backed by the given [cacheStorage].
 *
 * ```kotlin
 * val client = cachingHttpClient(cacheStorage) {
 *     setCacheConfig(cacheConfig)
 * }
 * ```
 */
inline fun cachingHttpClient(
    cacheStorage: HttpCacheStorage,
    builder: CachingHttpClientBuilder.() -> Unit = {},
): CloseableHttpClient =
    CachingHttpClientBuilder.create()
        .setHttpCacheStorage(cacheStorage)
        .apply(builder)
        .build()

/**
 * Creates an in-memory caching [CloseableHttpClient].
 *
 * ```kotlin
 * val client = memoryCachingHttpClientOf()
 * val response = client.execute(HttpGet("https://example.com"))
 * // Client that caches HTTP responses in memory
 * ```
 *
 * @return in-memory caching [CloseableHttpClient]
 */
fun memoryCachingHttpClientOf(): CloseableHttpClient =
    cachingHttpClient(InMemoryHttpCacheStorage.createObjectCache())

/**
 * Creates a file-backed caching [CloseableHttpClient].
 *
 * ```kotlin
 * val cacheDir = File("/tmp/http-cache")
 * val client = fileCachingHttpClientOf(cacheDir)
 * val response = client.execute(HttpGet("https://example.com"))
 * // Client that caches HTTP responses on disk
 * ```
 *
 * @param cacheDir directory to store cache files
 * @return file-backed caching [CloseableHttpClient]
 */
fun fileCachingHttpClientOf(cacheDir: File): CloseableHttpClient =
    CachingHttpClients.createFileBound(cacheDir)

/**
 * Creates an in-memory caching [CloseableHttpClient] with configurable limits.
 *
 * ## Defaults
 * - `maxEntries` — 1,000 entries
 * - `maxObjectSizeBytes` — 64 KB
 *
 * ```kotlin
 * val client = memoryCachingHttpClientOf(maxEntries = 500, maxObjectSizeBytes = 32 * 1024L)
 * ```
 *
 * @param maxEntries maximum number of cache entries (default: 1_000)
 * @param maxObjectSizeBytes maximum cacheable response body size in bytes (default: 64 KB)
 * @param builder optional [CachingHttpClientBuilder] customisation applied last
 * @return in-memory caching [CloseableHttpClient]
 */
fun memoryCachingHttpClientOf(
    maxEntries: Int = 1_000,
    maxObjectSizeBytes: Long = 64 * 1024L,
    builder: CachingHttpClientBuilder.() -> Unit = {},
): CloseableHttpClient = cachingHttpClient(
    InMemoryHttpCacheStorage.createObjectCache(memoryCacheConfigOf(maxEntries, maxObjectSizeBytes)),
    builder,
)

/**
 * Creates a file-backed caching [CloseableHttpClient] with configurable limits.
 *
 * The cache files are stored under [cacheDir]. The directory is created if it does not exist.
 *
 * ## Defaults
 * - `maxCacheMb` — 100 MB
 * - `maxObjectSizeBytes` — 1 MB
 *
 * ```kotlin
 * val client = fileCachingHttpClientOf(File("/var/cache/http"), maxCacheMb = 200L)
 * ```
 *
 * @param cacheDir directory to store cache files
 * @param maxCacheMb maximum total cache size in megabytes (default: 100 MB)
 * @param maxObjectSizeBytes maximum cacheable response body size in bytes (default: 1 MB)
 * @param builder optional [CachingHttpClientBuilder] customisation applied last
 * @return file-backed caching [CloseableHttpClient]
 */
fun fileCachingHttpClientOf(
    cacheDir: File,
    maxCacheMb: Long = 100L,
    maxObjectSizeBytes: Long = 1024 * 1024L,
    builder: CachingHttpClientBuilder.() -> Unit = {},
): CloseableHttpClient {
    maxCacheMb.requirePositiveNumber("maxCacheMb")
    maxObjectSizeBytes.requirePositiveNumber("maxObjectSizeBytes")
    val config = fileCacheConfigOf(
        maxEntries = (maxCacheMb * 1024 * 1024 / maxObjectSizeBytes).toInt().coerceAtLeast(100),
        maxObjectSizeBytes = maxObjectSizeBytes,
    )
    return CachingHttpClientBuilder.create()
        .setCacheConfig(config)
        .setCacheDir(cacheDir)
        .apply(builder)
        .build()
}
