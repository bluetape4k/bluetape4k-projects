package io.bluetape4k.http.hc5.cache

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
