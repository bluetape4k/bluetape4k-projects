package io.bluetape4k.io.okio

import io.bluetape4k.io.compressor.Compressor
import okio.ForwardingSource
import okio.Source

/**
 * 데이터를 압축 해제하여 [Source]로 읽는 [Source] 구현체.
 *
 * @see CompressSink
 */
@Deprecated(
    "Use io.bluetape4k.io.okio.compress.DecompressSource instead",
    ReplaceWith("io.bluetape4k.io.okio.compress.DecompressSource(delegate, compressor)")
)
class DecompressSource(
    delegate: Source,
    val compressor: Compressor,
): ForwardingSource(delegate) {
}
