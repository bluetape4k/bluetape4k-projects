package io.bluetape4k.okio

import okio.BufferedSink
import okio.Sink
import okio.buffer

/**
 * [Sink]를 [BufferedSink]로 변환합니다.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink: BufferedSink = (output as Sink).buffered()
 * sink.writeUtf8("hello")
 * sink.flush()
 * val text = output.readUtf8()
 * // text == "hello"
 * ```
 */
fun Sink.buffered(): BufferedSink = buffer()
