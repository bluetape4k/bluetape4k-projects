package io.bluetape4k.opentelemetry.coroutines

import io.opentelemetry.sdk.common.CompletableResultCode
import java.util.concurrent.CompletionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun CompletableResultCode.await(): CompletableResultCode {
    return suspendCoroutine { cont ->
        if (isDone) {
            cont.resume(this)
        } else {
            whenComplete {
                if (isSuccess) cont.resume(this)
                else cont.resumeWithException(CompletionException("Fail to await for $this", null))
            }
        }
    }
}
