package io.bluetape4k.http.hc5.routing

import org.apache.hc.client5.http.SchemePortResolver
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver
import org.apache.hc.client5.http.routing.RoutingSupport
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.HttpRequest

/**
 * HTTP 요청에서 대상 호스트를 결정합니다.
 *
 * ```kotlin
 * val request = basicHttpRequest("GET") {
 *     setUri("https://example.com/api/v1")
 * }
 * val host = request.determineHost()
 * // host.hostName == "example.com"
 * ```
 *
 * @return 결정된 [HttpHost]
 */
fun HttpRequest.determineHost(): HttpHost = RoutingSupport.determineHost(this)

/**
 * 대상 호스트를 정규화합니다.
 *
 * ```kotlin
 * val host = HttpHost("http", "example.com", -1)
 * val normalized = host.normalize()
 * // normalized.port == 80 (기본 포트로 정규화)
 * ```
 *
 * @param schemePortResolver 스킴별 포트 해석기
 * @return 정규화된 [HttpHost]
 */
fun HttpHost.normalize(
    schemePortResolver: SchemePortResolver = DefaultSchemePortResolver.INSTANCE,
): HttpHost =
    RoutingSupport.normalize(this, schemePortResolver)
