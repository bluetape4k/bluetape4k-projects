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

fun String.toAsyncRequestBody(cs: Charset = Charsets.UTF_8): AsyncRequestBody = AsyncRequestBody.fromString(this, cs)
fun ByteArray.toAsyncRequestBody(): AsyncRequestBody = AsyncRequestBody.fromBytes(this)
fun ByteBuffer.toAsyncRequestBody(): AsyncRequestBody = AsyncRequestBody.fromByteBuffer(this)
fun File.toAsyncRequestBody(): AsyncRequestBody = AsyncRequestBody.fromFile(this)
fun Path.toAsyncRequestBody(): AsyncRequestBody = AsyncRequestBody.fromFile(this)
fun InputStream.toAsyncRequestBody(
    executor: ExecutorService = ForkJoinPool.commonPool(),
): AsyncRequestBody {
    return AsyncRequestBody.fromInputStream(this, this.available().toLong(), executor)
}

fun asyncRequestBodyOf(text: String, cs: Charset = Charsets.UTF_8): AsyncRequestBody =
    AsyncRequestBody.fromString(text, cs)

fun asyncRequestBodyOf(bytes: ByteArray): AsyncRequestBody = AsyncRequestBody.fromBytes(bytes)
fun asyncRequestBodyOf(byteBuffer: ByteBuffer): AsyncRequestBody = AsyncRequestBody.fromByteBuffer(byteBuffer)

fun asyncRequestBodyOf(file: File): AsyncRequestBody = AsyncRequestBody.fromFile(file)
fun asyncRequestBodyOf(path: Path): AsyncRequestBody = AsyncRequestBody.fromFile(path)

fun asyncRequestBodyOf(
    inputStream: InputStream,
    contentLength: Long = inputStream.available().toLong(),
    executor: ExecutorService = ForkJoinPool.commonPool(),
): AsyncRequestBody {
    return AsyncRequestBody.fromInputStream(inputStream, contentLength, executor)
}

fun asyncRequestBodyOf(
    contentPublisher: Publisher<ByteBuffer>,
): AsyncRequestBody {
    return AsyncRequestBody.fromPublisher(contentPublisher)
}
