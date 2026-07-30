package io.bluetape4k.io.compressor

import org.xerial.snappy.Snappy
import org.xerial.snappy.SnappyError
import java.nio.BufferOverflowException
import java.nio.ByteBuffer

/**
 * Snappy의 caller-owned 버퍼 연산을 격리한 내부 어댑터입니다.
 *
 * 운영 구현은 안전한 direct/direct 조합을 Snappy의 `ByteBuffer` API에 위임합니다.
 * 테스트 구현은 backend 선택과 반환값·실패 경계를 검증합니다.
 */
internal interface SnappyBufferOperations {
    fun maxCompressedLength(sourceLength: Int): Int

    fun compress(source: ByteBuffer, target: ByteBuffer): Int

    fun isValidCompressedBuffer(source: ByteBuffer): Boolean

    fun uncompressedLength(source: ByteBuffer): Int

    fun decompress(source: ByteBuffer, target: ByteBuffer): Int
}

private object DefaultSnappyBufferOperations: SnappyBufferOperations {
    override fun maxCompressedLength(sourceLength: Int): Int =
        Snappy.maxCompressedLength(sourceLength)

    override fun compress(source: ByteBuffer, target: ByteBuffer): Int =
        Snappy.compress(source, target)

    override fun isValidCompressedBuffer(source: ByteBuffer): Boolean =
        Snappy.isValidCompressedBuffer(source)

    override fun uncompressedLength(source: ByteBuffer): Int =
        Snappy.uncompressedLength(source)

    override fun decompress(source: ByteBuffer, target: ByteBuffer): Int =
        Snappy.uncompress(source, target)
}

/**
 * Snappy 알고리즘을 사용한 Compressor
 *
 * Apache Commons Compress의 FramedSnappyCompressorOutputStream보다 훨씬 빠릅니다 (약 2배).
 * 낮은 지연 시간이 중요한 경우에 권장됩니다.
 *
 * caller-owned [ByteBuffer] 압축 API는 direct/direct 조합에서 target 여유 공간이
 * [Snappy.maxCompressedLength] 이상일 때 Snappy native `ByteBuffer` 연산을 사용합니다.
 * 압축 결과는 들어가지만 안전 상한보다 작은 target과 heap/mixed storage는 기존
 * allocating compatibility fallback을 유지합니다. 압축 해제는 복원 크기를 먼저
 * 확인할 수 있으므로 충분한 direct/direct target에서 native 경로를 사용합니다.
 * 이는 backend capability 설명이며 측정된 allocation 또는 처리량 개선을 주장하지
 * 않습니다.
 *
 * 압축은 [Snappy.maxCompressedLength]만큼 target 여유 공간이 있는지 native 호출 전에
 * 확인합니다. 압축 해제는 payload 전체를 검증하고 복원 크기와 256 MB 한도를 확인한
 * 뒤 target에 기록합니다.
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, Snappy!".toByteArray()
 * val compressed = Compressors.Snappy.compress(data)
 * val restored = Compressors.Snappy.decompress(compressed)
 * // restored contentEquals data == true
 * ```
 *
 * @see [FramedSnappyCompressor]
 * @throws IllegalArgumentException when source-size validation fails or exceeds the 256 MB limit.
 * @throws SnappyError when the Snappy native library or codec reports a state error.
 * @throws org.xerial.snappy.SnappyException when the Snappy codec reports a codec failure.
 */
class SnappyCompressor private constructor(
    private val bufferOperations: SnappyBufferOperations,
): AbstractCompressor() {

    constructor(): this(DefaultSnappyBufferOperations)

    companion object {
        private const val MAX_DECOMPRESSED_SIZE = 256 * 1024 * 1024  // 256 MB

        /**
         * backend 선택과 실패 경계를 검증하기 위한 내부 테스트 팩토리입니다.
         */
        internal fun forTesting(bufferOperations: SnappyBufferOperations): SnappyCompressor =
            SnappyCompressor(bufferOperations)
    }

    /**
     * [source]의 남은 데이터를 Snappy raw 형식으로 압축해 [target]에 기록합니다.
     * direct/direct 조합에서 안전 상한을 충족하면 native `ByteBuffer` 경로를 사용하고,
     * 그보다 작은 target이나 다른 storage 조합은 compatibility fallback으로 처리합니다.
     *
     * @return [target]에 기록한 압축 데이터 길이
     * @throws BufferOverflowException 실제 압축 결과를 target 여유 공간에 기록할 수 없는 경우
     */
    override fun compress(source: ByteBuffer, target: ByteBuffer): Int {
        if (!supportsOptimizedStoragePair(source, target)) {
            return super.compress(source, target)
        }
        return writeToCallerBufferViews(source, target) {
                sourceView,
                targetView,
                sourcePosition,
                sourceRemaining,
                targetPosition,
                targetRemaining,
            ->
            val maxCompressedLength = bufferOperations.maxCompressedLength(sourceRemaining)
            check(maxCompressedLength > 0) {
                "Snappy max compressed length must be positive: $maxCompressedLength"
            }
            if (maxCompressedLength > targetRemaining) {
                return@writeToCallerBufferViews writeFallback(sourceView, targetView) { bytes ->
                    compress(bytes)
                }
            }
            val input = sourceView
                .position(sourcePosition)
                .limit(sourcePosition + sourceRemaining)
            val output = targetView
                .position(targetPosition)
                .limit(targetPosition + maxCompressedLength)
            val written = bufferOperations.compress(input, output)
            check(written in 1..maxCompressedLength) {
                "Snappy compressed size out of range: written=$written, max=$maxCompressedLength"
            }
            written
        }
    }

    /**
     * [source]의 Snappy raw payload를 검증하고 [target]에 복원합니다.
     *
     * @return [target]에 기록한 복원 데이터 길이
     * @throws BufferOverflowException target 여유 공간이 복원 크기보다 작은 경우
     * @throws IllegalArgumentException payload가 유효하지 않거나 복원 크기 한도를 벗어난 경우
     */
    override fun decompress(source: ByteBuffer, target: ByteBuffer): Int {
        if (!supportsOptimizedStoragePair(source, target)) {
            return super.decompress(source, target)
        }
        return writeToCallerBufferViews(source, target) {
                sourceView,
                targetView,
                sourcePosition,
                sourceRemaining,
                targetPosition,
                targetRemaining,
            ->
            val input = sourceView
                .position(sourcePosition)
                .limit(sourcePosition + sourceRemaining)
            require(bufferOperations.isValidCompressedBuffer(input.duplicate())) {
                "유효하지 않은 Snappy payload입니다."
            }

            val uncompressedSize = bufferOperations.uncompressedLength(input.duplicate())
            validateUncompressedSize(uncompressedSize)
            if (uncompressedSize > targetRemaining) throw BufferOverflowException()

            val output = targetView
                .position(targetPosition)
                .limit(targetPosition + uncompressedSize)
            val written = bufferOperations.decompress(input.duplicate(), output)
            check(written == uncompressedSize) {
                "Snappy decompressed size mismatch: written=$written, expected=$uncompressedSize"
            }
            written
        }
    }

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        return Snappy.compress(plain)
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     *
     * 압축 해제 전에 원본 크기를 확인하여 과도한 메모리 할당을 방지합니다 (decompression bomb 방어).
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        val uncompressedSize = Snappy.uncompressedLength(compressed)
        validateUncompressedSize(uncompressedSize)
        return Snappy.uncompress(compressed)
    }

    private fun supportsOptimizedStoragePair(source: ByteBuffer, target: ByteBuffer): Boolean =
        source.isDirect && target.isDirect

    private fun validateUncompressedSize(uncompressedSize: Int) {
        require(uncompressedSize >= 0) {
            "uncompressedSize가 음수입니다. 손상된 데이터일 수 있습니다. uncompressedSize=$uncompressedSize"
        }
        require(uncompressedSize <= MAX_DECOMPRESSED_SIZE) {
            "uncompressedSize가 허용 한도(256MB)를 초과합니다. 손상되거나 악의적인 데이터일 수 있습니다. uncompressedSize=$uncompressedSize"
        }
    }
}
