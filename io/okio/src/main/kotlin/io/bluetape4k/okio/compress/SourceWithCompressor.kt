package io.bluetape4k.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.compressor.StreamingCompressor

/**
 * [Compressor]를 사용해 압축 해제하는 [DecompressableSource]로 변환합니다.
 *
 * ```kotlin
 * val compressed = Buffer() // 압축된 데이터
 * val source = (compressed as okio.Source).decompressableSource(Compressors.GZip)
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == 원본 데이터
 * ```
 */
fun okio.Source.decompressableSource(compressor: Compressor): DecompressableSource =
    DecompressableSource(this, compressor)

/**
 * [StreamingCompressor]를 사용해 스트리밍 방식으로 압축 해제하는 [DecompressableSource]로 변환합니다.
 *
 * ```kotlin
 * val compressed = Buffer() // 압축된 데이터
 * val source = (compressed as okio.Source).decompressableSource(
 *     Compressors.GZip as StreamingCompressor)
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * ```
 */
fun okio.Source.decompressableSource(compressor: StreamingCompressor): DecompressableSource =
    StreamingDecompressSource(this, compressor)

/**
 * Deflate 압축된 데이터를 해제하는 [DecompressableSource]로 변환합니다.
 *
 * ```kotlin
 * val compressed = Buffer() // deflate 압축된 데이터
 * val source = (compressed as okio.Source).inflateSource()
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == 원본 데이터
 * ```
 */
fun okio.Source.inflateSource(): DecompressableSource =
    decompressableSource(Compressors.Deflate)

/**
 * GZip 압축된 데이터를 해제하는 [DecompressableSource]로 변환합니다.
 *
 * ```kotlin
 * val compressed = Buffer() // gzip 압축된 데이터
 * val source = (compressed as okio.Source).gzipSource()
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == 원본 데이터
 * ```
 */
fun okio.Source.gzipSource(): DecompressableSource =
    decompressableSource(Compressors.GZip)

/**
 * LZ4 압축된 데이터를 해제하는 [DecompressableSource]로 변환합니다.
 *
 * ```kotlin
 * val compressed = Buffer() // lz4 압축된 데이터
 * val source = (compressed as okio.Source).lz4Source()
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == 원본 데이터
 * ```
 */
fun okio.Source.lz4Source(): DecompressableSource =
    decompressableSource(Compressors.LZ4)

/**
 * Snappy 압축된 데이터를 해제하는 [DecompressableSource]로 변환합니다.
 *
 * ```kotlin
 * val compressed = Buffer() // snappy 압축된 데이터
 * val source = (compressed as okio.Source).snappySource()
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == 원본 데이터
 * ```
 */
fun okio.Source.snappySource(): DecompressableSource =
    decompressableSource(Compressors.Snappy)

/**
 * Zstd 압축된 데이터를 해제하는 [DecompressableSource]로 변환합니다.
 *
 * ```kotlin
 * val compressed = Buffer() // zstd 압축된 데이터
 * val source = (compressed as okio.Source).zstdSource()
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == 원본 데이터
 * ```
 */
fun okio.Source.zstdSource(): DecompressableSource =
    decompressableSource(Compressors.Zstd)

/**
 * BZip2 압축된 데이터를 해제하는 [DecompressableSource]로 변환합니다.
 *
 * ```kotlin
 * val compressed = Buffer() // bzip2 압축된 데이터
 * val source = (compressed as okio.Source).bzip2Source()
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == 원본 데이터
 * ```
 */
fun okio.Source.bzip2Source(): DecompressableSource =
    decompressableSource(Compressors.BZip2)
