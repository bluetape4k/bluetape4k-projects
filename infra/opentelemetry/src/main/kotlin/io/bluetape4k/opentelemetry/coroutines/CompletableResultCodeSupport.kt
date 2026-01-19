package io.bluetape4k.opentelemetry.coroutines

import io.opentelemetry.sdk.common.CompletableResultCode
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
