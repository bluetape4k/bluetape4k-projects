package io.bluetape4k.http.hc5.classic

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.io.HttpClientConnectionManager

/**
 * Apache HttpComponent 5 의 [CloseableHttpClient] 를 빌드합니다.
 *
 * ```
 * val cm = httpClientConnectionManager {
 *      setMaxConnPerRoute(5)
 *      setMaxConnTotal(5)
 * }
 * val httpClient = httpClient { setConnectionManager(cm) }
 * ```
 *
 * @param initializer [HttpClientBuilder] 를 초기화하는 람다 함수
 * @return [CloseableHttpClient]
 * @see HttpClientBuilder
 */
inline fun httpClient(
    initializer: HttpClientBuilder.() -> Unit,
): CloseableHttpClient {
    return HttpClientBuilder.create().apply(initializer).build()
}

/**
 * 기본 [CloseableHttpClient] 를 생성합니다.
 *
 * @return [CloseableHttpClient]
 */
fun defaultHttpClient(): CloseableHttpClient = HttpClients.createDefault()

/**
 * [HttpClientConnectionManager] 를 사용하는 기본 [CloseableHttpClient] 를 생성합니다.
 *
 * @return [CloseableHttpClient]
 */
fun httpClientOf(): CloseableHttpClient = HttpClients.createDefault()

/**
 * [HttpClientConnectionManager] 를 사용하는 [CloseableHttpClient] 를 생성합니다.
 *
 * ```
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
 * @param connectionManager [HttpClientConnectionManager]
 * @param initializer [HttpClientBuilder] 를 초기화하는 람다 함수
 * @return [CloseableHttpClient]
 */
fun httpClientOf(
    connectionManager: HttpClientConnectionManager,
    initializer: HttpClientBuilder.() -> Unit = {},
): CloseableHttpClient = httpClient {
    setConnectionManager(connectionManager)
    initializer()
}

/**
 * 시스템 기본 [CloseableHttpClient] 를 생성합니다.
 *
 * @return [CloseableHttpClient]
 */
fun systemHttpClientOf(): CloseableHttpClient = HttpClients.createSystem()
