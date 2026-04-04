package io.bluetape4k.io.compressor

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorOutputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.InflaterInputStream

/**
 * 다양한 [Compressor] 구현체를 지연 초기화(lazy)하여 제공하는 싱글톤 레지스트리입니다.
 *
 * 압축기를 직접 인스턴스화하지 않고 이 레지스트리를 통해 공유 인스턴스를 사용하세요.
 * 모든 프로퍼티는 최초 접근 시 한 번만 초기화되며, 이후 재사용됩니다.
 *
 * 예제:
 * ```kotlin
 * val data = "Hello, bluetape4k!".toByteArray()
 *
 * // LZ4: 빠른 압축 (일반 목적으로 기본 권장)
 * val lz4Compressed = Compressors.LZ4.compress(data)
 * val lz4Restored = Compressors.LZ4.decompress(lz4Compressed)
 * // lz4Restored contentEquals data == true
 *
 * // Zstd: 높은 압축률이 필요한 경우
 * val zstdCompressed = Compressors.Zstd.compress(data)
 * val zstdRestored = Compressors.Zstd.decompress(zstdCompressed)
 *
 * // Snappy: 낮은 지연 시간이 중요한 경우
 * val snappyCompressed = Compressors.Snappy.compress(data)
 * val snappyRestored = Compressors.Snappy.decompress(snappyCompressed)
 *
 * // GZip: 표준 호환성이 필요한 경우
 * val gzipCompressed = Compressors.GZip.compress(data)
 * val gzipRestored = Compressors.GZip.decompress(gzipCompressed)
 * ```
 */
object Compressors {

    /**
     * Apache Commons Compress 기반 Deflate 압축기입니다.
     *
     * 예제:
     * ```kotlin
     * val data = "compress me".toByteArray()
     * val compressed = Compressors.ApacheDeflate.compress(data)
     * val restored = Compressors.ApacheDeflate.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val ApacheDeflate: ApacheDeflateCompressor by lazy { ApacheDeflateCompressor() }

    /**
     * JDK 표준 `DeflaterOutputStream` 기반 Deflate 압축기입니다.
     *
     * 예제:
     * ```kotlin
     * val data = "compress me".toByteArray()
     * val compressed = Compressors.Deflate.compress(data)
     * val restored = Compressors.Deflate.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val Deflate: DeflateCompressor by lazy { DeflateCompressor() }

    /**
     * Apache Commons Compress 기반 GZip 압축기입니다.
     *
     * 예제:
     * ```kotlin
     * val data = "compress me".toByteArray()
     * val compressed = Compressors.ApacheGZip.compress(data)
     * val restored = Compressors.ApacheGZip.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val ApacheGZip: ApacheGZipCompressor by lazy { ApacheGZipCompressor() }

    /**
     * JDK 표준 `GZIPOutputStream` 기반 GZip 압축기입니다.
     * 표준 gzip 형식과 호환되어 외부 시스템과의 파일 교환에 적합합니다.
     *
     * 예제:
     * ```kotlin
     * val data = "compress me".toByteArray()
     * val compressed = Compressors.GZip.compress(data)
     * val restored = Compressors.GZip.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val GZip: GZipCompressor by lazy { GZipCompressor() }

    /**
     * LZ4 압축기입니다. 압축/해제 속도가 매우 빠르며 일반 목적으로 권장됩니다.
     *
     * 예제:
     * ```kotlin
     * val data = "Hello, LZ4!".toByteArray()
     * val compressed = Compressors.LZ4.compress(data)
     * val restored = Compressors.LZ4.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val LZ4: LZ4Compressor by lazy { LZ4Compressor() }

    /**
     * Apache Commons Compress 기반 Block LZ4 압축기입니다.
     * 블록 단위로 압축하며 `FramedLZ4`보다 오버헤드가 적습니다.
     *
     * 예제:
     * ```kotlin
     * val data = "Hello, BlockLZ4!".toByteArray()
     * val compressed = Compressors.BlockLZ4.compress(data)
     * val restored = Compressors.BlockLZ4.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val BlockLZ4: BlockLZ4Compressor by lazy { BlockLZ4Compressor() }

    /**
     * Apache Commons Compress 기반 Framed LZ4 압축기입니다.
     * LZ4 프레임 포맷을 사용하여 스트리밍 처리에 적합합니다.
     *
     * 예제:
     * ```kotlin
     * val data = "Hello, FramedLZ4!".toByteArray()
     * val compressed = Compressors.FramedLZ4.compress(data)
     * val restored = Compressors.FramedLZ4.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val FramedLZ4: FramedLZ4Compressor by lazy { FramedLZ4Compressor() }

    /**
     * Google Snappy 압축기입니다. 낮은 지연 시간이 중요한 경우에 적합합니다.
     *
     * 예제:
     * ```kotlin
     * val data = "Hello, Snappy!".toByteArray()
     * val compressed = Compressors.Snappy.compress(data)
     * val restored = Compressors.Snappy.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val Snappy: SnappyCompressor by lazy { SnappyCompressor() }

    /**
     * Apache Commons Compress 기반 Framed Snappy 압축기입니다.
     * Snappy 프레임 포맷을 사용하여 스트리밍 처리에 적합합니다.
     *
     * 예제:
     * ```kotlin
     * val data = "Hello, FramedSnappy!".toByteArray()
     * val compressed = Compressors.FramedSnappy.compress(data)
     * val restored = Compressors.FramedSnappy.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val FramedSnappy: FramedSnappyCompressor by lazy { FramedSnappyCompressor() }

    /**
     * Apache Commons Compress 기반 Zstd 압축기입니다.
     *
     * 예제:
     * ```kotlin
     * val data = "Hello, ApacheZstd!".toByteArray()
     * val compressed = Compressors.ApacheZstd.compress(data)
     * val restored = Compressors.ApacheZstd.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val ApacheZstd: ApacheZstdCompressor by lazy { ApacheZstdCompressor() }

    /**
     * Zstd(Zstandard) 압축기입니다. 높은 압축률과 빠른 속도를 동시에 제공합니다.
     * 대용량 데이터 압축이나 네트워크 전송 최적화에 적합합니다.
     *
     * 예제:
     * ```kotlin
     * val data = "Hello, Zstd!".toByteArray()
     * val compressed = Compressors.Zstd.compress(data)
     * val restored = Compressors.Zstd.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val Zstd: ZstdCompressor by lazy { ZstdCompressor() }

    /**
     * BZip2 압축기입니다. 높은 압축률을 제공하지만 속도는 느립니다.
     * 압축률이 가장 중요하고 처리 시간에 여유가 있는 경우에 적합합니다.
     *
     * 예제:
     * ```kotlin
     * val data = "Hello, BZip2!".toByteArray()
     * val compressed = Compressors.BZip2.compress(data)
     * val restored = Compressors.BZip2.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val BZip2: BZip2Compressor by lazy { BZip2Compressor() }

    /**
     * ZIP 포맷 압축기입니다. 범용 아카이브 포맷으로 파일 배포 및 호환성에 적합합니다.
     *
     * 예제:
     * ```kotlin
     * val data = "Hello, Zip!".toByteArray()
     * val compressed = Compressors.Zip.compress(data)
     * val restored = Compressors.Zip.decompress(compressed)
     * // restored contentEquals data == true
     * ```
     */
    val Zip: ZipCompressor by lazy { ZipCompressor() }

    /**
     * 스트리밍 압축기 모음입니다.
     *
     * stream 래퍼를 제공하지 않는 알고리즘은 one-shot 어댑터를 사용합니다.
     *
     * 예제:
     * ```kotlin
     * val input = ByteArrayInputStream("Hello, Streaming!".toByteArray())
     * val output = ByteArrayOutputStream()
     *
     * // LZ4 스트리밍 압축
     * Compressors.Streaming.LZ4.compress(input, output)
     * val compressed = output.toByteArray()
     *
     * // LZ4 스트리밍 해제
     * val compressedInput = ByteArrayInputStream(compressed)
     * val decompressedOutput = ByteArrayOutputStream()
     * Compressors.Streaming.LZ4.decompress(compressedInput, decompressedOutput)
     * // decompressedOutput.toByteArray() contentEquals "Hello, Streaming!".toByteArray() == true
     * ```
     */
    object Streaming {
        /**
         * Apache Commons Compress 기반 스트리밍 Deflate 압축기입니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.ApacheDeflate.compress(input, output)
         * ```
         */
        val ApacheDeflate: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { DeflateCompressorOutputStream(it) },
                decompressing = { DeflateCompressorInputStream(it) }
            )
        }

        /**
         * JDK 표준 스트리밍 Deflate 압축기입니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.Deflate.compress(input, output)
         * ```
         */
        val Deflate: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { DeflaterOutputStream(it) },
                decompressing = { InflaterInputStream(it) }
            )
        }

        /**
         * Apache Commons Compress 기반 스트리밍 GZip 압축기입니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.ApacheGZip.compress(input, output)
         * ```
         */
        val ApacheGZip: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { GzipCompressorOutputStream(it) },
                decompressing = { GzipCompressorInputStream(it) }
            )
        }

        /**
         * JDK 표준 스트리밍 GZip 압축기입니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.GZip.compress(input, output)
         * ```
         */
        val GZip: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { GZIPOutputStream(it) },
                decompressing = { GZIPInputStream(it) }
            )
        }

        /**
         * LZ4 기반 스트리밍 압축기입니다. `Compressors.LZ4`를 one-shot 어댑터로 래핑합니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.LZ4.compress(input, output)
         * ```
         */
        val LZ4: StreamingCompressor by lazy { StreamingCompressors.from(Compressors.LZ4) }

        /**
         * Apache Commons Compress 기반 스트리밍 Block LZ4 압축기입니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.BlockLZ4.compress(input, output)
         * ```
         */
        val BlockLZ4: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { BlockLZ4CompressorOutputStream(it) },
                decompressing = { BlockLZ4CompressorInputStream(it) }
            )
        }

        /**
         * Apache Commons Compress 기반 스트리밍 Framed LZ4 압축기입니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.FramedLZ4.compress(input, output)
         * ```
         */
        val FramedLZ4: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { FramedLZ4CompressorOutputStream(it) },
                decompressing = { FramedLZ4CompressorInputStream(it) }
            )
        }

        /**
         * Snappy 기반 스트리밍 압축기입니다. `Compressors.Snappy`를 one-shot 어댑터로 래핑합니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.Snappy.compress(input, output)
         * ```
         */
        val Snappy: StreamingCompressor by lazy { StreamingCompressors.from(Compressors.Snappy) }

        /**
         * Apache Commons Compress 기반 스트리밍 Framed Snappy 압축기입니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.FramedSnappy.compress(input, output)
         * ```
         */
        val FramedSnappy: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { FramedSnappyCompressorOutputStream(it) },
                decompressing = { FramedSnappyCompressorInputStream(it) }
            )
        }

        /**
         * Apache Commons Compress 기반 스트리밍 Zstd 압축기입니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.ApacheZstd.compress(input, output)
         * ```
         */
        val ApacheZstd: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { ZstdCompressorOutputStream(it) },
                decompressing = { ZstdCompressorInputStream(it) }
            )
        }

        /**
         * Zstd 기반 스트리밍 압축기입니다. `Compressors.Zstd`를 one-shot 어댑터로 래핑합니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.Zstd.compress(input, output)
         * ```
         */
        val Zstd: StreamingCompressor by lazy { StreamingCompressors.from(Compressors.Zstd) }

        /**
         * Apache Commons Compress 기반 스트리밍 BZip2 압축기입니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.BZip2.compress(input, output)
         * ```
         */
        val BZip2: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { BZip2CompressorOutputStream(it) },
                decompressing = { BZip2CompressorInputStream(it) }
            )
        }

        /**
         * ZIP 기반 스트리밍 압축기입니다. `Compressors.Zip`을 one-shot 어댑터로 래핑합니다.
         *
         * 예제:
         * ```kotlin
         * val input = ByteArrayInputStream("data".toByteArray())
         * val output = ByteArrayOutputStream()
         * Compressors.Streaming.Zip.compress(input, output)
         * ```
         */
        val Zip: StreamingCompressor by lazy { StreamingCompressors.from(Compressors.Zip) }
    }
}
