package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.ForwardingSink
import io.bluetape4k.io.okio.coroutines.internal.ForwardingSuspendSink
import kotlinx.coroutines.Dispatchers
import okio.Sink
import kotlin.coroutines.CoroutineContext

fun Sink.toSuspend(context: CoroutineContext = Dispatchers.IO): SuspendSink {
    if (this is ForwardingSink) return this.delegate
    return ForwardingSuspendSink(this, context)
}

fun SuspendSink.toBlocking(): Sink {
    if (this is ForwardingSuspendSink) return this.delegate
    return ForwardingSink(this)
}
