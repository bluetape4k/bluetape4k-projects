package io.bluetape4k.okio.coroutines

import io.bluetape4k.okio.coroutines.internal.ForwardBlockingSink
import io.bluetape4k.okio.coroutines.internal.ForwardSuspendedSink

/**
 * 블로킹 [okio.Sink]를 코루틴 방식의 [SuspendedSink]로 변환합니다.
 * 이미 [SuspendedSink]를 감싸고 있는 경우 원래 [SuspendedSink]를 반환합니다.
 *
 * ```kotlin
 * val buffer = Buffer()
 * val suspendedSink = (buffer as okio.Sink).asSuspended()
 * // suspendedSink는 코루틴에서 사용 가능한 SuspendedSink
 * ```
 */
fun okio.Sink.asSuspended(): SuspendedSink = when (this) {
    is ForwardBlockingSink -> this.delegate
    else -> ForwardSuspendedSink(this)
}

/**
 * 코루틴 방식의 [SuspendedSink]를 블로킹 [okio.Sink]로 변환합니다.
 * 이미 블로킹 [okio.Sink]를 감싸고 있는 경우 원래 [okio.Sink]를 반환합니다.
 *
 * ```kotlin
 * val suspendedSink: SuspendedSink = Buffer().asSuspended()
 * val blockingSink: okio.Sink = suspendedSink.asBlocking()
 * // blockingSink는 동기 I/O 컨텍스트에서 사용 가능
 * ```
 */
fun SuspendedSink.asBlocking(): okio.Sink = when (this) {
    is ForwardSuspendedSink -> this.delegate
    else -> ForwardBlockingSink(this)
}
