package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.ForwardBlockingSink
import io.bluetape4k.io.okio.coroutines.internal.ForwardSuspendedSink

/**
 * Okio 코루틴 타입 변환을 위한 `asSuspended` 함수를 제공합니다.
 */
fun okio.Sink.asSuspended(): SuspendedSink = when (this) {
    is ForwardBlockingSink -> this.delegate
    else -> ForwardSuspendedSink(this)
}

/**
 * Okio 코루틴 타입 변환을 위한 `asBlocking` 함수를 제공합니다.
 */
fun SuspendedSink.asBlocking(): okio.Sink = when (this) {
    is ForwardSuspendedSink -> this.delegate
    else -> ForwardBlockingSink(this)
}
