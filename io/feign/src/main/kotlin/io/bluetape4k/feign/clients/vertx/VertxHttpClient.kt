package io.bluetape4k.feign.clients.vertx

import feign.Client
import io.bluetape4k.http.vertx.vertxHttpClientOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient

/**
 * Vert.x [HttpClient]를 사용하는 Feign 동기 [Client] 구현입니다.
 *
 * ## 동작/계약
 * - 내부 전송은 비동기 [sendAsync]를 사용하고 `get(timeout)`으로 동기 대기합니다.
 * - timeout 기준은 [feign.Request.Options.readTimeout] 설정을 따릅니다.
 *
 * ```kotlin
 * val client = VertxHttpClient()
 * // client != null
 * ```
 */
class VertxHttpClient private constructor(
    private val vertxClient: HttpClient,
): feign.Client, AutoCloseable {

    companion object: KLoggingChannel() {
        /**
         * [VertxHttpClient] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 [vertxClient]를 그대로 사용합니다.
         *
         * ```kotlin
         * val client = VertxHttpClient()
         * // client != null
         * ```
         */
        @JvmStatic
        operator fun invoke(vertxClient: HttpClient = vertxHttpClientOf()): VertxHttpClient {
            return VertxHttpClient(vertxClient)
        }
    }

    /**
     * Feign [Request]를 동기 실행하고 [feign.Response]를 반환합니다.
     *
     * ## 주의사항
     * - **Vert.x 이벤트 루프 스레드에서 호출 금지.** 이벤트 루프 컨텍스트에서 호출하면
     *   [IllegalStateException]이 발생합니다. 이벤트 루프 내에서 Feign 요청이 필요한 경우
     *   [AsyncVertxHttpClient]를 사용하거나 워커 스레드에서 호출하십시오.
     *
     * ```kotlin
     * val client = VertxHttpClient()
     * val request = feignRequestOf("https://example.com/health", HttpMethod.GET)
     * val response = client.execute(request, defaultRequestOptions)
     * // response.status() == 200
     * ```
     *
     * @throws IllegalStateException Vert.x 이벤트 루프 컨텍스트에서 호출된 경우
     */
    override fun execute(
        feignRequest: feign.Request,
        feignOptions: feign.Request.Options,
    ): feign.Response {
        check(Vertx.currentContext()?.isEventLoopContext != true) {
            "VertxHttpClient.execute() must not be called from a Vert.x event loop thread. " +
            "Use AsyncVertxHttpClient or invoke from a worker/blocking thread."
        }
        return vertxClient
            .sendAsync(feignRequest, feignOptions)
            .get(feignOptions.readTimeout(), feignOptions.readTimeoutUnit())
    }

    override fun close() {
        vertxClient.close()
    }
}
