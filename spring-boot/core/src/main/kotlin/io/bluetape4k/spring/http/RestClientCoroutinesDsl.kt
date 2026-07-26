package io.bluetape4k.spring.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

/**
 * Performs a suspend GET request with [RestClient] and deserializes a non-null response body.
 *
 * ## Contract
 * - Blocking RestClient I/O runs on [Dispatchers.IO].
 * - Empty response bodies fail with [IllegalStateException] that includes the method, URI, and target type.
 * - Use [suspendGetOrNull] when an empty body is a valid response.
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
    runInterruptible(Dispatchers.IO) {
        val spec = get().uri(uri)
        if (accept != null) spec.accept(accept)
        requireRestClientBody(spec.retrieve().body(T::class.java), "GET", uri)
    }

/**
 * Performs a suspend POST request with [RestClient] and deserializes a non-null response body.
 *
 * ## Contract
 * - Blocking RestClient I/O runs on [Dispatchers.IO].
 * - Empty response bodies fail with [IllegalStateException] that includes the method, URI, and target type.
 * - Use [suspendPostOrNull] when an empty body is a valid response.
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
    runInterruptible(Dispatchers.IO) {
        val spec = post().uri(uri)
        if (contentType != null) spec.contentType(contentType)
        if (accept != null) spec.accept(accept)
        if (body != null) spec.body(body)
        requireRestClientBody(spec.retrieve().body(T::class.java), "POST", uri)
    }

/**
 * Performs a suspend PUT request with [RestClient] and deserializes a non-null response body.
 *
 * ## Contract
 * - Blocking RestClient I/O runs on [Dispatchers.IO].
 * - Empty response bodies fail with [IllegalStateException] that includes the method, URI, and target type.
 * - Use [suspendPutOrNull] when an empty body is a valid response.
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
    runInterruptible(Dispatchers.IO) {
        val spec = put().uri(uri)
        if (contentType != null) spec.contentType(contentType)
        if (accept != null) spec.accept(accept)
        if (body != null) spec.body(body)
        requireRestClientBody(spec.retrieve().body(T::class.java), "PUT", uri)
    }

/**
 * Performs a suspend PATCH request with [RestClient] and deserializes a non-null response body.
 *
 * ## Contract
 * - Blocking RestClient I/O runs on [Dispatchers.IO].
 * - Empty response bodies fail with [IllegalStateException] that includes the method, URI, and target type.
 * - Use [suspendPatchOrNull] when an empty body is a valid response.
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
    runInterruptible(Dispatchers.IO) {
        val spec = patch().uri(uri)
        if (contentType != null) spec.contentType(contentType)
        if (accept != null) spec.accept(accept)
        if (body != null) spec.body(body)
        requireRestClientBody(spec.retrieve().body(T::class.java), "PATCH", uri)
    }

/**
 * [RestClient]를 사용하여 suspend GET 요청을 수행하고 응답이 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val user: User? = client.suspendGetOrNull("/users/1")
 * ```
 *
 * @param uri 요청 URI
 * @param accept 수신할 미디어 타입
 * @return 역직렬화된 응답 객체 또는 `null`
 */
suspend inline fun <reified T: Any> RestClient.suspendGetOrNull(
    uri: String,
    accept: MediaType? = null,
): T? =
    runInterruptible(Dispatchers.IO) {
        val spec = get().uri(uri)
        if (accept != null) spec.accept(accept)
        spec.retrieve().body(T::class.java)
    }

/**
 * [RestClient]를 사용하여 suspend POST 요청을 수행하고 응답이 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val created: User? = client.suspendPostOrNull("/users", newUser)
 * ```
 *
 * @param uri 요청 URI
 * @param body 요청 바디
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 * @return 역직렬화된 응답 객체 또는 `null`
 */
suspend inline fun <reified T: Any> RestClient.suspendPostOrNull(
    uri: String,
    body: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): T? =
    runInterruptible(Dispatchers.IO) {
        val spec = post().uri(uri)
        if (contentType != null) spec.contentType(contentType)
        if (accept != null) spec.accept(accept)
        if (body != null) spec.body(body)
        spec.retrieve().body(T::class.java)
    }

/**
 * [RestClient]를 사용하여 suspend PUT 요청을 수행하고 응답이 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val updated: User? = client.suspendPutOrNull("/users/1", updatedUser, MediaType.APPLICATION_JSON)
 * ```
 *
 * @param uri 요청 URI
 * @param body 요청 바디
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 * @return 역직렬화된 응답 객체 또는 `null`
 */
suspend inline fun <reified T: Any> RestClient.suspendPutOrNull(
    uri: String,
    body: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): T? =
    runInterruptible(Dispatchers.IO) {
        val spec = put().uri(uri)
        if (contentType != null) spec.contentType(contentType)
        if (accept != null) spec.accept(accept)
        if (body != null) spec.body(body)
        spec.retrieve().body(T::class.java)
    }

/**
 * [RestClient]를 사용하여 suspend PATCH 요청을 수행하고 응답이 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val patched: User? = client.suspendPatchOrNull("/users/1", patchData, MediaType.APPLICATION_JSON)
 * ```
 *
 * @param uri 요청 URI
 * @param body 요청 바디
 * @param contentType 요청 바디 타입
 * @param accept 수신할 미디어 타입
 * @return 역직렬화된 응답 객체 또는 `null`
 */
suspend inline fun <reified T: Any> RestClient.suspendPatchOrNull(
    uri: String,
    body: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): T? =
    runInterruptible(Dispatchers.IO) {
        val spec = patch().uri(uri)
        if (contentType != null) spec.contentType(contentType)
        if (accept != null) spec.accept(accept)
        if (body != null) spec.body(body)
        spec.retrieve().body(T::class.java)
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
    runInterruptible(Dispatchers.IO) {
        val spec = delete().uri(uri)
        if (accept != null) spec.accept(accept)
        spec.retrieve().toBodilessEntity()
    }

@PublishedApi
internal inline fun <reified T: Any> requireRestClientBody(
    body: T?,
    method: String,
    uri: String,
): T =
    body ?: throw IllegalStateException(
        "RestClient $method $uri returned an empty response body for ${T::class.java.name}. " +
                "Use the corresponding OrNull coroutine helper when an empty body is valid."
    )
