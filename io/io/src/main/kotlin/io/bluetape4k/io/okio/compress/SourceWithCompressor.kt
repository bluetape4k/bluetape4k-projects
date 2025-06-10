package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors

fun okio.Source.decompressableSource(compressor: Compressor): DecompressableSource =
    DecompressableSource(this, compressor)

fun okio.Source.inflateSource(): DecompressableSource =
    decompressableSource(Compressors.Deflate)

fun okio.Source.gzipSource(): DecompressableSource =
    decompressableSource(Compressors.GZip)

fun okio.Source.lz4Source(): DecompressableSource =
    decompressableSource(Compressors.LZ4)

fun okio.Source.snappySource(): DecompressableSource =
    decompressableSource(Compressors.Snappy)

fun okio.Source.zstdSource(): DecompressableSource =
    decompressableSource(Compressors.Zstd)

fun okio.Source.bzip2Source(): DecompressableSource =
    decompressableSource(Compressors.BZip2)
