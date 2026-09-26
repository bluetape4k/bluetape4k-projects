package io.bluetape4k.coroutines.support

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

private object TimeoutLog: KLoggingChannel()

suspend fun <T> retryWithTimeoutOrNull(
    maxRetries: Int,
    timeout: Duration,
    block: suspend CoroutineScope.() -> T,
): T? {
    maxRetries.requirePositiveNumber("maxRetries")

    repeat(maxRetries) { attempt ->
        val result = withTimeoutOrNull(timeout, block)
        if (result != null) return result
        TimeoutLog.log.debug { "시도 ${attempt + 1}/$maxRetries 타임아웃" }
    }
    return null
}
