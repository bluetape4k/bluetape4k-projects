package io.bluetape4k.io.okio

import okio.BufferedSink
import okio.Sink
import okio.buffer

/**
 * [Sink]를 [BufferedSink]로 변환합니다.
 */
fun Sink.buffered(): BufferedSink = buffer()
