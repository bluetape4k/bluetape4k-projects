package io.bluetape4k.http.hc5.classic

import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.classic.MinimalHttpClient
import org.apache.hc.client5.http.io.HttpClientConnectionManager

/**
 * 최소한의 [MinimalHttpClient] 를 생성합니다.
 *
 * ```
 * val cm = httpClientConnectionManager {
 *     setMaxConnPerRoute(5)
 *     setMaxConnTotal(5)
 * }
 * val httpClient = minimalHttpClientOf(cm)
 * ```
 *
 * @param connManager [HttpClientConnectionManager]
 * @return [MinimalHttpClient]
 */
fun minimalHttpClientOf(
    connManager: HttpClientConnectionManager = defaultHttpClientConnectionManager,
): MinimalHttpClient =
    HttpClients.createMinimal(connManager)
