@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireZeroOrPositiveNumber
import software.amazon.awssdk.core.sync.RequestBody
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Path

/**
 * [requestBodyOf] 기본 mime type 입니다.
 */
const val DEFAULT_MIME_TYPE: String = "application/octet-stream"

/**
 * 문자열을 [RequestBody]로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * val result = "hello".toRequestBody()
 * // result.optionalContentLength().orElse(-1L) == 5L
 * ```
 */
inline fun String.toRequestBody(charset: Charset = Charsets.UTF_8): RequestBody =
    RequestBody.fromString(this, charset)

/**
 * 바이트 배열을 [RequestBody]로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * val result = byteArrayOf(1, 2, 3).toRequestBody()
 * // result.optionalContentLength().orElse(-1L) == 3L
 * ```
 */
inline fun ByteArray.toRequestBody(): RequestBody = RequestBody.fromBytes(this)

/**
 * [ByteBuffer]를 [RequestBody]로 변환합니다.
 */
inline fun ByteBuffer.toRequestBody(): RequestBody = RequestBody.fromByteBuffer(this)

/**
 * 파일을 [RequestBody]로 변환합니다.
 */
inline fun File.toRequestBody(): RequestBody = RequestBody.fromFile(this)

/**
 * 경로의 파일을 [RequestBody]로 변환합니다.
 */
inline fun Path.toRequestBody(): RequestBody = RequestBody.fromFile(this)

/**
 * [InputStream]을 [RequestBody]로 변환합니다.
 *
 * [contentLength]는 0 이상이어야 합니다.
 */
inline fun InputStream.toRequestBody(contentLength: Long): RequestBody {
    contentLength.requireZeroOrPositiveNumber("contentLength")
    return RequestBody.fromInputStream(this, contentLength)
}

/**
 * 문자열로 [RequestBody]를 생성합니다.
 */
inline fun requestBodyOf(text: String, charset: Charset = Charsets.UTF_8): RequestBody =
    RequestBody.fromString(text, charset)

/**
 * 바이트 배열로 [RequestBody]를 생성합니다.
 */
inline fun requestBodyOf(bytes: ByteArray): RequestBody = RequestBody.fromBytes(bytes)

/**
 * [ByteBuffer]로 [RequestBody]를 생성합니다.
 */
inline fun requestBodyOf(byteBuffer: ByteBuffer): RequestBody = RequestBody.fromByteBuffer(byteBuffer)

/**
 * 파일로 [RequestBody]를 생성합니다.
 */
inline fun requestBodyOf(file: File): RequestBody = RequestBody.fromFile(file)

/**
 * 경로의 파일로 [RequestBody]를 생성합니다.
 */
inline fun requestBodyOf(path: Path): RequestBody = RequestBody.fromFile(path)

/**
 * [InputStream]으로 [RequestBody]를 생성합니다.
 *
 * [contentLength]는 0 이상이어야 합니다.
 *
 * 예제:
 * ```kotlin
 * val stream = java.io.ByteArrayInputStream(byteArrayOf(10, 20, 30))
 * val result = requestBodyOf(stream, 3)
 * // result.optionalContentLength().orElse(-1L) == 3L
 * ```
 */
inline fun requestBodyOf(
    inputStream: InputStream,
    contentLength: Long,
): RequestBody {
    contentLength.requireZeroOrPositiveNumber("contentLength")
    return RequestBody.fromInputStream(inputStream, contentLength)
}

/**
 * content provider와 mime type으로 [RequestBody]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = requestBodyOf("text/plain") {
 *     java.io.ByteArrayInputStream("hello".toByteArray())
 * }
 * // result.contentType() == "text/plain"
 * ```
 */
fun requestBodyOf(
    mimeType: String = DEFAULT_MIME_TYPE,
    contentProvider: () -> InputStream,
): RequestBody =
    RequestBody.fromContentProvider(contentProvider, mimeType)
