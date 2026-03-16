package io.bluetape4k.retrofit2.clients.ahc

import io.bluetape4k.http.ahc.defaultAsyncHttpClient
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.asynchttpclient.AsyncCompletionHandler
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.BoundRequestBuilder
import org.asynchttpclient.Response
import org.asynchttpclient.extras.retrofit.AsyncHttpClientCallFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * AsyncHttpClient 기반 Retrofit `Call.Factory`를 빌더 DSL로 생성합니다.
 *
 * ## 동작/계약
 * - [builder]를 `AsyncHttpClientCallFactory.builder()`에 적용해 최종 팩토리를 만듭니다.
 * - 기본값 없이 [builder]에서 `httpClient(...)`를 지정해야 합니다.
 *
 * ```kotlin
 * val factory = asyncHttpClientCallFactory { httpClient(defaultAsyncHttpClient) }
 * // factory != null
 * ```
 */
inline fun asyncHttpClientCallFactory(
    builder: AsyncHttpClientCallFactory.AsyncHttpClientCallFactoryBuilder.() -> Unit,
): okhttp3.Call.Factory =
    AsyncHttpClientCallFactory.builder().apply(builder).build()

/**
 * AsyncHttpClient 인스턴스를 지정해 Retrofit `Call.Factory`를 생성합니다.
 *
 * ## 동작/계약
 * - [client]를 기본값([defaultAsyncHttpClient])으로 사용합니다.
 * - 추가 [builder] 설정을 동일 빌더에 이어서 적용합니다.
 *
 * ```kotlin
 * val factory = asyncHttpClientCallFactoryOf()
 * // factory != null
 * ```
 *
 * @param client 사용할 AsyncHttpClient입니다.
 */
inline fun asyncHttpClientCallFactoryOf(
    client: AsyncHttpClient = defaultAsyncHttpClient,
    builder: AsyncHttpClientCallFactory.AsyncHttpClientCallFactoryBuilder.() -> Unit = {},
): okhttp3.Call.Factory =
    asyncHttpClientCallFactory {
        httpClient(client)
        builder()
    }

/**
 * [BoundRequestBuilder]를 코루틴으로 실행하고 응답을 반환합니다.
 *
 * ## 동작/계약
 * - 내부 `execute(handler)` Future를 코루틴 취소와 연동합니다.
 * - 코루틴 취소 시 `future.cancel(true)`를 호출합니다.
 * - AHC 실패는 예외로 재개됩니다.
 *
 * ```kotlin
 * val response = requestBuilder.coExecute()
 * // response.statusCode >= 200
 * ```
 */
suspend fun BoundRequestBuilder.coExecute(): Response =
    suspendCancellableCoroutine { cont ->
        val future = execute(DefaultCoroutineCompletionHandler(cont))
        cont.invokeOnCancellation { future.cancel(true) }
    }

internal class DefaultCoroutineCompletionHandler(
    private val cont: CancellableContinuation<Response>,
): AsyncCompletionHandler<Response>() {

    override fun onCompleted(response: Response): Response {
        cont.resume(response)
        return response
    }

    override fun onThrowable(t: Throwable) {
        cont.resumeWithException(t)
    }
}
