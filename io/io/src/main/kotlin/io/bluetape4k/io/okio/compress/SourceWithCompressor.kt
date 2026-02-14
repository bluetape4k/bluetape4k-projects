package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors

/**
 * Okio 압축/해제에서 `decompressableSource` 함수를 제공합니다.
 */
fun okio.Source.decompressableSource(compressor: Compressor): DecompressableSource =
    DecompressableSource(this, compressor)

/**
 * Okio 압축/해제에서 `inflateSource` 함수를 제공합니다.
 */
fun okio.Source.inflateSource(): DecompressableSource =
    decompressableSource(Compressors.Deflate)

/**
 * Okio 압축/해제에서 `gzipSource` 함수를 제공합니다.
 */
fun okio.Source.gzipSource(): DecompressableSource =
    decompressableSource(Compressors.GZip)

/**
 * Okio 압축/해제에서 `lz4Source` 함수를 제공합니다.
 */
fun okio.Source.lz4Source(): DecompressableSource =
    decompressableSource(Compressors.LZ4)

/**
 * Okio 압축/해제에서 `snappySource` 함수를 제공합니다.
 */
fun okio.Source.snappySource(): DecompressableSource =
    decompressableSource(Compressors.Snappy)

/**
 * Okio 압축/해제에서 `zstdSource` 함수를 제공합니다.
 */
fun okio.Source.zstdSource(): DecompressableSource =
    decompressableSource(Compressors.Zstd)

/**
 * Okio 압축/해제에서 `bzip2Source` 함수를 제공합니다.
 */
fun okio.Source.bzip2Source(): DecompressableSource =
    decompressableSource(Compressors.BZip2)
