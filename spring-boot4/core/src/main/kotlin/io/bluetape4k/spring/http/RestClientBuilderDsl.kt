package io.bluetape4k.spring.http

import org.springframework.web.client.RestClient

/**
 * DSL 방식으로 [RestClient]를 생성합니다.
 *
 * ```kotlin
 * val client = restClientOf("https://api.example.com") {
 *     defaultHeader("Authorization", "Bearer $token")
 * }
 * ```
 *
 * @param baseUrl 기본 URL
 * @param configure [RestClient.Builder] 설정 블록
 */
fun restClientOf(
    baseUrl: String,
    configure: RestClient.Builder.() -> Unit = {},
): RestClient =
    RestClient
        .builder()
        .baseUrl(baseUrl)
        .apply(configure)
        .build()
