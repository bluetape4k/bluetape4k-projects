package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.ForwardingBlockingSource
import io.bluetape4k.io.okio.coroutines.internal.ForwardingSuspendSource
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

fun okio.Source.toSuspend(context: CoroutineContext = Dispatchers.IO): SuspendSource {
    if (this is ForwardingBlockingSource)
        return this.delegate
    return ForwardingSuspendSource(this, context)
}

fun SuspendSource.toBlocking(): okio.Source {
    if (this is ForwardingSuspendSource)
        return this.delegate
    return ForwardingBlockingSource(this)
}
