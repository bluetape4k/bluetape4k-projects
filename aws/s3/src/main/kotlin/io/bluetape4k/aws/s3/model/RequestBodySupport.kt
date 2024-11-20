package io.bluetape4k.aws.s3.model

import software.amazon.awssdk.core.internal.util.Mimetype
import software.amazon.awssdk.core.sync.RequestBody
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Path

fun String.toRequestBody(cs: Charset = Charsets.UTF_8): RequestBody = RequestBody.fromString(this, cs)
fun ByteArray.toRequestBody(): RequestBody = RequestBody.fromBytes(this)
fun ByteBuffer.toRequestBody(): RequestBody = RequestBody.fromByteBuffer(this)
fun File.toRequestBody(): RequestBody = RequestBody.fromFile(this)
fun Path.toRequestBody(): RequestBody = RequestBody.fromFile(this)
fun InputStream.toRequestBody(): RequestBody {
    return RequestBody.fromInputStream(this, this.available().toLong())
}

fun requestBodyOf(text: String, cs: Charset = Charsets.UTF_8): RequestBody = RequestBody.fromString(text, cs)
fun requestBodyOf(bytes: ByteArray): RequestBody = RequestBody.fromBytes(bytes)
fun requestBodyOf(byteBuffer: ByteBuffer): RequestBody = RequestBody.fromByteBuffer(byteBuffer)

fun requestBodyOf(file: File): RequestBody = RequestBody.fromFile(file)
fun requestBodyOf(path: Path): RequestBody = RequestBody.fromFile(path)

fun requestBodyOf(
    inputStream: InputStream,
    contentLength: Long = inputStream.available().toLong(),
): RequestBody {
    return RequestBody.fromInputStream(inputStream, contentLength)
}

fun requestBodyOf(
    mimeType: String = Mimetype.MIMETYPE_OCTET_STREAM,
    contentProvider: () -> InputStream,
): RequestBody {
    return RequestBody.fromContentProvider(contentProvider, mimeType)
}
