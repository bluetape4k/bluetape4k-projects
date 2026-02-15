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
 * 현재 요청을 코루틴에서 실행하고 응답 완료 시점까지 대기합니다.
 */
suspend fun BoundRequestBuilder.executeSuspending(): Response =
    suspendCancellableCoroutine { cont ->
        val future = execute(DefaultCoroutineCompletionHandler(cont))
        cont.invokeOnCancellation {
            future.cancel(true)
        }
    }

/**
 * 현재 요청을 코루틴에서 실행하고 응답 완료 시점까지 대기합니다.
 *
 * 코루틴이 취소되면 내부 AHC Future 도 함께 취소합니다.
 */
@Deprecated("executeSuspending()을 사용하세요.", ReplaceWith("this.executeSuspending()"))
suspend fun BoundRequestBuilder.suspendExecute(): Response = executeSuspending()

/**
 * AHC 비동기 콜백을 코루틴 continuation 으로 연결합니다.
 */
internal class DefaultCoroutineCompletionHandler(
    private val cont: CancellableContinuation<Response>,
): AsyncCompletionHandler<Response>() {

    companion object: KLoggingChannel()

    /** continuation 이 활성 상태일 때 완료 응답으로 재개합니다. */
    override fun onCompleted(response: Response?): Response? {
        if (!cont.isActive) {
            return response
        }
        if (response != null) {
            cont.resume(response)
        } else {
            cont.resumeWithException(IllegalStateException("Response is null"))
        }
        return response
    }

    /** continuation 이 활성 상태일 때 예외로 재개합니다. */
    override fun onThrowable(t: Throwable) {
        if (cont.isActive) {
            cont.resumeWithException(t)
        }
    }
}
