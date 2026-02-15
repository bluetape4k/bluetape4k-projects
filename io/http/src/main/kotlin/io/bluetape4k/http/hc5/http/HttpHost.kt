package io.bluetape4k.http.hc5.http

import org.apache.hc.core5.http.HttpHost
import java.net.URI

/**
 * [URI]를 [HttpHost]로 변환합니다.
 *
 * ```
 * val httpHost = URI("http://localhost:8080").toHttpHost()
 * ```
 *
 * @receiver [URI]
 * @return 변환된 [HttpHost]
 */
fun URI.toHttpHost(): HttpHost = HttpHost.create(this)

/**
 * URL 문자열을 [HttpHost]로 변환합니다.
 *
 * ```
 * val httpHost = httpHostOf("http://localhost:8080")
 * ```
 *
 * @param url URL 문자열
 * @return 변환된 [HttpHost]
 */
fun httpHostOf(url: String): HttpHost = HttpHost.create(url)
