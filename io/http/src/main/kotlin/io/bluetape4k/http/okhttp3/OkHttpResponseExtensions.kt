package io.bluetape4k.http.okhttp3

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import java.io.InputStream

private val log = KotlinLogging.logger { }

/**
 * Response Body를 [InputStream]으로 변환합니다.
 *
 * ```kotlin
 * val client = OkHttpClient()
 * val request = okhttp3.Request.Builder().url("https://example.com").build()
 * val response = client.newCall(request).execute()
 * val inputStream = response.bodyAsInputStream()
 * // inputStream != null
 * ```
 *
 * @return [InputStream] 또는 null
 */
fun okhttp3.Response?.bodyAsInputStream(): InputStream? = this?.body?.byteStream()

/**
 * Response Body를 [ByteArray]로 변환합니다.
 *
 * ```kotlin
 * val client = OkHttpClient()
 * val request = okhttp3.Request.Builder().url("https://example.com").build()
 * val response = client.newCall(request).execute()
 * val bytes = response.bodyAsByteArray()
 * // bytes != null && bytes.isNotEmpty()
 * ```
 *
 * @return [ByteArray] 또는 null
 */
fun okhttp3.Response?.bodyAsByteArray(): ByteArray? = this?.body?.bytes()

/**
 * Response Body를 [String]으로 변환합니다.
 *
 * ```kotlin
 * val client = OkHttpClient()
 * val request = okhttp3.Request.Builder().url("https://example.com").build()
 * val response = client.newCall(request).execute()
 * val body = response.bodyAsString()
 * // body != null
 * ```
 *
 * @return [String] 또는 null
 */
fun okhttp3.Response?.bodyAsString(): String? = this?.body?.string()

/**
 * [okhttp3.Response]의 핵심 정보를 DEBUG 레벨로 로깅합니다.
 *
 * ```kotlin
 * val client = OkHttpClient()
 * val request = okhttp3.Request.Builder().url("https://example.com").build()
 * val response = client.newCall(request).execute()
 * response.print(no = 1)
 * // DEBUG 로그: "Response[1]: 200 OK", "Headers[1]: ..."
 * ```
 *
 * @param no 로그에 표시할 응답 번호 (기본값: 1)
 */
fun okhttp3.Response.print(no: Int = 1) {
    log.debug { "Response[$no]: ${this.code} ${this.message}" }
    log.debug { "Headers[$no]: ${this.headers}" }
    log.debug { "Cache Response[$no]: ${this.cacheResponse}" }
    log.debug { "Network Response[$no]: ${this.networkResponse}" }
}

/**
 * [okhttp3.MediaType]을 `type/subtype` 문자열로 변환합니다.
 *
 * ```kotlin
 * val mediaType = "application/json".toMediaType()
 * val typeString = mediaType.toTypeString()
 * // "application/json"
 * ```
 *
 * @return `type/subtype` 형식의 문자열
 */
fun okhttp3.MediaType.toTypeString(): String = "${this.type}/${this.subtype}"
