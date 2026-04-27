package io.bluetape4k.http.hc5.classic

import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.classic.MinimalHttpClient
import org.apache.hc.client5.http.io.HttpClientConnectionManager

/**
 * 최소한의 [MinimalHttpClient] 를 생성합니다.
 *
 * ```kotlin
 * val cm = httpClientConnectionManager {
 *     setMaxConnPerRoute(5)
 *     setMaxConnTotal(5)
 * }
 * val httpClient = minimalHttpClientOf(cm)
 * ```
 *
 * @param connManager [HttpClientConnectionManager] 인스턴스
 * @return [MinimalHttpClient]
 */
fun minimalHttpClientOf(
    // MinimalHttpClient 은 setConnectionManagerShared() API 미지원 →
    // 호출마다 독립 CM 생성하여 소유권(lifecycle)을 client 가 갖도록 함
    connManager: HttpClientConnectionManager = httpClientConnectionManager {},
): MinimalHttpClient =
    HttpClients.createMinimal(connManager)
