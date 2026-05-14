package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.isNullOrEmpty
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Base implementation for [Compressor].
 *
 * ## Null and empty input contract
 * - `compress(null)` and `compress(emptyByteArray)`: return an empty [ByteArray].
 * - `decompress(null)` and `decompress(emptyByteArray)`: return an empty [ByteArray].
 * - Compression and decompression failures are propagated to the caller.
 *
 * ## Failure handling
 * [compress] and [decompress] are throwing APIs. Use [compressOrNull] or
 * [decompressOrNull] when a failure should be represented as `null`.
 *
 * Common propagated failures are:
 * - [IOException] for stream or codec I/O failures.
 * - [IllegalArgumentException] for invalid input or defensive size-limit checks.
 * - Algorithm-specific runtime failures such as `net.jpountz.lz4.LZ4Exception`,
 *   `org.xerial.snappy.SnappyException`, `org.xerial.snappy.SnappyError`,
 *   `com.github.luben.zstd.ZstdException`, or `java.util.zip.ZipException`.
 *
 * Implementation classes document their codec-specific failure types.
 *
 * ## Implementation guide
 * Implement [doCompress] and [doDecompress]. This base class owns the null and
 * empty input checks.
 *
 * ```kotlin
 * class MyCompressor: AbstractCompressor() {
 *     override fun doCompress(plain: ByteArray): ByteArray = /* compression */
 *     override fun doDecompress(compressed: ByteArray): ByteArray = /* decompression */
 * }
 * ```
 *
 * @see Compressor
 * @see Compressors
 */
abstract class AbstractCompressor: Compressor {

    companion object: KLogging()

    protected abstract fun doCompress(plain: ByteArray): ByteArray
    protected abstract fun doDecompress(compressed: ByteArray): ByteArray

    /**
     * Compresses [plain] data.
     *
     * Null or empty input returns an empty [ByteArray]. Compression failures are
     * propagated to the caller. Use [compressOrNull] when failure should return `null`.
     *
     * ```
     * val compressor = GzipCompressor()
     * val compressed = compressor.compress("Hello, World!".toByteArray())
     * ```
     *
     * @param plain source data
     * @return compressed data
     * @throws IOException when the underlying codec fails during stream or buffer I/O
     * @throws IllegalArgumentException when input validation or defensive size-limit checks fail
     */
    override fun compress(plain: ByteArray?): ByteArray {
        if (plain.isNullOrEmpty()) return emptyByteArray
        return doCompress(plain!!)
    }

    /**
     * Decompresses [compressed] data.
     *
     * Null or empty input returns an empty [ByteArray]. Decompression failures are
     * propagated to the caller. Use [decompressOrNull] when corrupt input should
     * return `null`.
     *
     * ```
     * val compressor = GzipCompressor()
     * val compressed = compressor.compress("Hello, World!".toByteArray())
     * val plain = compressor.decompress(compressed)
     * ```
     *
     * @param compressed compressed data
     * @return decompressed data
     * @throws IOException when the underlying codec fails during stream or buffer I/O
     * @throws IllegalArgumentException when corrupt input, invalid headers, or defensive size-limit checks fail
     */
    override fun decompress(compressed: ByteArray?): ByteArray {
        if (compressed.isNullOrEmpty()) return emptyByteArray
        return doDecompress(compressed!!)
    }

    /**
     * Compresses [plain] and returns `null` for null/empty input or compression failure.
     *
     * Unlike [compress], this method represents failure as `null`.
     *
     * ```kotlin
     * val result = compressor.compressOrNull(corruptData)
     * if (result == null) {
     *     // 압축 실패 처리
     * }
     * ```
     *
     * @param plain source data
     * @return compressed data or `null`
     */
    fun compressOrNull(plain: ByteArray?): ByteArray? {
        if (plain.isNullOrEmpty()) return null
        return try {
            doCompress(plain!!)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "Fail to compress. plain size=${plain?.size}" }
            null
        }
    }

    /**
     * Decompresses [compressed] and returns `null` for null/empty input or decompression failure.
     *
     * Unlike [decompress], this method represents failure as `null`.
     *
     * ```kotlin
     * val result = compressor.decompressOrNull(suspectData)
     * if (result == null) {
     *     // 손상 데이터 처리
     * }
     * ```
     *
     * @param compressed compressed data
     * @return decompressed data or `null`
     */
    fun decompressOrNull(compressed: ByteArray?): ByteArray? {
        if (compressed.isNullOrEmpty()) return null
        return try {
            doDecompress(compressed!!)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "Fail to decompress. compressed size=${compressed?.size}" }
            null
        }
    }
}
