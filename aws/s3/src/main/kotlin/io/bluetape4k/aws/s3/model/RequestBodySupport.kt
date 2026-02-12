@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireZeroOrPositiveNumber
import software.amazon.awssdk.core.sync.RequestBody
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Path

const val DEFAULT_MIME_TYPE: String = "application/octet-stream"

inline fun String.toRequestBody(charset: Charset = Charsets.UTF_8): RequestBody = RequestBody.fromString(this, charset)
inline fun ByteArray.toRequestBody(): RequestBody = RequestBody.fromBytes(this)
inline fun ByteBuffer.toRequestBody(): RequestBody = RequestBody.fromByteBuffer(this)
inline fun File.toRequestBody(): RequestBody = RequestBody.fromFile(this)
inline fun Path.toRequestBody(): RequestBody = RequestBody.fromFile(this)
inline fun InputStream.toRequestBody(contentLength: Long): RequestBody {
    contentLength.requireZeroOrPositiveNumber("contentLength")
    return RequestBody.fromInputStream(this, contentLength)
}

inline fun requestBodyOf(text: String, cs: Charset = Charsets.UTF_8): RequestBody = RequestBody.fromString(text, cs)
inline fun requestBodyOf(bytes: ByteArray): RequestBody = RequestBody.fromBytes(bytes)
inline fun requestBodyOf(byteBuffer: ByteBuffer): RequestBody = RequestBody.fromByteBuffer(byteBuffer)

inline fun requestBodyOf(file: File): RequestBody = RequestBody.fromFile(file)
inline fun requestBodyOf(path: Path): RequestBody = RequestBody.fromFile(path)

inline fun requestBodyOf(
    inputStream: InputStream,
    contentLength: Long,
): RequestBody {
    contentLength.requireZeroOrPositiveNumber("contentLength")
    return RequestBody.fromInputStream(inputStream, contentLength)
}

fun requestBodyOf(
    mimeType: String = DEFAULT_MIME_TYPE,
    contentProvider: () -> InputStream,
): RequestBody {
    return RequestBody.fromContentProvider(contentProvider, mimeType)
}
