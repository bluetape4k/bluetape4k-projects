package io.bluetape4k.feign.clients.vertx

import feign.AsyncClient
import feign.Request
import feign.Response
import io.bluetape4k.http.vertx.vertxHttpClientOf
import io.bluetape4k.logging.KLogging
import io.vertx.core.http.HttpClient
import java.util.*
import java.util.concurrent.CompletableFuture

class AsyncVertxHttpClient private constructor(
    private val vertxClient: HttpClient,
): AsyncClient<Any>, AutoCloseable {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(vertxClient: HttpClient = vertxHttpClientOf()): AsyncVertxHttpClient {
            return AsyncVertxHttpClient(vertxClient)
        }
    }

    override fun execute(
        feignRequest: feign.Request,
        feignOptions: Request.Options,
        requestContext: Optional<Any>,
    ): CompletableFuture<Response> {
        return vertxClient.sendAsync(feignRequest, feignOptions)
    }

    override fun close() {
        vertxClient.close()
    }
}
