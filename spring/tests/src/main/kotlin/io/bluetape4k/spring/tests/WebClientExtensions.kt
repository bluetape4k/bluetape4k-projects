package io.bluetape4k.spring.tests

import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body

/**
 * GET 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
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
        }
        .retrieve()

/**
 * POST 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
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
        }
        .body(publisher)
        .retrieve()

/**
 * POST 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
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
        }
        .body(flow)
        .retrieve()

/**
 * PUT 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
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
        }
        .retrieve()

/**
 * PUT 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
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
        }
        .body(publisher)
        .retrieve()

/**
 * PUT 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
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
        }
        .body(flow)

        .retrieve()

/**
 * PATCH 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
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
        }
        .retrieve()

/**
 * DELETE 요청을 전송하고 [WebClient.ResponseSpec]를 반환합니다.
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
