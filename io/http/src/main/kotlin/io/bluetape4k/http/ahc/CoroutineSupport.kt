package io.bluetape4k.http.ahc

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.asynchttpclient.AsyncCompletionHandler
import org.asynchttpclient.BoundRequestBuilder
import org.asynchttpclient.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * HTTP 처리에서 `executeSuspending` 함수를 제공합니다.
 */
@Deprecated("use suspendExecute instead", ReplaceWith("this.suspendExecute()"))
suspend fun BoundRequestBuilder.executeSuspending(): Response =
    suspendCancellableCoroutine { cont ->
        execute(DefaultCoroutineCompletionHandler(cont))
    }

/**
 * AsyncHttpClient 실행을 Coroutine 환경에서 수행하도록 합니다.
 */
suspend fun BoundRequestBuilder.suspendExecute(): Response =
    suspendCancellableCoroutine { cont ->
        execute(DefaultCoroutineCompletionHandler(cont))
    }

/**
 * HTTP 처리에서 사용하는 `DefaultCoroutineCompletionHandler` 타입입니다.
 */
internal class DefaultCoroutineCompletionHandler(
    private val cont: CancellableContinuation<Response>,
): AsyncCompletionHandler<Response>() {

    companion object: KLoggingChannel()

    /**
     * HTTP 처리에서 `onCompleted` 함수를 제공합니다.
     */
    override fun onCompleted(response: Response?): Response? {
        response?.let {
            cont.resume(it)
        } ?: cont.resumeWithException(IllegalStateException("Response is null"))
        return response
    }

    /**
     * HTTP 처리에서 `onThrowable` 함수를 제공합니다.
     */
    override fun onThrowable(t: Throwable) {
        cont.resumeWithException(t)
    }
}
