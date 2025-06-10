package io.bluetape4k.io.okio

import io.bluetape4k.io.compressor.Compressor
import okio.ForwardingSink
import okio.Sink

/**
 * 데이터를 압축하여 [Sink]에 쓰는 [Sink] 구현체.
 *
 * @see DecompressSource
 */
@Deprecated(
    "Use io.bluetape4k.io.okio.compress.CompressSource instead",
    ReplaceWith("io.bluetape4k.io.okio.compress.CompressSource(delegate, compressor)")
)
class CompressSink(
    delegate: Sink,
    val compressor: Compressor,
): ForwardingSink(delegate) {
}
