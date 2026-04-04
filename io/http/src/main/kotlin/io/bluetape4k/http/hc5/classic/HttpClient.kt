package io.bluetape4k.http.hc5.classic

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.io.HttpClientConnectionManager

/**
 * Apache HttpComponent 5 의 [CloseableHttpClient] 를 빌드합니다.
 *
 * ```kotlin
 * val cm = httpClientConnectionManager {
 *      setMaxConnPerRoute(5)
 *      setMaxConnTotal(5)
 * }
 * val httpClient = httpClient { setConnectionManager(cm) }
 * ```
 *
 * @param builder [HttpClientBuilder] 초기화 람다
 * @return [CloseableHttpClient]
 * @see HttpClientBuilder
 */
inline fun httpClient(
    builder: HttpClientBuilder.() -> Unit,
): CloseableHttpClient =
    HttpClientBuilder.create().apply(builder).build()

/**
 * 기본 [CloseableHttpClient] 를 생성합니다.
 *
 * @return [CloseableHttpClient]
 */
fun defaultHttpClient(): CloseableHttpClient = HttpClients.createDefault()

/**
 * 기본 설정으로 [CloseableHttpClient]를 생성합니다.
 *
 * ```kotlin
 * val client = httpClientOf()
 * val response = client.execute(HttpGet("https://example.com"))
 * // response.code == 200
 * ```
 *
 * @return [CloseableHttpClient]
 */
fun httpClientOf(): CloseableHttpClient = HttpClients.createDefault()

/**
 * [HttpClientConnectionManager] 를 사용하는 [CloseableHttpClient] 를 생성합니다.
 *
 * ```kotlin
 * val cm = httpClientConnectionManager {
 *     setMaxConnPerRoute(5)
 *     setMaxConnTotal(5)
 * }
 * val httpClient = httpClientOf(cm) {
 *    setDefaultRequestConfig(requestConfig)
 *    setDefaultCredentialsProvider(credentialsProvider)
 * }
 * ```
 *
 * @param connectionManager [HttpClientConnectionManager] 인스턴스
 * @param builder [HttpClientBuilder] 초기화 람다
 * @return [CloseableHttpClient]
 */
inline fun httpClientOf(
    connectionManager: HttpClientConnectionManager,
    builder: HttpClientBuilder.() -> Unit = {},
): CloseableHttpClient = httpClient {
    setConnectionManager(connectionManager)
    builder()
}

/**
 * 시스템 기본 [CloseableHttpClient] 를 생성합니다.
 *
 * ```kotlin
 * val client = systemHttpClientOf()
 * val response = client.execute(HttpGet("https://example.com"))
 * // 시스템 속성(http.proxyHost 등)이 적용된 클라이언트
 * ```
 *
 * @return [CloseableHttpClient]
 */
fun systemHttpClientOf(): CloseableHttpClient = HttpClients.createSystem()
