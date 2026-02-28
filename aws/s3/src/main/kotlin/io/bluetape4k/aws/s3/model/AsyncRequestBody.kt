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

inline fun String.toAsyncRequestBody(charset: Charset = Charsets.UTF_8): AsyncRequestBody =
    AsyncRequestBody.fromString(this, charset)

inline fun ByteArray.toAsyncRequestBody(): AsyncRequestBody = AsyncRequestBody.fromBytes(this)
inline fun ByteBuffer.toAsyncRequestBody(): AsyncRequestBody = AsyncRequestBody.fromByteBuffer(this)
inline fun File.toAsyncRequestBody(): AsyncRequestBody = AsyncRequestBody.fromFile(this)
inline fun Path.toAsyncRequestBody(): AsyncRequestBody = AsyncRequestBody.fromFile(this)
inline fun InputStream.toAsyncRequestBody(
    contentLength: Long,
    executor: ExecutorService = ForkJoinPool.commonPool(),
): AsyncRequestBody {
    require(contentLength >= 0L) { "contentLength must be >= 0, but was $contentLength" }
    return AsyncRequestBody.fromInputStream(this, contentLength, executor)
}

inline fun asyncRequestBodyOf(text: String, cs: Charset = Charsets.UTF_8): AsyncRequestBody =
    AsyncRequestBody.fromString(text, cs)

inline fun asyncRequestBodyOf(bytes: ByteArray): AsyncRequestBody = AsyncRequestBody.fromBytes(bytes)
inline fun asyncRequestBodyOf(byteBuffer: ByteBuffer): AsyncRequestBody = AsyncRequestBody.fromByteBuffer(byteBuffer)

inline fun asyncRequestBodyOf(file: File): AsyncRequestBody = AsyncRequestBody.fromFile(file)
inline fun asyncRequestBodyOf(path: Path): AsyncRequestBody = AsyncRequestBody.fromFile(path)

inline fun asyncRequestBodyOf(
    inputStream: InputStream,
    contentLength: Long,
    executor: ExecutorService = ForkJoinPool.commonPool(),
): AsyncRequestBody {
    require(contentLength >= 0L) { "contentLength must be >= 0, but was $contentLength" }
    return AsyncRequestBody.fromInputStream(inputStream, contentLength, executor)
}

inline fun asyncRequestBodyOf(
    contentPublisher: Publisher<ByteBuffer>,
): AsyncRequestBody =
    AsyncRequestBody.fromPublisher(contentPublisher)
