package io.bluetape4k.io.okio.coroutines

import kotlinx.coroutines.CancellableContinuation
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 비동기 파일 채널의 코루틴 방식으로 읽기 작업을 위한 [CompletionHandler] 구현체
 */
internal object ChannelCompletionHandler: CompletionHandler<Int, CancellableContinuation<Int>> {

    override fun completed(result: Int, attachment: CancellableContinuation<Int>) {
        attachment.resume(result)
    }

    override fun failed(exc: Throwable?, attachment: CancellableContinuation<Int>) {
        exc?.let { attachment.resumeWithException(it) }
    }
}
