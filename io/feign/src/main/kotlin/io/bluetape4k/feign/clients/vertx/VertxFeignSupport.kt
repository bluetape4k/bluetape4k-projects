package io.bluetape4k.feign.clients.vertx

import feign.Request
import feign.Request.Options
import io.bluetape4k.feign.feignResponseBuilder
import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.support.isNullOrEmpty
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.RequestOptions
import java.util.concurrent.CompletableFuture

private val log by lazy { KotlinLogging.logger { } }

/**
 * [RequestOptions]를 생성하고 초기화 블록을 적용합니다.
 */
internal inline fun requestOptions(initializer: RequestOptions.() -> Unit): RequestOptions {
    return RequestOptions().apply(initializer)
}

/**
 * Feign [Options]를 Vert.x [RequestOptions]로 변환합니다.
 */
internal fun Options.toVertxRequestOptions(feignRequest: feign.Request): RequestOptions {
    val self = this
    return requestOptions {
        followRedirects = self.isFollowRedirects
        timeout = self.readTimeoutMillis().toLong()
        method = HttpMethod.valueOf(feignRequest.httpMethod().name)
        setAbsoluteURI(feignRequest.url())
    }
}

/**
 * Feign 요청 정보를 Vert.x 요청 객체에 반영합니다.
 */
internal fun HttpClientRequest.parseFromFeignRequest(feignRequest: feign.Request) {
    if (!headers().contains("accept")) {
        headers().set("accept", "*/*")
    }
    if (!headers().contains("user-agent")) {
        headers().set("user-agent", "VertxHttpClient/4.4")
    }
    if (!headers().contains("accept-encoding")) {
        headers().set("accept-encoding", "gzip,deflate")
    }

    feignRequest.headers().forEach { (name, values) ->
        headers().add(name, values)
    }

    if (feignRequest.body().isNullOrEmpty() && !headers().contains("content-length")) {
        headers().set("content-length", "0")
    }
}

/**
 * Vert.x 응답을 Feign 응답으로 변환합니다.
 */
internal fun HttpClientResponse.convertToFeignResponse(
    feignRequest: feign.Request,
    responsePromise: CompletableFuture<feign.Response>,
) {
    val self = this
    body()
        .onSuccess { buffer ->
            log.trace { "Convert Vertx HttpClientResponse to Feign Response." }

            val responseHeaders = self.headers()
            val headers = responseHeaders
                .names()
                .associate { headerName ->
                    headerName.lowercase() to responseHeaders.getAll(headerName)
                }

            val builder = feignResponseBuilder {
                protocolVersion(Request.ProtocolVersion.valueOf(self.version().name))
                request(feignRequest)
                status(self.statusCode())
                reason(self.statusMessage())
                headers(headers)

                val contentEncodings = headers["content-encoding"]
                    .orEmpty()
                    .asSequence()
                    .flatMap { it.split(',').asSequence() }
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .toSet()

                val bytes = when {
                    "gzip" in contentEncodings || "x-gzip" in contentEncodings ->
                        decompress(Compressors.GZip, buffer)
                    "deflate" in contentEncodings                              ->
                        decompress(Compressors.Deflate, buffer)
                    else                                                       ->
                        buffer.bytes
                }
                body(bytes)
            }
            responsePromise.complete(builder.build())
        }
        .onFailure { error ->
            log.warn(error) { "Fail to retrieve body." }
            responsePromise.completeExceptionally(error)
        }
}

internal fun decompress(compressor: Compressor, buffer: Buffer) =
    runCatching {
        compressor.decompress(buffer.bytes)
    }.getOrElse { error ->
        log.warn(error) { "Fail to decompress response. fallback to raw bytes." }
        buffer.bytes
    }

/**
 * Vert.x [HttpClient]로 Feign [Request]를 비동기 전송하고 [feign.Response]를 반환합니다.
 *
 * @param feignRequest Feign 요청 객체
 * @param feignOptions Feign 요청 옵션
 * @return Feign 응답을 담은 [CompletableFuture]
 */
internal fun HttpClient.sendAsync(
    feignRequest: feign.Request,
    feignOptions: feign.Request.Options,
): CompletableFuture<feign.Response> {

    val promise = CompletableFuture<feign.Response>()
    val options = feignOptions.toVertxRequestOptions(feignRequest)

    this.request(options)
        .onSuccess { request ->
            val vertxRequest = request.apply { parseFromFeignRequest(feignRequest) }
            val requestBody = feignRequest.body()

            log.trace { "Send vertx httpclient request ..." }

            val sendAction = if (requestBody.isNullOrEmpty()) {
                vertxRequest.send()
            } else {
                vertxRequest.send(Buffer.buffer(requestBody))
            }
            sendAction
                .onSuccess { response ->
                    log.trace { "Build feign response ... " }
                    response.convertToFeignResponse(feignRequest, promise)
                }
                .onFailure { error ->
                    log.error(error) { "Fail to send vertx httpclient request." }
                    promise.completeExceptionally(error)
                }
        }
        .onFailure { error ->
            log.error(error) { "Fail to build vertx httpclient request." }
            promise.completeExceptionally(error)
        }

    return promise
}
