package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.ForwardingAsyncSource
import io.bluetape4k.io.okio.coroutines.internal.ForwardingSource
import kotlinx.coroutines.Dispatchers
import okio.Source
import kotlin.coroutines.CoroutineContext

fun Source.toAsync(context: CoroutineContext = Dispatchers.IO): AsyncSource {
    if (this is ForwardingSource) return this.delegate
    return ForwardingAsyncSource(this, context)
}

fun AsyncSource.toBlocking(): Source {
    if (this is ForwardingAsyncSource) return this.delegate
    return ForwardingSource(this)
}
