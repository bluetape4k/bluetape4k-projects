package io.bluetape4k.feign.clients.vertx

import feign.Client
import io.bluetape4k.http.vertx.vertxHttpClientOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.vertx.core.http.HttpClient

/**
 * Vertx [HttpClient]을 동기방식의 HTTP 통신에 사용하는 Feign 용 [Client]
 */
class VertxHttpClient private constructor(
    private val vertxClient: HttpClient,
): feign.Client {

    companion object: KLoggingChannel() {
        @JvmStatic
        operator fun invoke(vertxClient: HttpClient = vertxHttpClientOf()): VertxHttpClient {
            return VertxHttpClient(vertxClient)
        }
    }

    override fun execute(
        feignRequest: feign.Request,
        feignOptions: feign.Request.Options,
    ): feign.Response {
        return vertxClient
            .sendAsync(feignRequest, feignOptions)
            .get(feignOptions.readTimeout(), feignOptions.readTimeoutUnit())
    }
}
