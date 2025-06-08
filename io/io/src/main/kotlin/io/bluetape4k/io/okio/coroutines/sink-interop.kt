package io.bluetape4k.io.okio.coroutines

//fun Sink.toAsync(context: CoroutineContext = Dispatchers.IO): AsyncSink {
//    if (this is ForwardingSink) return this.delegate
//    return ForwardingAsyncSink(this, context)
//}

//fun AsyncSink.toBlocking(): Sink {
//    if (this is ForwardingAsyncSink) return this.delegate
//    return ForwardingSink(this)
//}
