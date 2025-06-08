package io.bluetape4k.feign.clients.vertx

import feign.AsyncClient
import feign.Request
import feign.Response
import io.bluetape4k.http.vertx.vertxHttpClientOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.vertx.core.http.HttpClient
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.CompletableFuture

class AsyncVertxHttpClient private constructor(
    private val vertxClient: HttpClient,
): AsyncClient<Any>, AutoCloseable {

    companion object: KLoggingChannel() {
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
        runBlocking {
            vertxClient.close().coAwait()
        }
    }
}
