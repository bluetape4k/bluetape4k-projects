package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.ForwardingBlockingSink
import io.bluetape4k.io.okio.coroutines.internal.ForwardingSuspendSink
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

fun okio.Sink.toSuspend(context: CoroutineContext = Dispatchers.IO): SuspendSink {
    if (this is ForwardingBlockingSink) return this.delegate
    return ForwardingSuspendSink(this, context)
}

fun SuspendSink.toBlocking(): okio.Sink {
    if (this is ForwardingSuspendSink) return this.delegate
    return ForwardingBlockingSink(this)
}
