package io.bluetape4k.feign.clients.vertx

import feign.Request
import feign.Request.Options
import io.bluetape4k.feign.feignResponseBuilder
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.support.isNullOrEmpty
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.RequestOptions
import java.util.concurrent.CompletableFuture

private val log by lazy { KotlinLogging.logger { } }

/**
 * Feign 연동에서 `requestOptions` 함수를 제공합니다.
 */
internal inline fun requestOptions(intializer: RequestOptions.() -> Unit): RequestOptions {
    return RequestOptions().apply(intializer)
}

/**
 * Feign 연동 타입 변환을 위한 `toVertxRequestOptions` 함수를 제공합니다.
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
 * Feign 연동에서 `parseFromFeignRequest` 함수를 제공합니다.
 */
internal fun HttpClientRequest.parseFromFeignRequest(feignRequest: feign.Request) {
    headers().add("accept", "*/*")
    headers().add("user-agent", "VertxHttpClient/4.4")
    headers().add("accept-encoding", "gzip,deflate")

    feignRequest.headers().forEach { (name, values) ->
        headers().add(name, values)
    }

    if (feignRequest.body().isNullOrEmpty()) {
        headers().add("content-length", "0")
    } else {
        write(io.vertx.core.buffer.Buffer.buffer(feignRequest.body()))
    }
}

/**
 * Feign 연동에서 `convertToFeignResponse` 함수를 제공합니다.
 */
internal fun HttpClientResponse.convertToFeignResponse(
    feignRequest: feign.Request,
    responsePromise: CompletableFuture<feign.Response>,
) {
    val self = this
    body()
        .onSuccess { buffer ->
            log.trace { "Convert Vertx HttpClientResponse to Feign Response." }

            val headers = with(self.headers()) {
                names().map { it.lowercase() }.associateWith { getAll(it) }
            }.toMap()

            val builder = feignResponseBuilder {
                protocolVersion(Request.ProtocolVersion.valueOf(self.version().name))
                request(feignRequest)
                status(self.statusCode())
                reason(self.statusMessage())
                headers(headers)

                val contentEncoding = headers["content-encoding"]?.firstOrNull().orEmpty()
                val bytes = when (contentEncoding) {
                    "gzip"    -> Compressors.GZip.decompress(buffer.bytes)
                    "deflate" -> Compressors.Deflate.decompress(buffer.bytes)
                    else      -> buffer.bytes
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

/**
 * Vertx [HttpClient]를 이용하여 Feign 의 [Request] 를 Async/Non-Blocking 방식으로 요청하고 응답을 받습니다.
 *
 * @param feignRequest [feign.Request] 인스턴스
 * @param feignOptions [feign.Request.Options] 인스턴스
 * @return [feign.Response]를 담은 [CompletableFuture] 인스턴스
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

            log.trace { "Send vertx httpclient request ..." }
            vertxRequest.send()
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
