package io.bluetape4k.feign.clients.vertx

import feign.Client
import io.bluetape4k.http.vertx.vertxHttpClientOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
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
): feign.Client {

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

    override fun execute(
        feignRequest: feign.Request,
        feignOptions: feign.Request.Options,
    ): feign.Response {
        return vertxClient
            .sendAsync(feignRequest, feignOptions)
            .get(feignOptions.readTimeout(), feignOptions.readTimeoutUnit())
    }
}
