package io.bluetape4k.spring4.http

import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

/**
 * GET 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [accept]가 지정되면 `Accept` 헤더를 설정합니다.
 * - 응답 본문/예외 평가는 `body()`, `toEntity()` 호출 시 수행됩니다.
 *
 * ```kotlin
 * val body = client.httpGet("/get").body<String>()
 * // body?.contains("/get") == true
 * ```
 *
 * @param uri 요청 URI
 * @param accept 수신할 미디어 타입
 */
fun RestClient.httpGet(
    uri: String,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    get()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()

/**
 * HEAD 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - `HEAD` 요청으로 상태/헤더 검증에 사용합니다.
 * - 본문은 일반적으로 비어 있습니다.
 *
 * ```kotlin
 * val status = client.httpHead("/get").toBodilessEntity().statusCode
 * // status.is2xxSuccessful == true
 * ```
 *
 * @param uri 요청 URI
 * @param accept 수신할 미디어 타입
 */
fun RestClient.httpHead(
    uri: String,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    head()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()

/**
 * POST 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [value]가 `null`이 아닐 때만 요청 바디를 설정합니다.
 * - [contentType], [accept]는 제공된 경우에만 반영됩니다.
 *
 * ```kotlin
 * val body = client.httpPost("/post", "hello").body<String>()
 * // body?.contains("hello") == true
 * ```
 *
 * @param uri 요청 URI
 * @param value 요청 바디
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
fun RestClient.httpPost(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { body(it) }
            accept?.let { accept(it) }
        }.retrieve()

/**
 * PUT 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [value]가 있으면 PUT 바디로 설정합니다.
 * - 반환값은 후속 응답 디코딩 호출에 사용됩니다.
 *
 * ```kotlin
 * val body = client.httpPut("/put", "hello").body<String>()
 * // body?.contains("hello") == true
 * ```
 *
 * @param uri 요청 URI
 * @param value 요청 바디
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
fun RestClient.httpPut(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { body(it) }
            accept?.let { accept(it) }
        }.retrieve()

/**
 * PATCH 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [value]가 있을 때만 바디를 설정합니다.
 * - 헤더 설정 규칙은 POST/PUT과 동일합니다.
 *
 * ```kotlin
 * val body = client.httpPatch("/patch", "hello").body<String>()
 * // body?.contains("hello") == true
 * ```
 *
 * @param uri 요청 URI
 * @param value 요청 바디
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
fun RestClient.httpPatch(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    patch()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { body(it) }
            accept?.let { accept(it) }
        }.retrieve()

/**
 * DELETE 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - 바디 없는 `DELETE` 요청을 전송합니다.
 * - [accept]가 주어지면 응답 미디어 타입을 지정합니다.
 *
 * ```kotlin
 * val body = client.httpDelete("/delete").body<String>()
 * // body?.contains("/delete") == true
 * ```
 *
 * @param uri 요청 URI
 * @param accept 수신할 미디어 타입
 */
fun RestClient.httpDelete(
    uri: String,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    delete()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()

/**
 * OPTIONS 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - 대상 URI의 OPTIONS 응답을 조회합니다.
 * - [accept]가 있으면 `Accept` 헤더를 설정합니다.
 *
 * ```kotlin
 * val entity = client.httpOptions("/get").toBodilessEntity()
 * // entity.statusCode.is2xxSuccessful == true
 * ```
 *
 * @param uri 요청 URI
 * @param accept 수신할 미디어 타입
 */
fun RestClient.httpOptions(
    uri: String,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    options()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()
