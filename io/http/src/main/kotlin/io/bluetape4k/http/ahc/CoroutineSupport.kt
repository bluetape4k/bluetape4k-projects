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
 * AsyncHttpClient 실행을 Coroutine 환경에서 수행하도록 합니다.
 */
suspend fun BoundRequestBuilder.executeSuspending(): Response =
    suspendCancellableCoroutine { cont ->
        execute(DefaultCoroutineCompletionHandler(cont))
    }

internal class DefaultCoroutineCompletionHandler(
    private val cont: CancellableContinuation<Response>,
): AsyncCompletionHandler<Response>() {

    companion object: KLoggingChannel()

    override fun onCompleted(response: Response?): Response? {
        response?.let {
            cont.resume(it)
        } ?: cont.resumeWithException(IllegalStateException("Response is null"))
        return response
    }

    override fun onThrowable(t: Throwable) {
        cont.resumeWithException(t)
    }
}
