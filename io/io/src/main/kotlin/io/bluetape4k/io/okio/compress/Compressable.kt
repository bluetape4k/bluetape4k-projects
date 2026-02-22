package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.compressor.StreamingCompressor

/**
 * `Compressable` 싱글톤/유틸리티입니다.
 */
object Compressable {

    /**
     * `Sinks` 싱글톤/유틸리티입니다.
     */
    object Sinks {

        /**
         * Okio 압축/해제에서 `compressableSink` 함수를 제공합니다.
         */
        fun compressableSink(delegate: okio.Sink, compressor: Compressor): CompressableSink {
            return CompressableSink(delegate, compressor)
        }

        /**
         * Okio 압축/해제에서 `compressableSink` 함수를 제공합니다.
         */
        fun compressableSink(delegate: okio.Sink, compressor: StreamingCompressor): CompressableSink {
            return StreamingCompressSink(delegate, compressor)
        }

        /**
         * Okio 압축/해제에서 `deflate` 함수를 제공합니다.
         */
        fun deflate(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.Deflate)
        }

        /**
         * Okio 압축/해제에서 `gzip` 함수를 제공합니다.
         */
        fun gzip(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.GZip)
        }

        /**
         * Okio 압축/해제에서 `lz4` 함수를 제공합니다.
         */
        fun lz4(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.LZ4)
        }

        /**
         * Okio 압축/해제에서 `snappy` 함수를 제공합니다.
         */
        fun snappy(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.Snappy)
        }

        /**
         * Okio 압축/해제에서 `zstd` 함수를 제공합니다.
         */
        fun zstd(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.Zstd)
        }

        /**
         * Okio 압축/해제에서 `bzip2` 함수를 제공합니다.
         */
        fun bzip2(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.BZip2)
        }
    }

    /**
     * `Sources` 싱글톤/유틸리티입니다.
     */
    object Sources {

        /**
         * Okio 압축/해제에서 `decompressableSource` 함수를 제공합니다.
         */
        fun decompressableSource(delegate: okio.Source, compressor: Compressor): DecompressableSource {
            return DecompressableSource(delegate, compressor)
        }

        /**
         * Okio 압축/해제에서 `decompressableSource` 함수를 제공합니다.
         */
        fun decompressableSource(delegate: okio.Source, compressor: StreamingCompressor): DecompressableSource {
            return StreamingDecompressSource(delegate, compressor)
        }

        /**
         * Okio 압축/해제에서 `deflate` 함수를 제공합니다.
         */
        fun deflate(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.Deflate)
        }

        /**
         * Okio 압축/해제에서 `gzip` 함수를 제공합니다.
         */
        fun gzip(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.GZip)
        }

        /**
         * Okio 압축/해제에서 `lz4` 함수를 제공합니다.
         */
        fun lz4(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.LZ4)
        }

        /**
         * Okio 압축/해제에서 `snappy` 함수를 제공합니다.
         */
        fun snappy(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.Snappy)
        }

        /**
         * Okio 압축/해제에서 `zstd` 함수를 제공합니다.
         */
        fun zstd(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.Zstd)
        }

        /**
         * Okio 압축/해제에서 `bzip2` 함수를 제공합니다.
         */
        fun bzip2(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.BZip2)
        }
    }
}
