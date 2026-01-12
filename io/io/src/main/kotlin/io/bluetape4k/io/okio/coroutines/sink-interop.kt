package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.ForwardBlockingSink
import io.bluetape4k.io.okio.coroutines.internal.ForwardSuspendedSink

fun okio.Sink.asSuspended(): SuspendedSink = when (this) {
    is ForwardBlockingSink -> this.delegate
    else -> ForwardSuspendedSink(this)
}

fun SuspendedSink.asBlocking(): okio.Sink = when (this) {
    is ForwardSuspendedSink -> this.delegate
    else -> ForwardBlockingSink(this)
}
