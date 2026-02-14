package io.bluetape4k.io.okio.coroutines

import kotlinx.coroutines.CancellableContinuation
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 비동기 파일 채널의 코루틴 방식으로 읽기 작업을 위한 [CompletionHandler] 구현체
 */
/**
 * `ChannelCompletionHandler` 싱글톤/유틸리티입니다.
 */
internal object ChannelCompletionHandler: CompletionHandler<Int, CancellableContinuation<Int>> {

    /**
     * Okio 코루틴에서 `completed` 함수를 제공합니다.
     */
    override fun completed(result: Int, attachment: CancellableContinuation<Int>) {
        attachment.resume(result)
    }

    /**
     * Okio 코루틴에서 `failed` 함수를 제공합니다.
     */
    override fun failed(exc: Throwable?, attachment: CancellableContinuation<Int>) {
        exc?.let { attachment.resumeWithException(it) }
    }
}
