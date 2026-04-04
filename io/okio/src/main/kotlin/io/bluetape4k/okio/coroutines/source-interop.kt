package io.bluetape4k.okio.coroutines

import io.bluetape4k.okio.coroutines.internal.ForwardBlockingSource
import io.bluetape4k.okio.coroutines.internal.ForwardSuspendedSource

/**
 * 블로킹 [okio.Source]를 코루틴 방식의 [SuspendedSource]로 변환합니다.
 * 이미 [SuspendedSource]를 감싸고 있는 경우 원래 [SuspendedSource]를 반환합니다.
 *
 * ```kotlin
 * val buffer = bufferOf("hello")
 * val suspendedSource = (buffer as okio.Source).asSuspended()
 * // suspendedSource는 코루틴에서 사용 가능한 SuspendedSource
 * ```
 */
fun okio.Source.asSuspended(): SuspendedSource = when (this) {
    is ForwardBlockingSource -> this.delegate
    else -> ForwardSuspendedSource(this)
}

/**
 * 코루틴 방식의 [SuspendedSource]를 블로킹 [okio.Source]로 변환합니다.
 * 이미 블로킹 [okio.Source]를 감싸고 있는 경우 원래 [okio.Source]를 반환합니다.
 *
 * ```kotlin
 * val suspendedSource: SuspendedSource = bufferOf("hello").asSuspended()
 * val blockingSource: okio.Source = suspendedSource.asBlocking()
 * // blockingSource는 동기 I/O 컨텍스트에서 사용 가능
 * ```
 */
fun SuspendedSource.asBlocking(): okio.Source = when (this) {
    is ForwardSuspendedSource -> this.delegate
    else -> ForwardBlockingSource(this)
}
