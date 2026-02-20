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
