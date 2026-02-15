package io.bluetape4k.http.okhttp3

import okhttp3.Callback
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.CompletableFuture

/**
 * [request]를 전송하고 [okhttp3.Response]를 반환합니다.
 *
 * @param request [okhttp3.Request] 인스턴스
 */
fun OkHttpClient.execute(request: okhttp3.Request): okhttp3.Response = newCall(request).execute()

/**
 * [OkHttpClient]를 비동기 방식으로 실행합니다.
 *
 * @param request [okhttp3.Request] 인스턴스
 * @param cancelHandler 취소 시 호출할 핸들러
 * @return [okhttp3.Response]를 담는 [CompletableFuture]
 */
inline fun OkHttpClient.executeAsync(
    request: okhttp3.Request,
    crossinline cancelHandler: (Throwable) -> Unit = {},
): CompletableFuture<okhttp3.Response> {
    val promise = CompletableFuture<okhttp3.Response>()

    val callback = object: Callback {
        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            when {
                response.isSuccessful -> promise.complete(response)
                call.isCanceled()     -> handleCanceled(IOException("Canceled"))
                else                  -> handleCanceled(IOException("Unexpected code $response"))
            }
        }

        override fun onFailure(call: okhttp3.Call, e: IOException) {
            if (call.isCanceled()) {
                handleCanceled(e)
            } else {
                promise.completeExceptionally(e)
            }
        }

        private fun handleCanceled(e: IOException) {
            cancelHandler(e)
            promise.completeExceptionally(e)
        }
    }

    newCall(request).enqueue(callback)
    return promise
}
