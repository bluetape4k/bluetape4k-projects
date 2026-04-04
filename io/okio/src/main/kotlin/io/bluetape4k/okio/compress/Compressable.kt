package io.bluetape4k.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.compressor.StreamingCompressor

/**
 * 압축 [Sink]와 압축 해제 [okio.Source]를 생성하는 팩토리 유틸리티 싱글톤입니다.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = Compressable.Sinks.gzip(output)
 * val plainSource = bufferOf("hello world")
 * sink.write(plainSource, plainSource.size)
 * sink.close()
 * // output에는 gzip 압축된 데이터가 담겨 있다
 * ```
 */
object Compressable {

    /**
     * 압축 [Sink]를 생성하는 팩토리 유틸리티입니다.
     *
     * ```kotlin
     * val output = Buffer()
     * val sink = Compressable.Sinks.lz4(output)
     * val source = bufferOf("hello")
     * sink.write(source, source.size)
     * sink.close()
     * // output에는 LZ4 압축된 데이터가 담겨 있다
     * ```
     */
    object Sinks {

        /**
         * [Compressor]를 사용해 압축하는 [CompressableSink]를 생성합니다.
         *
         * ```kotlin
         * val output = Buffer()
         * val sink = Compressable.Sinks.compressableSink(output, Compressors.GZip)
         * val source = bufferOf("hello")
         * sink.write(source, source.size)
         * sink.close()
         * ```
         */
        fun compressableSink(delegate: okio.Sink, compressor: Compressor): CompressableSink {
            return CompressableSink(delegate, compressor)
        }

        /**
         * [StreamingCompressor]를 사용해 스트리밍 방식으로 압축하는 [CompressableSink]를 생성합니다.
         *
         * ```kotlin
         * val output = Buffer()
         * val sink = Compressable.Sinks.compressableSink(output, Compressors.GZip as StreamingCompressor)
         * val source = bufferOf("hello")
         * sink.write(source, source.size)
         * sink.close()
         * ```
         */
        fun compressableSink(delegate: okio.Sink, compressor: StreamingCompressor): CompressableSink {
            return StreamingCompressSink(delegate, compressor)
        }

        /**
         * Deflate 알고리즘으로 압축하는 [CompressableSink]를 생성합니다.
         *
         * ```kotlin
         * val output = Buffer()
         * val sink = Compressable.Sinks.deflate(output)
         * val source = bufferOf("hello")
         * sink.write(source, source.size)
         * sink.close()
         * // output에는 Deflate 압축된 데이터가 담겨 있다
         * ```
         */
        fun deflate(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.Deflate)
        }

        /**
         * GZip 알고리즘으로 압축하는 [CompressableSink]를 생성합니다.
         *
         * ```kotlin
         * val output = Buffer()
         * val sink = Compressable.Sinks.gzip(output)
         * val source = bufferOf("hello")
         * sink.write(source, source.size)
         * sink.close()
         * // output에는 GZip 압축된 데이터가 담겨 있다
         * ```
         */
        fun gzip(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.GZip)
        }

        /**
         * LZ4 알고리즘으로 압축하는 [CompressableSink]를 생성합니다.
         *
         * ```kotlin
         * val output = Buffer()
         * val sink = Compressable.Sinks.lz4(output)
         * val source = bufferOf("hello")
         * sink.write(source, source.size)
         * sink.close()
         * // output에는 LZ4 압축된 데이터가 담겨 있다
         * ```
         */
        fun lz4(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.LZ4)
        }

        /**
         * Snappy 알고리즘으로 압축하는 [CompressableSink]를 생성합니다.
         *
         * ```kotlin
         * val output = Buffer()
         * val sink = Compressable.Sinks.snappy(output)
         * val source = bufferOf("hello")
         * sink.write(source, source.size)
         * sink.close()
         * // output에는 Snappy 압축된 데이터가 담겨 있다
         * ```
         */
        fun snappy(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.Snappy)
        }

        /**
         * Zstd 알고리즘으로 압축하는 [CompressableSink]를 생성합니다.
         *
         * ```kotlin
         * val output = Buffer()
         * val sink = Compressable.Sinks.zstd(output)
         * val source = bufferOf("hello")
         * sink.write(source, source.size)
         * sink.close()
         * // output에는 Zstd 압축된 데이터가 담겨 있다
         * ```
         */
        fun zstd(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.Zstd)
        }

        /**
         * BZip2 알고리즘으로 압축하는 [CompressableSink]를 생성합니다.
         *
         * ```kotlin
         * val output = Buffer()
         * val sink = Compressable.Sinks.bzip2(output)
         * val source = bufferOf("hello")
         * sink.write(source, source.size)
         * sink.close()
         * // output에는 BZip2 압축된 데이터가 담겨 있다
         * ```
         */
        fun bzip2(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.BZip2)
        }
    }

    /**
     * 압축 해제 [okio.Source]를 생성하는 팩토리 유틸리티입니다.
     *
     * ```kotlin
     * val compressed = Buffer() // 이미 압축된 데이터
     * val source = Compressable.Sources.lz4(compressed)
     * val sink = Buffer()
     * source.read(sink, Long.MAX_VALUE)
     * val text = sink.readUtf8()
     * // text == 원본 데이터
     * ```
     */
    object Sources {

        /**
         * [Compressor]를 사용해 압축 해제하는 [DecompressableSource]를 생성합니다.
         *
         * ```kotlin
         * val compressed = Buffer() // 이미 압축된 데이터
         * val source = Compressable.Sources.decompressableSource(compressed, Compressors.GZip)
         * val sink = Buffer()
         * source.read(sink, Long.MAX_VALUE)
         * ```
         */
        fun decompressableSource(delegate: okio.Source, compressor: Compressor): DecompressableSource {
            return DecompressableSource(delegate, compressor)
        }

        /**
         * [StreamingCompressor]를 사용해 스트리밍 방식으로 압축 해제하는 [DecompressableSource]를 생성합니다.
         *
         * ```kotlin
         * val compressed = Buffer() // 이미 압축된 데이터
         * val source = Compressable.Sources.decompressableSource(
         *     compressed, Compressors.GZip as StreamingCompressor)
         * val sink = Buffer()
         * source.read(sink, Long.MAX_VALUE)
         * ```
         */
        fun decompressableSource(delegate: okio.Source, compressor: StreamingCompressor): DecompressableSource {
            return StreamingDecompressSource(delegate, compressor)
        }

        /**
         * Deflate 압축된 데이터를 해제하는 [DecompressableSource]를 생성합니다.
         *
         * ```kotlin
         * val compressed = Buffer() // deflate 압축된 데이터
         * val source = Compressable.Sources.deflate(compressed)
         * val sink = Buffer()
         * source.read(sink, Long.MAX_VALUE)
         * val text = sink.readUtf8()
         * // text == 원본 데이터
         * ```
         */
        fun deflate(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.Deflate)
        }

        /**
         * GZip 압축된 데이터를 해제하는 [DecompressableSource]를 생성합니다.
         *
         * ```kotlin
         * val compressed = Buffer() // gzip 압축된 데이터
         * val source = Compressable.Sources.gzip(compressed)
         * val sink = Buffer()
         * source.read(sink, Long.MAX_VALUE)
         * val text = sink.readUtf8()
         * // text == 원본 데이터
         * ```
         */
        fun gzip(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.GZip)
        }

        /**
         * LZ4 압축된 데이터를 해제하는 [DecompressableSource]를 생성합니다.
         *
         * ```kotlin
         * val compressed = Buffer() // lz4 압축된 데이터
         * val source = Compressable.Sources.lz4(compressed)
         * val sink = Buffer()
         * source.read(sink, Long.MAX_VALUE)
         * val text = sink.readUtf8()
         * // text == 원본 데이터
         * ```
         */
        fun lz4(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.LZ4)
        }

        /**
         * Snappy 압축된 데이터를 해제하는 [DecompressableSource]를 생성합니다.
         *
         * ```kotlin
         * val compressed = Buffer() // snappy 압축된 데이터
         * val source = Compressable.Sources.snappy(compressed)
         * val sink = Buffer()
         * source.read(sink, Long.MAX_VALUE)
         * val text = sink.readUtf8()
         * // text == 원본 데이터
         * ```
         */
        fun snappy(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.Snappy)
        }

        /**
         * Zstd 압축된 데이터를 해제하는 [DecompressableSource]를 생성합니다.
         *
         * ```kotlin
         * val compressed = Buffer() // zstd 압축된 데이터
         * val source = Compressable.Sources.zstd(compressed)
         * val sink = Buffer()
         * source.read(sink, Long.MAX_VALUE)
         * val text = sink.readUtf8()
         * // text == 원본 데이터
         * ```
         */
        fun zstd(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.Zstd)
        }

        /**
         * BZip2 압축된 데이터를 해제하는 [DecompressableSource]를 생성합니다.
         *
         * ```kotlin
         * val compressed = Buffer() // bzip2 압축된 데이터
         * val source = Compressable.Sources.bzip2(compressed)
         * val sink = Buffer()
         * source.read(sink, Long.MAX_VALUE)
         * val text = sink.readUtf8()
         * // text == 원본 데이터
         * ```
         */
        fun bzip2(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.BZip2)
        }
    }
}
