package io.bluetape4k.spring4.tests

import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body

/**
 * GET 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - `uri`를 그대로 사용해 `GET` 요청을 생성합니다.
 * - [accept]가 `null`이면 `Accept` 헤더를 설정하지 않습니다.
 * - 실제 HTTP 호출/예외는 이후 body 추출(`awaitBody` 등) 시점에 발생합니다.
 *
 * ```kotlin
 * val body = client.httpGet("/get").awaitBody<String>()
 * // body.contains("/get") == true
 * ```
 *
 * @param uri 요청 URI
 * @param accept 수신할 미디어 타입
 */
fun WebClient.httpGet(
    uri: String,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    get()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()

/**
 * HEAD 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - `HEAD` 요청을 전송하며 응답 본문 없이 상태/헤더 검증에 사용합니다.
 * - [accept]가 있으면 `Accept` 헤더를 설정합니다.
 *
 * ```kotlin
 * val status = client.httpHead("/get").toBodilessEntity().statusCode
 * // status.is2xxSuccessful == true
 * ```
 *
 * @param uri 요청 URI
 * @param accept 수신할 미디어 타입
 */
fun WebClient.httpHead(
    uri: String,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    head()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()

/**
 * POST 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [value]가 `null`이 아니면 `bodyValue`로 요청 바디를 설정합니다.
 * - [contentType], [accept]는 `null`이 아닐 때만 헤더로 반영됩니다.
 *
 * ```kotlin
 * val body = client.httpPost("/post", "hello").awaitBody<String>()
 * // body.contains("hello") == true
 * ```
 *
 * @param uri 요청 URI
 * @param value 요청 바디
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
fun WebClient.httpPost(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
            accept?.let { accept(it) }
        }.retrieve()

/**
 * POST 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [publisher]를 request body로 스트리밍합니다.
 * - 제네릭 [T]는 `body(publisher)` element type 추론에 사용됩니다.
 *
 * ```kotlin
 * val body = client.httpPost("/post", Flux.just("a", "b")).awaitBody<String>()
 * // body.contains("/post") == true
 * ```
 *
 * @param uri 요청 URI
 * @param publisher 요청 바디 Publisher
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
inline fun <reified T: Any> WebClient.httpPost(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            accept?.let { accept(it) }
        }.body(publisher)
        .retrieve()

/**
 * POST 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [flow]를 request body로 전송합니다.
 * - [contentType], [accept]는 제공된 경우에만 설정됩니다.
 *
 * ```kotlin
 * val body = client.httpPost("/post", flowOf("a", "b")).awaitBody<String>()
 * // body.contains("/post") == true
 * ```
 *
 * @param uri 요청 URI
 * @param flow 요청 바디 Flow
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
inline fun <reified T: Any> WebClient.httpPost(
    uri: String,
    flow: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            accept?.let { accept(it) }
        }.body(flow)
        .retrieve()

/**
 * PUT 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [value]가 있을 때만 바디를 설정합니다.
 * - 요청 전송/오류는 응답 소비 시점에 평가됩니다.
 *
 * ```kotlin
 * val body = client.httpPut("/put", "hello").awaitBody<String>()
 * // body.contains("hello") == true
 * ```
 *
 * @param uri 요청 URI
 * @param value 요청 바디
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
fun WebClient.httpPut(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
            accept?.let { accept(it) }
        }.retrieve()

/**
 * PUT 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [publisher]를 PUT 바디로 전달합니다.
 * - 요청 헤더는 `null`이 아닌 인자만 반영합니다.
 *
 * ```kotlin
 * val body = client.httpPut("/put", Flux.just("x")).awaitBody<String>()
 * // body.contains("/put") == true
 * ```
 *
 * @param uri 요청 URI
 * @param publisher 요청 바디 Publisher
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
inline fun <reified T: Any> WebClient.httpPut(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            accept?.let { accept(it) }
        }.body(publisher)
        .retrieve()

/**
 * PUT 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [flow]를 바디로 전달합니다.
 * - 반환 [WebClient.ResponseSpec]은 이후 체이닝(`onStatus`, `awaitBody`)에 사용됩니다.
 *
 * ```kotlin
 * val body = client.httpPut("/put", flowOf("x")).awaitBody<String>()
 * // body.contains("/put") == true
 * ```
 *
 * @param uri 요청 URI
 * @param flow 요청 바디 Flow
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
inline fun <reified T: Any> WebClient.httpPut(
    uri: String,
    flow: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            accept?.let { accept(it) }
        }.body(flow)
        .retrieve()

/**
 * PATCH 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [value]가 있을 때만 PATCH 바디를 설정합니다.
 * - [contentType], [accept]는 선택적으로 적용됩니다.
 *
 * ```kotlin
 * val body = client.httpPatch("/patch", "hello").awaitBody<String>()
 * // body.contains("hello") == true
 * ```
 *
 * @param uri 요청 URI
 * @param value 요청 바디
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
fun WebClient.httpPatch(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    patch()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
            accept?.let { accept(it) }
        }.retrieve()

/**
 * DELETE 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - 바디 없이 `DELETE` 요청을 전송합니다.
 * - [accept]가 제공되면 `Accept` 헤더를 설정합니다.
 *
 * ```kotlin
 * val body = client.httpDelete("/delete").awaitBody<String>()
 * // body.contains("/delete") == true
 * ```
 *
 * @param uri 요청 URI
 * @param accept 수신할 미디어 타입
 */
fun WebClient.httpDelete(
    uri: String,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    delete()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()

/**
 * OPTIONS 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - 대상 URI의 허용 메서드/옵션 확인용 요청입니다.
 * - [accept]가 있으면 `Accept` 헤더를 설정합니다.
 *
 * ```kotlin
 * val entity = client.httpOptions("/anything").toBodilessEntity()
 * // entity.statusCode.is2xxSuccessful == true
 * ```
 *
 * @param uri 요청 URI
 * @param accept 수신할 미디어 타입
 */
fun WebClient.httpOptions(
    uri: String,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    options()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()
