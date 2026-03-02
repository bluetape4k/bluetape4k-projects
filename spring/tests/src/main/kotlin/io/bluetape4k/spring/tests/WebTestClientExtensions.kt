package io.bluetape4k.spring.tests

import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.body

/**
 * GET 요청을 전송하고 [WebTestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - 요청 후 즉시 `exchange()`를 호출해 검증 가능한 응답 스펙을 반환합니다.
 * - [httpStatus]가 지정되면 상태 코드를 즉시 검증합니다.
 *
 * ```kotlin
 * client.httpGet("/get", HttpStatus.OK)
 *     .expectBody().jsonPath("$.url").exists()
 * ```
 *
 * @param uri 요청 URI
 * @param httpStatus 기대하는 상태 코드
 * @param accept 수신할 미디어 타입
 */
fun WebTestClient.httpGet(
    uri: String,
    httpStatus: HttpStatus? = null,
    accept: MediaType? = null,
): WebTestClient.ResponseSpec =
    get()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .exchange()
        .apply {
            httpStatus?.let { expectStatus().isEqualTo(it) }
        }

/**
 * HEAD 요청을 전송하고 [WebTestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - `HEAD` 요청 결과를 상태/헤더 검증에 사용합니다.
 * - [httpStatus]가 있으면 `expectStatus().isEqualTo(...)`를 적용합니다.
 *
 * ```kotlin
 * client.httpHead("/get", HttpStatus.OK)
 *     .expectHeader().exists("Content-Type")
 * ```
 *
 * @param uri 요청 URI
 * @param httpStatus 기대하는 상태 코드
 * @param accept 수신할 미디어 타입
 */
fun WebTestClient.httpHead(
    uri: String,
    httpStatus: HttpStatus? = null,
    accept: MediaType? = null,
): WebTestClient.ResponseSpec =
    head()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .exchange()
        .apply {
            httpStatus?.let { expectStatus().isEqualTo(it) }
        }

/**
 * POST 요청을 전송하고 [WebTestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [value]가 `null`이 아니면 `bodyValue`로 전송합니다.
 * - [contentType], [accept]는 `null`이 아닌 경우만 헤더에 반영됩니다.
 *
 * ```kotlin
 * client.httpPost("/post", "Hello", HttpStatus.OK)
 *     .expectBody().jsonPath("$.data").isEqualTo("Hello")
 * ```
 *
 * @param uri 요청 URI
 * @param value 요청 바디
 * @param httpStatus 기대하는 상태 코드
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
fun WebTestClient.httpPost(
    uri: String,
    value: Any? = null,
    httpStatus: HttpStatus? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebTestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
            accept?.let { accept(it) }
        }
        .exchange()
        .apply {
            httpStatus?.let { expectStatus().isEqualTo(it) }
        }

/**
 * POST 요청을 전송하고 [WebTestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [publisher]를 바디로 전송합니다.
 * - [httpStatus]가 지정되면 응답 상태를 즉시 검증합니다.
 *
 * ```kotlin
 * client.httpPost("/post", Flux.just("a"), HttpStatus.OK)
 *     .expectBody().jsonPath("$.url").exists()
 * ```
 *
 * @param uri 요청 URI
 * @param publisher 요청 바디 Publisher
 * @param httpStatus 기대하는 상태 코드
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
inline fun <reified T: Any> WebTestClient.httpPost(
    uri: String,
    publisher: Publisher<T>,
    httpStatus: HttpStatus? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebTestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            accept?.let { accept(it) }
        }
        .body(publisher)
        .exchange()
        .apply {
            httpStatus?.let { expectStatus().isEqualTo(it) }
        }

/**
 * POST 요청을 전송하고 [WebTestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [flow]를 바디로 전송합니다.
 * - 이후 `expectBody` 체이닝으로 응답 본문 검증을 수행합니다.
 *
 * ```kotlin
 * client.httpPost("/post", flowOf("a"), HttpStatus.OK)
 *     .expectBody().jsonPath("$.url").exists()
 * ```
 *
 * @param uri 요청 URI
 * @param flow 요청 바디 Flow
 * @param httpStatus 기대하는 상태 코드
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
inline fun <reified T: Any> WebTestClient.httpPost(
    uri: String,
    flow: Flow<T>,
    httpStatus: HttpStatus? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebTestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            accept?.let { accept(it) }
        }
        .body(flow)
        .exchange()
        .apply {
            httpStatus?.let { expectStatus().isEqualTo(it) }
        }

/**
 * PUT 요청을 전송하고 [WebTestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [value]가 제공되면 PUT 바디로 반영합니다.
 * - 상태 검증은 [httpStatus]가 있을 때 즉시 수행됩니다.
 *
 * ```kotlin
 * client.httpPut("/put", "Hello", HttpStatus.OK)
 *     .expectBody().jsonPath("$.data").isEqualTo("Hello")
 * ```
 *
 * @param uri 요청 URI
 * @param value 요청 바디
 * @param httpStatus 기대하는 상태 코드
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
fun WebTestClient.httpPut(
    uri: String,
    value: Any? = null,
    httpStatus: HttpStatus? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebTestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
            accept?.let { accept(it) }
        }
        .exchange()
        .apply {
            httpStatus?.let { expectStatus().isEqualTo(it) }
        }

/**
 * PUT 요청을 전송하고 [WebTestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [publisher]를 PUT 바디로 사용합니다.
 * - 미디어 타입 헤더는 선택적으로 적용됩니다.
 *
 * ```kotlin
 * client.httpPut("/put", Flux.just("x"), HttpStatus.OK)
 *     .expectBody().jsonPath("$.url").exists()
 * ```
 *
 * @param uri 요청 URI
 * @param publisher 요청 바디 Publisher
 * @param httpStatus 기대하는 상태 코드
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
inline fun <reified T: Any> WebTestClient.httpPut(
    uri: String,
    publisher: Publisher<T>,
    httpStatus: HttpStatus? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebTestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            accept?.let { accept(it) }
        }
        .body(publisher)
        .exchange()
        .apply {
            httpStatus?.let { expectStatus().isEqualTo(it) }
        }

/**
 * PUT 요청을 전송하고 [WebTestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [flow]를 PUT 바디로 사용합니다.
 * - 반환된 스펙에서 상태/본문 검증을 이어서 수행합니다.
 *
 * ```kotlin
 * client.httpPut("/put", flowOf("x"), HttpStatus.OK)
 *     .expectBody().jsonPath("$.url").exists()
 * ```
 *
 * @param uri 요청 URI
 * @param flow 요청 바디 Flow
 * @param httpStatus 기대하는 상태 코드
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
inline fun <reified T: Any> WebTestClient.httpPut(
    uri: String,
    flow: Flow<T>,
    httpStatus: HttpStatus? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebTestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            accept?.let { accept(it) }
        }
        .body(flow)
        .exchange()
        .apply {
            httpStatus?.let { expectStatus().isEqualTo(it) }
        }


/**
 * PATCH 요청을 전송하고 [WebTestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - [value]가 있을 때만 PATCH 바디를 설정합니다.
 * - [httpStatus] 검증은 선택 적용입니다.
 *
 * ```kotlin
 * client.httpPatch("/patch", "Hello", HttpStatus.OK)
 *     .expectBody().jsonPath("$.data").isEqualTo("Hello")
 * ```
 *
 * @param uri 요청 URI
 * @param value 요청 바디
 * @param httpStatus 기대하는 상태 코드
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
fun WebTestClient.httpPatch(
    uri: String,
    value: Any? = null,
    httpStatus: HttpStatus? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebTestClient.ResponseSpec =
    patch()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
            accept?.let { accept(it) }
        }
        .exchange()
        .apply {
            httpStatus?.let { expectStatus().isEqualTo(it) }
        }


/**
 * DELETE 요청을 전송하고 [WebTestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - 바디 없이 삭제 요청을 전송합니다.
 * - [httpStatus]가 주어지면 즉시 상태를 검증합니다.
 *
 * ```kotlin
 * client.httpDelete("/delete", HttpStatus.OK)
 *     .expectBody().jsonPath("$.url").exists()
 * ```
 *
 * @param uri 요청 URI
 * @param httpStatus 기대하는 상태 코드
 * @param accept 수신할 미디어 타입
 */
fun WebTestClient.httpDelete(
    uri: String,
    httpStatus: HttpStatus? = null,
    accept: MediaType? = null,
): WebTestClient.ResponseSpec =
    delete()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .exchange()
        .apply {
            httpStatus?.let { expectStatus().isEqualTo(it) }
        }

/**
 * OPTIONS 요청을 전송하고 [WebTestClient.ResponseSpec]를 반환합니다.
 *
 * ## 동작/계약
 * - OPTIONS 요청 결과를 테스트 검증 체인으로 반환합니다.
 * - [httpStatus] 지정 시 상태 코드를 즉시 검증합니다.
 *
 * ```kotlin
 * client.httpOptions("/get", HttpStatus.OK)
 *     .expectHeader().exists("Allow")
 * ```
 *
 * @param uri 요청 URI
 * @param httpStatus 기대하는 상태 코드
 * @param accept 수신할 미디어 타입
 */
fun WebTestClient.httpOptions(
    uri: String,
    httpStatus: HttpStatus? = null,
    accept: MediaType? = null,
): WebTestClient.ResponseSpec =
    options()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .exchange()
        .apply {
            httpStatus?.let { expectStatus().isEqualTo(it) }
        }
