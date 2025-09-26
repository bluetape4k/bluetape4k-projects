package io.bluetape4k.http.hc5.http

import io.bluetape4k.coroutines.support.suspendAwait
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.nio.AsyncClientEndpoint
import org.apache.hc.core5.http.nio.AsyncRequestProducer
import org.apache.hc.core5.http.nio.AsyncResponseConsumer

/**
 * Coroutines 환경에서 [AsyncClientEndpoint]를 이용하여
 * 요청 [SimpleHttpRequest] 을 전송하고, [SimpleHttpResponse] 를 받습니다.
 *
 * ```
 * runBlocking {
 *      val endpoint = asyncClientEndpointOf()
 *      val request = simpleHttpRequestOf("http://localhost:8080")
 *      val response = endpoint.executeSuspending(request)
 * }
 * ```
 *
 * @param request 요청 정보 [SimpleHttpRequest]
 * @return 응답 정보 [SimpleHttpResponse]
 */
suspend fun AsyncClientEndpoint.suspendExecute(request: SimpleHttpRequest): SimpleHttpResponse {
    return execute(request.toProducer(), SimpleResponseConsumer.create(), null).suspendAwait()
}

/**
 * Coroutines 환경에서 [AsyncClientEndpoint]를 이용하여
 * [requestProducer] 를 이용하여 요청을 전송하고, [responseConsumer] 를 이용하여 응답을 [T] 타입으로 변환하여 반환합니다.
 *
 * ```
 * runBlocking {
 *      val endpoint = asyncClientEndpointOf()
 *      val requestProducer = simpleRequestProducerOf("http://localhost:8080")
 *      val responseConsumer = simpleResponseConsumerOf()
 *      val response = endpoint.executeSuspending(requestProducer, responseConsumer)
 * }
 * ```
 *
 * @param requestProducer 요청 정보 제공자 [AsyncRequestProducer]
 * @param responseConsumer 응답 정보 변환자 [AsyncResponseConsumer]
 * @param callback 응답 콜백 [FutureCallback]
 * @return 응답 정보 [T]
 */
suspend fun <T: Any> AsyncClientEndpoint.suspendExecute(
    requestProducer: AsyncRequestProducer,
    responseConsumer: AsyncResponseConsumer<T>,
    callback: FutureCallback<T>? = null,
): T {
    return execute(requestProducer, responseConsumer, callback).suspendAwait()
}
