package io.bluetape4k.http.hc5.cache

import org.apache.hc.client5.http.cache.HttpCacheStorage
import org.apache.hc.client5.http.impl.cache.CachingHttpClientBuilder
import org.apache.hc.client5.http.impl.cache.CachingHttpClients
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import java.io.File

/**
 * 캐시를 지원하는 [CloseableHttpClient]를 생성합니다.
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
 * 캐시를 지원하는 [CloseableHttpClient]를 생성합니다.
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
 * 메모리에 캐시하는 [CloseableHttpClient]를 생성합니다.
 *
 * ```kotlin
 * val client = memoryCachingHttpClientOf()
 * val response = client.execute(HttpGet("https://example.com"))
 * // HTTP 응답을 메모리에 캐시하는 클라이언트
 * ```
 *
 * @return [CloseableHttpClient] 인스턴스
 */
fun memoryCachingHttpClientOf(): CloseableHttpClient =
    CachingHttpClients.createMemoryBound()

/**
 * 파일에 캐시하는 [CloseableHttpClient]를 생성합니다.
 *
 * ```kotlin
 * val cacheDir = File("/tmp/http-cache")
 * val client = fileCachingHttpClientOf(cacheDir)
 * val response = client.execute(HttpGet("https://example.com"))
 * // HTTP 응답을 파일에 캐시하는 클라이언트
 * ```
 *
 * @param cacheDir 캐시 파일을 저장할 디렉토리
 * @return [CloseableHttpClient] 인스턴스
 */
fun fileCachingHttpClientOf(cacheDir: File): CloseableHttpClient =
    CachingHttpClients.createFileBound(cacheDir)
