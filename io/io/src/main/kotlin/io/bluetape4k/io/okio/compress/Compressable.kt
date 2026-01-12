package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors

object Compressable {

    object Sinks {

        fun compressableSink(delegate: okio.Sink, compressor: Compressor): CompressableSink {
            return CompressableSink(delegate, compressor)
        }

        fun deflate(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.Deflate)
        }

        fun gzip(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.GZip)
        }

        fun lz4(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.LZ4)
        }

        fun snappy(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.Snappy)
        }

        fun zstd(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.Zstd)
        }

        fun bzip2(delegate: okio.Sink): CompressableSink {
            return compressableSink(delegate, Compressors.BZip2)
        }
    }

    object Sources {

        fun decompressableSource(delegate: okio.Source, compressor: Compressor): DecompressableSource {
            return DecompressableSource(delegate, compressor)
        }

        fun deflate(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.Deflate)
        }

        fun gzip(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.GZip)
        }

        fun lz4(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.LZ4)
        }

        fun snappy(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.Snappy)
        }

        fun zstd(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.Zstd)
        }

        fun bzip2(delegate: okio.Source): DecompressableSource {
            return decompressableSource(delegate, Compressors.BZip2)
        }
    }
}
