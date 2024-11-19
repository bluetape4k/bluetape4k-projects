package io.bluetape4k.http.hc5.cache

import org.apache.hc.client5.http.cache.HttpAsyncCacheStorage
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.cache.CachingHttpAsyncClientBuilder
import org.apache.hc.client5.http.impl.cache.CachingHttpAsyncClients
import java.io.File

/**
 * Cache를 지원하는 [CloseableHttpAsyncClient]를 생성합니다.
 *
 * ```
 * val client = cachingHttpAsyncClient {
 *     setCacheConfig(cacheConfig)
 *     setHttpCacheStorage(cacheStorage)
 * }
 * ```
 */
inline fun cachingHttpAsyncClient(
    initializer: CachingHttpAsyncClientBuilder.() -> Unit,
): CloseableHttpAsyncClient {
    return CachingHttpAsyncClientBuilder.create().apply(initializer).build()
}

/**
 * Cache를 지원하는 [CloseableHttpAsyncClient]를 생성합니다.
 *
 * ```
 * val client = cachingHttpAsyncClient(cacheStorage) {
 *     setCacheConfig(cacheConfig)
 * }
 * ```
 */
inline fun cachingHttpAsyncClient(
    cacheStorage: HttpAsyncCacheStorage,
    initializer: CachingHttpAsyncClientBuilder.() -> Unit = {},
): CloseableHttpAsyncClient {
    return CachingHttpAsyncClientBuilder.create()
        .setHttpCacheStorage(cacheStorage)
        .apply(initializer)
        .build()
}

/**
 * 메모리에 캐시하는 [CloseableHttpAsyncClient]를 생성합니다.
 */
fun memoryCachingHttpAsyncClientOf(): CloseableHttpAsyncClient =
    CachingHttpAsyncClients.createMemoryBound()

/**
 * 파일에 캐시하는 [CloseableHttpAsyncClient]를 생성합니다.
 */
fun fileCachingHttpAsyncClientOf(cacheDir: File): CloseableHttpAsyncClient =
    CachingHttpAsyncClients.createFileBound(cacheDir)
