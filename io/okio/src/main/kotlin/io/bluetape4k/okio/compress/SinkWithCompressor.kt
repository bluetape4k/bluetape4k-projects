package io.bluetape4k.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.compressor.StreamingCompressor

/**
 * [Compressor]를 사용해 압축하는 [CompressableSink]로 변환합니다.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = (output as okio.Sink).compressableSink(Compressors.GZip)
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * sink.close()
 * // output에는 GZip 압축된 데이터가 담겨 있다
 * ```
 */
fun okio.Sink.compressableSink(compressor: Compressor): CompressableSink =
    CompressableSink(this, compressor)

/**
 * [StreamingCompressor]를 사용해 스트리밍 방식으로 압축하는 [CompressableSink]로 변환합니다.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = (output as okio.Sink).compressableSink(Compressors.GZip as StreamingCompressor)
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * sink.close()
 * ```
 */
fun okio.Sink.compressableSink(compressor: StreamingCompressor): CompressableSink =
    StreamingCompressSink(this, compressor)

/**
 * Deflate 알고리즘으로 압축하는 [CompressableSink]로 변환합니다.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = (output as okio.Sink).deflateSink()
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * sink.close()
 * // output에는 Deflate 압축된 데이터가 담겨 있다
 * ```
 */
fun okio.Sink.deflateSink(): CompressableSink =
    compressableSink(Compressors.Deflate)

/**
 * GZip 알고리즘으로 압축하는 [CompressableSink]로 변환합니다.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = (output as okio.Sink).gzipSink()
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * sink.close()
 * // output에는 GZip 압축된 데이터가 담겨 있다
 * ```
 */
fun okio.Sink.gzipSink(): CompressableSink =
    compressableSink(Compressors.GZip)

/**
 * LZ4 알고리즘으로 압축하는 [CompressableSink]로 변환합니다.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = (output as okio.Sink).lz4Sink()
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * sink.close()
 * // output에는 LZ4 압축된 데이터가 담겨 있다
 * ```
 */
fun okio.Sink.lz4Sink(): CompressableSink =
    compressableSink(Compressors.LZ4)

/**
 * Snappy 알고리즘으로 압축하는 [CompressableSink]로 변환합니다.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = (output as okio.Sink).snappySink()
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * sink.close()
 * // output에는 Snappy 압축된 데이터가 담겨 있다
 * ```
 */
fun okio.Sink.snappySink(): CompressableSink =
    compressableSink(Compressors.Snappy)

/**
 * Zstd 알고리즘으로 압축하는 [CompressableSink]로 변환합니다.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = (output as okio.Sink).zstdSink()
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * sink.close()
 * // output에는 Zstd 압축된 데이터가 담겨 있다
 * ```
 */
fun okio.Sink.zstdSink(): CompressableSink =
    compressableSink(Compressors.Zstd)

/**
 * BZip2 알고리즘으로 압축하는 [CompressableSink]로 변환합니다.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = (output as okio.Sink).bzip2Sink()
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * sink.close()
 * // output에는 BZip2 압축된 데이터가 담겨 있다
 * ```
 */
fun okio.Sink.bzip2Sink(): CompressableSink =
    compressableSink(Compressors.BZip2)
