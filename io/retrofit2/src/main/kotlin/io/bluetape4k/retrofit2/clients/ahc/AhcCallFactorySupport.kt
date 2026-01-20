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
 * Retrofit2 에서 OkHttp3를 대신 AsyncHttpClient를 사용할 수 있도록 해주는 Call.Factroy 입니다.
 *
 * ```
 * val factory = asyncHttpClientCallFactory {
 *      httpClient(defaultAsyncHttpClient)
 * }
 * ```
 * @param builder [AsyncHttpClient] 제공 함수
 * @return okhttp3.Call.Factory
 */
inline fun asyncHttpClientCallFactory(
    @BuilderInference builder: AsyncHttpClientCallFactory.AsyncHttpClientCallFactoryBuilder.() -> Unit,
): okhttp3.Call.Factory =
    AsyncHttpClientCallFactory.builder().apply(builder).build()

/**
 * Retrofit2에서 OkHttp3를 대신 AsyncHttpClient를 사용할 수 있도록 해주는 Call.Factroy 입니다.
 *
 * ```
 * val factory = asyncHttpClientCallFactoryOf(defaultAsyncHttpClient)
 * ```
 *
 * @param client [AsyncHttpClient] 제공 함수
 * @return okhttp3.Call.Factory
 */
inline fun asyncHttpClientCallFactoryOf(
    client: AsyncHttpClient = defaultAsyncHttpClient,
    @BuilderInference builder: AsyncHttpClientCallFactory.AsyncHttpClientCallFactoryBuilder.() -> Unit = {},
): okhttp3.Call.Factory =
    asyncHttpClientCallFactory {
        httpClient(client)
        builder()
    }

/**
 * [BoundRequestBuilder] 를 Coroutines 를 이용하여 실행합니다.
 *
 * ```
 * val response = requestBuilder.coExecute()
 * ```
 *
 * @receiver BoundRequestBuilder ahc의 요청 빌더
 * @return Response ahc의 응답
 */
suspend fun BoundRequestBuilder.coExecute(): Response =
    suspendCancellableCoroutine { cont ->
        execute(DefaultCoroutineCompletionHandler(cont))
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
