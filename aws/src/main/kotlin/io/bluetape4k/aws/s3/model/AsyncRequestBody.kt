@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.aws.s3.model

import org.reactivestreams.Publisher
import software.amazon.awssdk.core.async.AsyncRequestBody
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

/**
 * 문자열을 [AsyncRequestBody]로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * val result = "hello".toAsyncRequestBody()
 * // result.contentLength().orElse(-1L) == 5L
 * ```
 */
inline fun String.toAsyncRequestBody(charset: Charset = Charsets.UTF_8): AsyncRequestBody =
    AsyncRequestBody.fromString(this, charset)

/**
 * 바이트 배열을 [AsyncRequestBody]로 변환합니다.
 */
inline fun ByteArray.toAsyncRequestBody(): AsyncRequestBody = AsyncRequestBody.fromBytes(this)

/**
 * [ByteBuffer]를 [AsyncRequestBody]로 변환합니다.
 */
inline fun ByteBuffer.toAsyncRequestBody(): AsyncRequestBody = AsyncRequestBody.fromByteBuffer(this)

/**
 * 파일을 [AsyncRequestBody]로 변환합니다.
 */
inline fun File.toAsyncRequestBody(): AsyncRequestBody = AsyncRequestBody.fromFile(this)

/**
 * 경로의 파일을 [AsyncRequestBody]로 변환합니다.
 */
inline fun Path.toAsyncRequestBody(): AsyncRequestBody = AsyncRequestBody.fromFile(this)

/**
 * [InputStream]을 [AsyncRequestBody]로 변환합니다.
 *
 * [contentLength]는 0 이상이어야 합니다.
 */
inline fun InputStream.toAsyncRequestBody(
    contentLength: Long,
    executor: ExecutorService = ForkJoinPool.commonPool(),
): AsyncRequestBody {
    require(contentLength >= 0L) { "contentLength must be >= 0, but was $contentLength" }
    return AsyncRequestBody.fromInputStream(this, contentLength, executor)
}

/**
 * 문자열로 [AsyncRequestBody]를 생성합니다.
 */
inline fun asyncRequestBodyOf(text: String, cs: Charset = Charsets.UTF_8): AsyncRequestBody =
    AsyncRequestBody.fromString(text, cs)

/**
 * 바이트 배열로 [AsyncRequestBody]를 생성합니다.
 */
inline fun asyncRequestBodyOf(bytes: ByteArray): AsyncRequestBody = AsyncRequestBody.fromBytes(bytes)

/**
 * [ByteBuffer]로 [AsyncRequestBody]를 생성합니다.
 */
inline fun asyncRequestBodyOf(byteBuffer: ByteBuffer): AsyncRequestBody = AsyncRequestBody.fromByteBuffer(byteBuffer)

/**
 * 파일로 [AsyncRequestBody]를 생성합니다.
 */
inline fun asyncRequestBodyOf(file: File): AsyncRequestBody = AsyncRequestBody.fromFile(file)

/**
 * 경로의 파일로 [AsyncRequestBody]를 생성합니다.
 */
inline fun asyncRequestBodyOf(path: Path): AsyncRequestBody = AsyncRequestBody.fromFile(path)

/**
 * [InputStream]으로 [AsyncRequestBody]를 생성합니다.
 *
 * [contentLength]는 0 이상이어야 합니다.
 *
 * 예제:
 * ```kotlin
 * val stream = java.io.ByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
 * val result = asyncRequestBodyOf(stream, 4)
 * // result.contentLength().orElse(-1L) == 4L
 * ```
 */
inline fun asyncRequestBodyOf(
    inputStream: InputStream,
    contentLength: Long,
    executor: ExecutorService = ForkJoinPool.commonPool(),
): AsyncRequestBody {
    require(contentLength >= 0L) { "contentLength must be >= 0, but was $contentLength" }
    return AsyncRequestBody.fromInputStream(inputStream, contentLength, executor)
}

/**
 * [Publisher] 기반 [AsyncRequestBody]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val publisher = org.reactivestreams.FlowAdapters.toPublisher(
 *     java.util.concurrent.SubmissionPublisher<java.nio.ByteBuffer>().apply {
 *         submit(java.nio.ByteBuffer.wrap(byteArrayOf(1, 2, 3)))
 *         close()
 *     }
 * )
 * val result = asyncRequestBodyOf(publisher)
 * // result.contentLength().isPresent == false
 * ```
 */
inline fun asyncRequestBodyOf(
    contentPublisher: Publisher<ByteBuffer>,
): AsyncRequestBody =
    AsyncRequestBody.fromPublisher(contentPublisher)
