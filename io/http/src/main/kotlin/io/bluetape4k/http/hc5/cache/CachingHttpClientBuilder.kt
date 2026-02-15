package io.bluetape4k.http.hc5.cache

import org.apache.hc.client5.http.cache.HttpCacheStorage
import org.apache.hc.client5.http.impl.cache.CachingHttpClientBuilder
import org.apache.hc.client5.http.impl.cache.CachingHttpClients
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import java.io.File

/**
 * 캐시를 지원하는 [CloseableHttpClient]를 생성합니다.
 *
 * ```
 * val client = cachingHttpClient {
 *     setCacheConfig(cacheConfig)
 *     setHttpCacheStorage(cacheStorage)
 * }
 * ```
 */
inline fun cachingHttpClient(
    @BuilderInference builder: CachingHttpClientBuilder.() -> Unit,
): CloseableHttpClient =
    CachingHttpClientBuilder.create()
        .apply(builder)
        .build()

/**
 * 캐시를 지원하는 [CloseableHttpClient]를 생성합니다.
 *
 * ```
 * val client = cachingHttpClient(cacheStorage) {
 *     setCacheConfig(cacheConfig)
 * }
 * ```
 */
inline fun cachingHttpClient(
    cacheStorage: HttpCacheStorage,
    @BuilderInference builder: CachingHttpClientBuilder.() -> Unit = {},
): CloseableHttpClient =
    CachingHttpClientBuilder.create()
        .setHttpCacheStorage(cacheStorage)
        .apply(builder)
        .build()

/**
 * 메모리에 캐시하는 [CloseableHttpClient]를 생성합니다.
 */
fun memoryCachingHttpClientOf(): CloseableHttpClient =
    CachingHttpClients.createMemoryBound()

/**
 * 파일에 캐시하는 [CloseableHttpClient]를 생성합니다.
 */
fun fileCachingHttpClientOf(cacheDir: File): CloseableHttpClient =
    CachingHttpClients.createFileBound(cacheDir)
