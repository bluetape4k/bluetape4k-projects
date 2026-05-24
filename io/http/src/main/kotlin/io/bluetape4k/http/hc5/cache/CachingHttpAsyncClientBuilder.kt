package io.bluetape4k.http.hc5.cache

import io.bluetape4k.support.requirePositiveNumber
import org.apache.hc.client5.http.cache.HttpAsyncCacheStorage
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.cache.CachingHttpAsyncClientBuilder
import org.apache.hc.client5.http.impl.cache.CachingHttpAsyncClients
import java.io.File

/**
 * 캐시를 지원하는 [CloseableHttpAsyncClient]를 생성합니다.
 *
 * ```kotlin
 * val client = cachingHttpAsyncClient {
 *     setCacheConfig(cacheConfig)
 *     setHttpCacheStorage(cacheStorage)
 * }
 * ```
 */
inline fun cachingHttpAsyncClient(
    builder: CachingHttpAsyncClientBuilder.() -> Unit,
): CloseableHttpAsyncClient {
    return CachingHttpAsyncClientBuilder.create().apply(builder).build()
}

/**
 * 캐시를 지원하는 [CloseableHttpAsyncClient]를 생성합니다.
 *
 * ```kotlin
 * val client = cachingHttpAsyncClient(cacheStorage) {
 *     setCacheConfig(cacheConfig)
 * }
 * ```
 */
inline fun cachingHttpAsyncClient(
    cacheStorage: HttpAsyncCacheStorage,
    builder: CachingHttpAsyncClientBuilder.() -> Unit = {},
): CloseableHttpAsyncClient {
    return CachingHttpAsyncClientBuilder.create()
        .setHttpCacheStorage(cacheStorage)
        .apply(builder)
        .build()
}

/**
 * 메모리에 캐시하는 [CloseableHttpAsyncClient]를 생성합니다.
 *
 * ```kotlin
 * val client = memoryCachingHttpAsyncClientOf()
 * client.start()
 * // HTTP 응답을 메모리에 캐시하는 클라이언트
 * ```
 *
 * @return [CloseableHttpAsyncClient] 인스턴스
 */
fun memoryCachingHttpAsyncClientOf(): CloseableHttpAsyncClient =
    CachingHttpAsyncClients.createMemoryBound()

/**
 * 파일에 캐시하는 [CloseableHttpAsyncClient]를 생성합니다.
 *
 * ```kotlin
 * val cacheDir = File("/tmp/http-cache")
 * val client = fileCachingHttpAsyncClientOf(cacheDir)
 * client.start()
 * // HTTP 응답을 파일에 캐시하는 클라이언트
 * ```
 *
 * @param cacheDir 캐시 파일을 저장할 디렉토리
 * @return [CloseableHttpAsyncClient] 인스턴스
 */
fun fileCachingHttpAsyncClientOf(cacheDir: File): CloseableHttpAsyncClient =
    CachingHttpAsyncClients.createFileBound(cacheDir)

/**
 * Creates an in-memory caching [CloseableHttpAsyncClient] with configurable limits.
 *
 * ## Defaults
 * - `maxEntries` — 1,000 entries
 * - `maxObjectSizeBytes` — 64 KB
 *
 * ```kotlin
 * val client = memoryCachingHttpAsyncClientOf(maxEntries = 500)
 * ```
 *
 * @param maxEntries maximum number of cache entries (default: 1_000)
 * @param maxObjectSizeBytes maximum cacheable response body size in bytes (default: 64 KB)
 * @param builder optional [CachingHttpAsyncClientBuilder] customisation applied last
 * @return in-memory caching [CloseableHttpAsyncClient] (call [CloseableHttpAsyncClient.start] before use)
 */
fun memoryCachingHttpAsyncClientOf(
    maxEntries: Int = 1_000,
    maxObjectSizeBytes: Long = 64 * 1024L,
    builder: CachingHttpAsyncClientBuilder.() -> Unit = {},
): CloseableHttpAsyncClient {
    val config = memoryCacheConfigOf(maxEntries, maxObjectSizeBytes)
    val storage = InMemoryHttpCacheStorage.createObjectCache(config)
    return CachingHttpAsyncClientBuilder.create()
        .setHttpCacheStorage(storage)
        .setCacheConfig(config)
        .apply(builder)
        .build()
}

/**
 * Creates a file-backed caching [CloseableHttpAsyncClient] with configurable limits.
 *
 * ## Defaults
 * - `maxCacheMb` — 100 MB
 * - `maxObjectSizeBytes` — 1 MB
 *
 * ```kotlin
 * val client = fileCachingHttpAsyncClientOf(File("/var/cache/http"), maxCacheMb = 200L)
 * ```
 *
 * @param cacheDir directory to store cache files
 * @param maxCacheMb maximum total cache size in megabytes (default: 100 MB)
 * @param maxObjectSizeBytes maximum cacheable response body size in bytes (default: 1 MB)
 * @param builder optional [CachingHttpAsyncClientBuilder] customisation applied last
 * @return file-backed caching [CloseableHttpAsyncClient] (call [CloseableHttpAsyncClient.start] before use)
 */
fun fileCachingHttpAsyncClientOf(
    cacheDir: File,
    maxCacheMb: Long = 100L,
    maxObjectSizeBytes: Long = 1024 * 1024L,
    builder: CachingHttpAsyncClientBuilder.() -> Unit = {},
): CloseableHttpAsyncClient {
    maxCacheMb.requirePositiveNumber("maxCacheMb")
    maxObjectSizeBytes.requirePositiveNumber("maxObjectSizeBytes")
    val config = fileCacheConfigOf(
        maxEntries = (maxCacheMb * 1024 * 1024 / maxObjectSizeBytes).toInt().coerceAtLeast(100),
        maxObjectSizeBytes = maxObjectSizeBytes,
    )
    return CachingHttpAsyncClientBuilder.create()
        .setCacheConfig(config)
        .setCacheDir(cacheDir)
        .apply(builder)
        .build()
}
