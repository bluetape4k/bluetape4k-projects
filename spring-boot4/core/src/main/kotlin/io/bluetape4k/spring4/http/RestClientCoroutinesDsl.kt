package io.bluetape4k.spring4.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

/**
 * [RestClient]를 사용하여 suspend GET 요청을 수행하고 응답을 역직렬화합니다.
 *
 * ```kotlin
 * val user = client.suspendGet<User>("/users/1")
 * ```
 *
 * @param uri 요청 URI
 * @param accept 수신할 미디어 타입
 * @return 역직렬화된 응답 객체
 */
suspend inline fun <reified T: Any> RestClient.suspendGet(
    uri: String,
    accept: MediaType? = null,
): T =
    withContext(Dispatchers.IO) {
        val spec = get().uri(uri)
        if (accept != null) spec.accept(accept)
        spec.retrieve().body(T::class.java)!!
    }

/**
 * [RestClient]를 사용하여 suspend POST 요청을 수행하고 응답을 역직렬화합니다.
 *
 * ```kotlin
 * val created = client.suspendPost<User>("/users", newUser, MediaType.APPLICATION_JSON)
 * ```
 *
 * @param uri 요청 URI
 * @param body 요청 바디
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 * @return 역직렬화된 응답 객체
 */
suspend inline fun <reified T: Any> RestClient.suspendPost(
    uri: String,
    body: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): T =
    withContext(Dispatchers.IO) {
        val spec = post().uri(uri)
        if (contentType != null) spec.contentType(contentType)
        if (accept != null) spec.accept(accept)
        if (body != null) spec.body(body)
        spec.retrieve().body(T::class.java)!!
    }

/**
 * [RestClient]를 사용하여 suspend PUT 요청을 수행하고 응답을 역직렬화합니다.
 *
 * ```kotlin
 * val updated = client.suspendPut<User>("/users/1", updatedUser, MediaType.APPLICATION_JSON)
 * ```
 *
 * @param uri 요청 URI
 * @param body 요청 바디
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 * @return 역직렬화된 응답 객체
 */
suspend inline fun <reified T: Any> RestClient.suspendPut(
    uri: String,
    body: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): T =
    withContext(Dispatchers.IO) {
        val spec = put().uri(uri)
        if (contentType != null) spec.contentType(contentType)
        if (accept != null) spec.accept(accept)
        if (body != null) spec.body(body)
        spec.retrieve().body(T::class.java)!!
    }

/**
 * [RestClient]를 사용하여 suspend PATCH 요청을 수행하고 응답을 역직렬화합니다.
 *
 * ```kotlin
 * val patched = client.suspendPatch<User>("/users/1", patchData, MediaType.APPLICATION_JSON)
 * ```
 *
 * @param uri 요청 URI
 * @param body 요청 바디
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 * @return 역직렬화된 응답 객체
 */
suspend inline fun <reified T: Any> RestClient.suspendPatch(
    uri: String,
    body: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): T =
    withContext(Dispatchers.IO) {
        val spec = patch().uri(uri)
        if (contentType != null) spec.contentType(contentType)
        if (accept != null) spec.accept(accept)
        if (body != null) spec.body(body)
        spec.retrieve().body(T::class.java)!!
    }

/**
 * [RestClient]를 사용하여 suspend DELETE 요청을 수행합니다.
 *
 * ```kotlin
 * client.suspendDelete("/users/1")
 * ```
 *
 * @param uri 요청 URI
 * @param accept 수신할 미디어 타입
 */
suspend fun RestClient.suspendDelete(
    uri: String,
    accept: MediaType? = null,
): Unit =
    withContext(Dispatchers.IO) {
        val spec = delete().uri(uri)
        if (accept != null) spec.accept(accept)
        spec.retrieve().toBodilessEntity()
    }
