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

/**
 * Feign 연동에서 사용하는 `AsyncVertxHttpClient` 타입입니다.
 */
class AsyncVertxHttpClient private constructor(
    private val vertxClient: HttpClient,
): AsyncClient<Any>, AutoCloseable {

    companion object: KLoggingChannel() {
        /**
         * Feign 연동용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(vertxClient: HttpClient = vertxHttpClientOf()): AsyncVertxHttpClient {
            return AsyncVertxHttpClient(vertxClient)
        }
    }

    /**
     * Feign 연동에서 `execute` 함수를 제공합니다.
     */
    override fun execute(
        feignRequest: feign.Request,
        feignOptions: Request.Options,
        requestContext: Optional<Any>,
    ): CompletableFuture<Response> {
        return vertxClient.sendAsync(feignRequest, feignOptions)
    }

    /**
     * Feign 연동 리소스를 정리하고 닫습니다.
     */
    override fun close() {
        runBlocking {
            vertxClient.close().coAwait()
        }
    }
}
