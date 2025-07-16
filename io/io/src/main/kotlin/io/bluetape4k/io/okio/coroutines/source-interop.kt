package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.ForwardingBlockingSource
import io.bluetape4k.io.okio.coroutines.internal.ForwardingSuspendedSource
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

fun okio.Source.toSuspend(context: CoroutineContext = Dispatchers.IO): SuspendedSource {
    if (this is ForwardingBlockingSource)
        return this.delegate
    return ForwardingSuspendedSource(this, context)
}

fun SuspendedSource.toBlocking(): okio.Source {
    if (this is ForwardingSuspendedSource)
        return this.delegate
    return ForwardingBlockingSource(this)
}
