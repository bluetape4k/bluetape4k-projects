package io.bluetape4k.http.hc5.async

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.http.hc5.async.methods.toProducer
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.protocol.HttpContext

/**
 * Coroutines 방식으로 [CloseableHttpAsyncClient]를 사용하여 요청을 실행합니다.
 *
 * ```
 * val client = CloseableHttpAsyncClientBuilder.create().build()
 * client.start()
 *
 * val request = SimpleHttpRequest.get("https://httpbin.org/get")
 * val response = runBlocking { client.executeSuspending(request) }
 * ```
 *
 * @param request 요청 정보
 * @param responseConsumer 응답 정보
 * @param context [HttpContext] 인스턴스
 * @param callback [FutureCallback] 인스턴스
 * @return [SimpleHttpResponse] 인스턴스
 */
suspend fun CloseableHttpAsyncClient.suspendExecute(
    request: SimpleHttpRequest,
    responseConsumer: SimpleResponseConsumer = SimpleResponseConsumer.create(),
    context: HttpContext = HttpClientContext.create(),
    callback: FutureCallback<SimpleHttpResponse>? = null,
): SimpleHttpResponse {
    return execute(request.toProducer(), responseConsumer, context, callback).suspendAwait()
}
