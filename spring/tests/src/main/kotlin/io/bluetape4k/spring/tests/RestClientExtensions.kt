package io.bluetape4k.spring.tests

import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

/**
 * GET 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
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
        }
        .retrieve()

/**
 * POST 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
 *
 * @param uri 요청 URI
 * @param publisher 요청 바디 Publisher
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
inline fun <reified T: Any> RestClient.httpPost(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            accept?.let { accept(it) }
        }
        .body(publisher)
        .retrieve()

/**
 * POST 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
 *
 * @param uri 요청 URI
 * @param flow 요청 바디 Flow
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
inline fun <reified T: Any> RestClient.httpPost(
    uri: String,
    flow: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            accept?.let { accept(it) }
        }
        .body(flow)
        .retrieve()

/**
 * PUT 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
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
        }
        .retrieve()

/**
 * PUT 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
 *
 * @param uri 요청 URI
 * @param publisher 요청 바디 Publisher
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
inline fun <reified T: Any> RestClient.httpPut(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            accept?.let { accept(it) }
        }
        .body(publisher)
        .retrieve()

/**
 * PUT 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
 *
 * @param uri 요청 URI
 * @param flow 요청 바디 Flow
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 */
inline fun <reified T: Any> RestClient.httpPut(
    uri: String,
    flow: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            accept?.let { accept(it) }
        }
        .body(flow)
        .retrieve()

/**
 * PATCH 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
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
        }
        .retrieve()

/**
 * DELETE 요청을 전송하고 [RestClient.ResponseSpec]를 반환합니다.
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
