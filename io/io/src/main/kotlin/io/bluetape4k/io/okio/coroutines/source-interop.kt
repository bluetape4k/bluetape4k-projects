package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.ForwardBlockingSource
import io.bluetape4k.io.okio.coroutines.internal.ForwardSuspendedSource

fun okio.Source.asSuspended(): SuspendedSource = when (this) {
    is ForwardBlockingSource -> this.delegate
    else -> ForwardSuspendedSource(this)
}

fun SuspendedSource.asBlocking(): okio.Source = when (this) {
    is ForwardSuspendedSource -> this.delegate
    else -> ForwardBlockingSource(this)
}
