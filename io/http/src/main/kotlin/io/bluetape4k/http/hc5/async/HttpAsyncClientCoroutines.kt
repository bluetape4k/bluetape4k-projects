package io.bluetape4k.http.hc5.async

import io.bluetape4k.coroutines.support.suspendAwait
import org.apache.hc.client5.http.async.HttpAsyncClient
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.nio.AsyncPushConsumer
import org.apache.hc.core5.http.nio.AsyncRequestProducer
import org.apache.hc.core5.http.nio.AsyncResponseConsumer
import org.apache.hc.core5.http.nio.HandlerFactory
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.reactor.IOReactorStatus

/**
 * Coroutines 환경에서 [HttpAsyncClient.execute]를 실행합니다.
 *
 * ```
 * val client = httpAsyncClient {
 *   setConnectionManager(cm)
 *   setMaxConnTotal(100)
 *   setMaxConnPerRoute(10)
 * }
 *
 * val requestProvider = AsyncRequestProducer.create(HttpHost("httpbin.org"), SimpleHttpRequest.get("/get"))
 * val responseConsumer = AsyncResponseConsumer.ofString()
 *
 * val response = client.executeSuspending(
 *      requestProvider,  // 요청 자료
 *      responseConsumer, // 응답 처리
 * )
 * ```
 *
 * @param T 반환 수형
 * @param requestProducer 비동기 요청을 생성하는 생산자([AsyncRequestProducer])
 * @param responseConsumer 응답을 처리하는 소비자([AsyncResponseConsumer])
 * @param pushHandlerFactory 비동기 푸시 소비자를 생성하는 팩토리
 * @param context  [HttpContext] 인스턴스
 * @return `responseConsumer`에서 처리한 결과
 */
suspend inline fun <T: Any> CloseableHttpAsyncClient.executeSuspending(
    requestProducer: AsyncRequestProducer,
    responseConsumer: AsyncResponseConsumer<T>,
    pushHandlerFactory: HandlerFactory<AsyncPushConsumer>? = null,
    context: HttpContext? = null,
): T {
    if (status == IOReactorStatus.INACTIVE) {
        start()
    }
    return execute(
        requestProducer,
        responseConsumer,
        pushHandlerFactory,
        context ?: HttpClientContext.create(),
        null
    ).suspendAwait()
}

/**
 * [executeSuspending]으로 대체되었습니다.
 */
@Deprecated(
    "executeSuspending(requestProducer, responseConsumer, pushHandlerFactory, context)을 사용하세요.",
    ReplaceWith("this.executeSuspending(requestProducer, responseConsumer, pushHandlerFactory, context)")
)
suspend inline fun <T: Any> CloseableHttpAsyncClient.execute(
    requestProducer: AsyncRequestProducer,
    responseConsumer: AsyncResponseConsumer<T>,
    pushHandlerFactory: HandlerFactory<AsyncPushConsumer>? = null,
    context: HttpContext? = null,
): T = executeSuspending(requestProducer, responseConsumer, pushHandlerFactory, context)

/**
 * Coroutines 환경에서 [CloseableHttpAsyncClient.execute]를 실행합니다.
 *
 * ```
 * val client = httpAsyncClient {
 *     setConnectionManager(cm)
 *     setMaxConnTotal(100)
 *     setMaxConnPerRoute(10)
 * }
 * val request = SimpleHttpRequest.get("http://httpbin.org/get")
 * val response = client.executeSuspending(request)
 * ```
 *
 * @param request 요청 자료
 * @param context [HttpClientContext] 인스턴스
 * @return [SimpleHttpResponse] 인스턴스
 */
suspend inline fun CloseableHttpAsyncClient.executeSuspending(
    request: SimpleHttpRequest,
    context: HttpClientContext = HttpClientContext.create(),
    callback: FutureCallback<SimpleHttpResponse>? = null,
): SimpleHttpResponse {
    if (status == IOReactorStatus.INACTIVE) {
        start()
    }
    return execute(request, context, callback).suspendAwait()
}

/**
 * [executeSuspending]으로 대체되었습니다.
 */
@Deprecated(
    "executeSuspending(request, context, callback)을 사용하세요.",
    ReplaceWith("this.executeSuspending(request, context, callback)")
)
suspend inline fun CloseableHttpAsyncClient.execute(
    request: SimpleHttpRequest,
    context: HttpClientContext = HttpClientContext.create(),
    callback: FutureCallback<SimpleHttpResponse>? = null,
): SimpleHttpResponse = executeSuspending(request, context, callback)

/**
 * Coroutines 환경에서 [CloseableHttpAsyncClient.execute]를 실행합니다.
 *
 * ```
 * val client = httpAsyncClient {
 *    setConnectionManager(cm)
 *    setMaxConnTotal(100)
 *    setMaxConnPerRoute(10)
 * }
 * val requestProducer = AsyncRequestProducer.create(HttpHost("httpbin.org"), SimpleHttpRequest.get("/get"))
 * val responseConsumer = AsyncResponseConsumer.ofString()
 *
 * val response = client.executeSuspending(requestProducer, responseConsumer)
 * ```
 *
 * @param T 반환 수형
 * @param requestProducer 비동기 요청을 생성하는 생산자([AsyncRequestProducer])
 * @param responseConsumer 응답을 처리하는 소비자([AsyncResponseConsumer])
 * @param callback [FutureCallback] 인스턴스
 * @return `responseConsumer`에서 처리한 결과
 */
suspend inline fun <T: Any> CloseableHttpAsyncClient.executeSuspending(
    requestProducer: AsyncRequestProducer,
    responseConsumer: AsyncResponseConsumer<T>,
    callback: FutureCallback<T>? = null,
): T {
    if (status == IOReactorStatus.INACTIVE) {
        start()
    }
    return execute(
        requestProducer,
        responseConsumer,
        callback,
    ).suspendAwait()
}

/**
 * [executeSuspending]으로 대체되었습니다.
 */
@Deprecated(
    "executeSuspending(requestProducer, responseConsumer, callback)을 사용하세요.",
    ReplaceWith("this.executeSuspending(requestProducer, responseConsumer, callback)")
)
suspend inline fun <T: Any> CloseableHttpAsyncClient.execute(
    requestProducer: AsyncRequestProducer,
    responseConsumer: AsyncResponseConsumer<T>,
    callback: FutureCallback<T>? = null,
): T = executeSuspending(requestProducer, responseConsumer, callback)

/**
 * Coroutines 환경에서 [CloseableHttpAsyncClient.execute]를 실행합니다.
 *
 * ```
 * val client = httpAsyncClient {
 *    setConnectionManager(cm)
 *    setMaxConnTotal(100)
 *    setMaxConnPerRoute(10)
 * }
 * val target = HttpHost("httpbin.org")
 * val requestProducer = AsyncRequestProducer.create(target, SimpleHttpRequest.get("/get"))
 * val responseConsumer = AsyncResponseConsumer.ofString()
 *
 * val response = client.executeSuspending(target, requestProducer, responseConsumer)
 * ```
 *
 * @param T 반환 수형
 * @param target [HttpHost] 인스턴스
 * @param requestProducer 비동기 요청을 생성하는 생산자([AsyncRequestProducer])
 * @param responseConsumer 응답을 처리하는 소비자([AsyncResponseConsumer])
 * @param pushHandlerFactory 비동기 푸시 소비자를 생성하는 팩토리
 * @param context [HttpContext] 인스턴스
 * @param callback [FutureCallback] 인스턴스
 * @return `responseConsumer`에서 처리한 결과
 */
suspend inline fun <T: Any> CloseableHttpAsyncClient.executeSuspending(
    target: HttpHost,
    requestProducer: AsyncRequestProducer,
    responseConsumer: AsyncResponseConsumer<T>,
    pushHandlerFactory: HandlerFactory<AsyncPushConsumer>? = null,
    context: HttpContext? = null,
    callback: FutureCallback<T>? = null,
): T {
    if (status == IOReactorStatus.INACTIVE) {
        start()
    }
    return execute(
        target,
        requestProducer,
        responseConsumer,
        pushHandlerFactory,
        context ?: HttpClientContext.create(),
        callback,
    ).suspendAwait()
}

/**
 * [executeSuspending]으로 대체되었습니다.
 */
@Deprecated(
    "executeSuspending(target, requestProducer, responseConsumer, pushHandlerFactory, context, callback)을 사용하세요.",
    ReplaceWith("this.executeSuspending(target, requestProducer, responseConsumer, pushHandlerFactory, context, callback)")
)
suspend inline fun <T: Any> CloseableHttpAsyncClient.execute(
    target: HttpHost,
    requestProducer: AsyncRequestProducer,
    responseConsumer: AsyncResponseConsumer<T>,
    pushHandlerFactory: HandlerFactory<AsyncPushConsumer>? = null,
    context: HttpContext? = null,
    callback: FutureCallback<T>? = null,
): T = executeSuspending(target, requestProducer, responseConsumer, pushHandlerFactory, context, callback)
