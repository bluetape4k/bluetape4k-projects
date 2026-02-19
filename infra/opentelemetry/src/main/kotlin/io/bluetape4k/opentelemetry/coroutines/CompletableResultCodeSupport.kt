package io.bluetape4k.opentelemetry.coroutines

import io.opentelemetry.sdk.common.CompletableResultCode
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [CompletableResultCode]가 완료될 때까지 현재 코루틴을 중단합니다.
 *
 * @return [CompletableResultCode] 자신을 반환합니다.
 * @throws CompletionException 작업이 실패한 경우
 */
suspend fun CompletableResultCode.await(): CompletableResultCode = suspendCancellableCoroutine { cont ->
    if (isDone) {
        cont.resume(this)
    } else {
        whenComplete {
            if (isSuccess) cont.resume(this)
            else cont.resumeWithException(CompletionException("Fail to await for $this", null))
        }
    }
}
