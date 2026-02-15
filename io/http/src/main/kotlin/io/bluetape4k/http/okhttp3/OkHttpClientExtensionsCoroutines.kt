package io.bluetape4k.http.okhttp3

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resumeWithException

/**
 * 코루틴 환경에서 [request]를 전송하고 [Response]를 반환합니다.
 *
 * @param request [okhttp3.Request] 인스턴스
 */
suspend inline fun okhttp3.OkHttpClient.executeSuspending(request: okhttp3.Request): Response =
    newCall(request).executeSuspending()

/**
 * [executeSuspending]으로 대체되었습니다.
 */
@Deprecated("executeSuspending(request)를 사용하세요.", ReplaceWith("this.executeSuspending(request)"))
suspend inline fun okhttp3.OkHttpClient.suspendExecute(request: okhttp3.Request): Response =
    executeSuspending(request)

/**
 * [Call]을 코루틴 방식으로 실행합니다. (Non-Blocking)
 */
suspend inline fun Call.executeSuspending(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation {
        this.cancel()
    }

    val responseCallback = object: Callback {
        override fun onResponse(call: Call, response: Response) {
            if (cont.isActive) {
                cont.resume(response) { _, _, _ -> call.cancel() }
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isActive) {
                return
            }
            if (call.isCanceled()) {
                cont.cancel(e)
            } else {
                cont.resumeWithException(e)
            }
        }
    }
    enqueue(responseCallback)
}

/**
 * [executeSuspending]으로 대체되었습니다.
 */
@Deprecated("executeSuspending()을 사용하세요.", ReplaceWith("this.executeSuspending()"))
suspend inline fun Call.suspendExecute(): Response = executeSuspending()
