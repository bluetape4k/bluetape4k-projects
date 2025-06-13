package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.ForwardingSource
import io.bluetape4k.io.okio.coroutines.internal.ForwardingSuspendSource
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

//fun Source.toAsync(context: CoroutineContext = Dispatchers.IO): AsyncSource {
//    if (this is ForwardingSource) return this.delegate
//    return ForwardingAsyncSource(this, context)
//}
//
//fun AsyncSource.toBlocking(): Source {
//    if (this is ForwardingAsyncSource) return this.delegate
//    return ForwardingSource(this)
//}

fun okio.Source.toSuspend(context: CoroutineContext = Dispatchers.IO): SuspendSource {
    if (this is ForwardingSource)
        return this.delegate
    return ForwardingSuspendSource(this, context)
}

fun SuspendSource.toBlocking(): okio.Source {
    if (this is ForwardingSuspendSource)
        return this.delegate
    return ForwardingSource(this)
}
