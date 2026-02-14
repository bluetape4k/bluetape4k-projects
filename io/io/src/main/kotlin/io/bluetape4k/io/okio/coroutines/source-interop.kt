package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.ForwardBlockingSource
import io.bluetape4k.io.okio.coroutines.internal.ForwardSuspendedSource

/**
 * Okio 코루틴 타입 변환을 위한 `asSuspended` 함수를 제공합니다.
 */
fun okio.Source.asSuspended(): SuspendedSource = when (this) {
    is ForwardBlockingSource -> this.delegate
    else -> ForwardSuspendedSource(this)
}

/**
 * Okio 코루틴 타입 변환을 위한 `asBlocking` 함수를 제공합니다.
 */
fun SuspendedSource.asBlocking(): okio.Source = when (this) {
    is ForwardSuspendedSource -> this.delegate
    else -> ForwardBlockingSource(this)
}
