package io.bluetape4k.retrofit2.clients.vertx

import io.bluetape4k.http.okhttp3.okhttp3Response
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.retrofit2.toIOException
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.util.concurrent.CompletableFuture

private val log by lazy { KotlinLogging.logger {} }

/**
 * Retrofit2 연동 타입 변환을 위한 `toVertxHttpClientRequest` 함수를 제공합니다.
 */
internal fun okhttp3.Request.toVertxHttpClientRequest(request: HttpClientRequest): HttpClientRequest {
    val self = this@toVertxHttpClientRequest

    if (!request.headers().contains(HttpHeaders.ACCEPT)) {
        request.putHeader(HttpHeaders.ACCEPT, "*/*")
    }

    self.headers.forEach { (name, value) ->
        request.headers().add(name, value)
    }
    request.method = HttpMethod.valueOf(self.method)

    // Build Vertx HttpClientRequest Body
    self.body?.let { body: RequestBody ->
        if (body.contentLength() > 0) {
            val contentType = body.contentType()

            request.putHeader(HttpHeaders.CONTENT_TYPE, contentType.toString())
            request.putHeader(HttpHeaders.CONTENT_LENGTH, body.contentLength().toString())

            val buffer = Buffer()
            body.writeTo(buffer)
            request.write(io.vertx.core.buffer.Buffer.buffer(buffer.readByteArray()))
        } else {
            request.putHeader(HttpHeaders.CONTENT_LENGTH, "0")
        }
    } ?: run {
        request.putHeader(HttpHeaders.CONTENT_LENGTH, "0")
    }

    return request
}

/**
 * Retrofit2 연동 타입 변환을 위한 `toOkResponse` 함수를 제공합니다.
 */
internal fun io.vertx.core.http.HttpClientResponse.toOkResponse(
    okRequest: okhttp3.Request,
    promise: CompletableFuture<okhttp3.Response>,
) {
    val self: HttpClientResponse = this@toOkResponse

    body()
        .onSuccess { buffer ->
            log.trace { "Convert Vertx HttpClientResponse to okhttp3.Response. version=${self.version()}" }

            val response = okhttp3Response {
                protocol(Protocol.valueOf(self.version().name))
                request(okRequest)
                code(self.statusCode())
                message(self.statusMessage())

                log.trace { "protocol=${self.version().name}, code=${self.statusCode()}, message=${self.statusMessage()}" }

                self.headers().forEach { (key, value) ->
                    addHeader(key, value)
                }

                val contentEncodings = self.getHeader(HttpHeaders.CONTENT_ENCODING)
                    .orEmpty()
                    .split(',')
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .toSet()

                val bytes = when {
                    "gzip" in contentEncodings || "x-gzip" in contentEncodings ->
                        runCatching { Compressors.GZip.decompress(buffer.bytes) }
                            .getOrElse {
                                log.warn(it) { "Fail to decompress gzip response. fallback to raw bytes." }
                                buffer.bytes
                            }
                    "deflate" in contentEncodings                              ->
                        runCatching { Compressors.Deflate.decompress(buffer.bytes) }
                            .getOrElse {
                                log.warn(it) { "Fail to decompress deflate response. fallback to raw bytes." }
                                buffer.bytes
                            }
                    else                                                       -> buffer.bytes
                }

                val contentTypeHeader = self.getHeader(HttpHeaders.CONTENT_TYPE)
                val contentType = contentTypeHeader?.toMediaTypeOrNull()

                body(bytes.toResponseBody(contentType))
            }
            promise.complete(response)
        }
        .onFailure { error ->
            promise.completeExceptionally(error.toIOException())
        }
}
