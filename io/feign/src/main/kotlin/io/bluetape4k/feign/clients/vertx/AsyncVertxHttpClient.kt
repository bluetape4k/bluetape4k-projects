package io.bluetape4k.feign.clients.vertx

import feign.AsyncClient
import feign.Request
import feign.Response
import io.bluetape4k.http.vertx.vertxHttpClientOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.vertx.core.http.HttpClient
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Vert.x [HttpClient]를 사용하는 Feign 비동기 클라이언트 구현입니다.
 *
 * ## 동작/계약
 * - [execute]는 Vert.x 요청을 [CompletableFuture]로 반환합니다.
 * - close 시 내부 Vert.x client를 비동기로 종료합니다.
 * - 기본 생성 경로는 [vertxHttpClientOf]를 사용합니다.
 *
 * ```kotlin
 * val client = AsyncVertxHttpClient()
 * // client != null
 * ```
 */
class AsyncVertxHttpClient private constructor(
    private val vertxClient: HttpClient,
): AsyncClient<Any>, AutoCloseable {

    companion object: KLoggingChannel() {
        /**
         * [AsyncVertxHttpClient] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 [vertxClient]를 그대로 사용합니다.
         *
         * ```kotlin
         * val client = AsyncVertxHttpClient()
         * // client != null
         * ```
         */
        @JvmStatic
        operator fun invoke(vertxClient: HttpClient = vertxHttpClientOf()): AsyncVertxHttpClient {
            return AsyncVertxHttpClient(vertxClient)
        }
    }

    /**
     * Feign [Request]를 비동기 실행하고 [CompletableFuture]<[Response]>를 반환합니다.
     *
     * ```kotlin
     * val client = AsyncVertxHttpClient()
     * val request = feignRequestOf("https://example.com/health", HttpMethod.GET)
     * val future = client.execute(request, defaultRequestOptions, Optional.empty())
     * val response = future.get()
     * // response.status() == 200
     * ```
     */
    override fun execute(
        feignRequest: feign.Request,
        feignOptions: Request.Options,
        requestContext: Optional<Any>,
    ): CompletableFuture<Response> {
        return vertxClient.sendAsync(feignRequest, feignOptions)
    }

    /**
     * 내부 Vert.x HTTP 클라이언트를 종료합니다.
     *
     * ## 동작/계약
     * - [HttpClient.close]는 non-blocking 호출입니다.
     * - runBlocking을 사용하지 않아 코루틴 컨텍스트와 충돌하지 않습니다.
     */
    override fun close() {
        vertxClient.close()
    }
}
