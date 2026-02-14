package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors

/**
 * Okio 압축/해제에서 `compressableSink` 함수를 제공합니다.
 */
fun okio.Sink.compressableSink(compressor: Compressor): CompressableSink =
    CompressableSink(this, compressor)

/**
 * Okio 압축/해제에서 `deflateSink` 함수를 제공합니다.
 */
fun okio.Sink.deflateSink(): CompressableSink =
    compressableSink(Compressors.Deflate)

/**
 * Okio 압축/해제에서 `gzipSink` 함수를 제공합니다.
 */
fun okio.Sink.gzipSink(): CompressableSink =
    compressableSink(Compressors.GZip)

/**
 * Okio 압축/해제에서 `lz4Sink` 함수를 제공합니다.
 */
fun okio.Sink.lz4Sink(): CompressableSink =
    compressableSink(Compressors.LZ4)

/**
 * Okio 압축/해제에서 `snappySink` 함수를 제공합니다.
 */
fun okio.Sink.snappySink(): CompressableSink =
    compressableSink(Compressors.Snappy)

/**
 * Okio 압축/해제에서 `zstdSink` 함수를 제공합니다.
 */
fun okio.Sink.zstdSink(): CompressableSink =
    compressableSink(Compressors.Zstd)

/**
 * Okio 압축/해제에서 `bzip2Sink` 함수를 제공합니다.
 */
fun okio.Sink.bzip2Sink(): CompressableSink =
    compressableSink(Compressors.BZip2)
