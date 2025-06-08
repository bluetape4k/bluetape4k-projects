package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors

fun okio.Sink.compressableSink(compressor: Compressor): CompressableSink =
    CompressableSink(this, compressor)

fun okio.Sink.deflateSink(): CompressableSink =
    compressableSink(Compressors.Deflate)

fun okio.Sink.gzipSink(): CompressableSink =
    compressableSink(Compressors.GZip)

fun okio.Sink.lz4Sink(): CompressableSink =
    compressableSink(Compressors.LZ4)

fun okio.Sink.snappySink(): CompressableSink =
    compressableSink(Compressors.Snappy)

fun okio.Sink.zstdSink(): CompressableSink =
    compressableSink(Compressors.Zstd)

fun okio.Sink.bzip2Sink(): CompressableSink =
    compressableSink(Compressors.BZip2)
