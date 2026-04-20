package io.bluetape4k.http.jdk

import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * [HttpClient.sendAsync] 를 코루틴 suspend 함수로 래핑합니다.
 *
 * ```kotlin
 * val response = client.sendAwait(request, HttpResponse.BodyHandlers.ofByteArray())
 * println(response.statusCode())
 * ```
 *
 * @param request [HttpRequest] 인스턴스
 * @param bodyHandler 응답 본문 핸들러
 * @return [HttpResponse] 인스턴스
 */
suspend fun <T> HttpClient.sendAwait(
    request: HttpRequest,
    bodyHandler: HttpResponse.BodyHandler<T>,
): HttpResponse<T> = sendAsync(request, bodyHandler).await()

/**
 * HTTP GET 요청을 코루틴으로 전송하고 byte array 응답을 반환합니다.
 *
 * ```kotlin
 * val response = client.getAwait("https://example.com")
 * println(response.statusCode())
 * ```
 *
 * @param uri 요청 URI
 * @return byte array 응답을 담은 [HttpResponse]
 */
suspend fun HttpClient.getAwait(uri: String): HttpResponse<ByteArray> {
    val request = HttpRequest.newBuilder(URI.create(uri)).GET().build()
    return sendAwait(request, HttpResponse.BodyHandlers.ofByteArray())
}

/**
 * HTTP GET 요청을 코루틴으로 전송하고 String 응답을 반환합니다.
 *
 * ```kotlin
 * val response = client.getStringAwait("https://example.com")
 * println(response.body())
 * ```
 *
 * @param uri 요청 URI
 * @return String 응답을 담은 [HttpResponse]
 */
suspend fun HttpClient.getStringAwait(uri: String): HttpResponse<String> {
    val request = HttpRequest.newBuilder(URI.create(uri)).GET().build()
    return sendAwait(request, HttpResponse.BodyHandlers.ofString())
}
