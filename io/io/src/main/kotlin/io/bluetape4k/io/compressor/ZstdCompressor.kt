package io.bluetape4k.io.compressor

import com.github.luben.zstd.Zstd
import com.github.luben.zstd.ZstdException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.toByteArray
import io.bluetape4k.support.toInt
import org.apache.commons.compress.compressors.zstandard.ZstdUtils
import java.nio.BufferOverflowException
import java.nio.ByteBuffer

/**
 * Zstd의 배열 및 direct buffer offset API를 격리한 내부 연산 계약입니다.
 *
 * offset은 각 저장소의 시작점을 기준으로 한 절대 위치이며, [targetLength]는 codec이 기록할 수
 * 있는 최대 길이입니다. 테스트에서는 이 경계를 사용해 JNI 호출 전 검증과 반환값 처리를 확인합니다.
 */
internal interface ZstdBufferOperations {
    /** direct source를 direct target에 압축하고 기록량을 반환합니다. */
    fun compressDirect(
        target: ByteBuffer,
        targetOffset: Int,
        targetLength: Int,
        source: ByteBuffer,
        sourceOffset: Int,
        sourceLength: Int,
        level: Int,
    ): Long

    /** direct source를 direct target에 복원하고 기록량을 반환합니다. */
    fun decompressDirect(
        target: ByteBuffer,
        targetOffset: Int,
        targetLength: Int,
        source: ByteBuffer,
        sourceOffset: Int,
        sourceLength: Int,
    ): Long

    /** 배열 source를 배열 target에 압축하고 기록량을 반환합니다. */
    fun compressHeap(
        target: ByteArray,
        targetOffset: Int,
        targetLength: Int,
        source: ByteArray,
        sourceOffset: Int,
        sourceLength: Int,
        level: Int,
    ): Long

    /** 배열 source를 배열 target에 복원하고 기록량을 반환합니다. */
    fun decompressHeap(
        target: ByteArray,
        targetOffset: Int,
        targetLength: Int,
        source: ByteArray,
        sourceOffset: Int,
        sourceLength: Int,
    ): Long
}

/** zstd-jni 정적 offset API에 직접 위임하는 운영 어댑터입니다. */
private object DefaultZstdBufferOperations: ZstdBufferOperations {
    override fun compressDirect(
        target: ByteBuffer,
        targetOffset: Int,
        targetLength: Int,
        source: ByteBuffer,
        sourceOffset: Int,
        sourceLength: Int,
        level: Int,
    ): Long = Zstd.compressDirectByteBuffer(
        target,
        targetOffset,
        targetLength,
        source,
        sourceOffset,
        sourceLength,
        level,
    )

    override fun decompressDirect(
        target: ByteBuffer,
        targetOffset: Int,
        targetLength: Int,
        source: ByteBuffer,
        sourceOffset: Int,
        sourceLength: Int,
    ): Long = Zstd.decompressDirectByteBuffer(
        target,
        targetOffset,
        targetLength,
        source,
        sourceOffset,
        sourceLength,
    )

    override fun compressHeap(
        target: ByteArray,
        targetOffset: Int,
        targetLength: Int,
        source: ByteArray,
        sourceOffset: Int,
        sourceLength: Int,
        level: Int,
    ): Long = Zstd.compressByteArray(
        target,
        targetOffset,
        targetLength,
        source,
        sourceOffset,
        sourceLength,
        level,
    )

    override fun decompressHeap(
        target: ByteArray,
        targetOffset: Int,
        targetLength: Int,
        source: ByteArray,
        sourceOffset: Int,
        sourceLength: Int,
    ): Long = Zstd.decompressByteArray(
        target,
        targetOffset,
        targetLength,
        source,
        sourceOffset,
        sourceLength,
    )
}

/**
 * zstd-jni 라이브러리를 사용하여 Zstd 알고리즘을 활용한 압축기
 *
 * 높은 압축률과 빠른 속도를 동시에 제공하여 대용량 데이터 압축이나 네트워크 전송 최적화에 적합합니다.
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, Zstd!".toByteArray()
 * val compressed = Compressors.Zstd.compress(data)
 * val restored = Compressors.Zstd.decompress(compressed)
 * // restored contentEquals data == true
 * ```
 *
 * 압축 레벨을 지정하려면 직접 인스턴스화하세요:
 * ```kotlin
 * val compressor = ZstdCompressor(level = 10)
 * ```
 *
 * 압축 데이터의 header에 선언된 원본 크기와 zstd-jni가 실제로 복원한 크기는 정확히
 * 일치해야 합니다. `ByteArray`와 `ByteBuffer` API는 저장소 유형과 관계없이 이 계약을
 * 동일하게 적용하며, 불일치하면 [IllegalStateException]을 던집니다. 비어 있지 않은
 * `ByteBuffer` 압축에서 native codec이 반환하는 기록량은 양수여야 하며, `0`은 유효한
 * 성공 결과로 취급하지 않습니다.
 *
 * 참고: [zstd-jni](https://github.com/luben/zstd-jni)
 *
 * @property level 압축 레벨
 * @throws IllegalArgumentException 저장된 원본 크기가 음수이거나 256 MiB 한도를 초과할 때
 * @throws IllegalStateException 압축 해제 결과가 저장된 원본 크기와 일치하지 않을 때
 * @throws BufferOverflowException 호출자가 제공한 target 공간이 부족할 때
 * @throws ZstdException zstd-jni codec 처리에 실패할 때
 */
class ZstdCompressor private constructor(
    val level: Int,
    private val bufferOperations: ZstdBufferOperations,
): AbstractCompressor() {

    companion object: KLogging() {
        private const val MAGIC_NUMBER_SIZE = Int.SIZE_BYTES
        private const val MAX_DECOMPRESSED_SIZE = 256 * 1024 * 1024
        const val DEFAULT_LEVEL: Int = 3

        /**
         * I/O 압축용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(level: Int = DEFAULT_LEVEL): ZstdCompressor {
            val cLevel = level.coerceIn(Zstd.minCompressionLevel(), Zstd.maxCompressionLevel())
            ZstdUtils.setCacheZstdAvailablity(true)
            return ZstdCompressor(cLevel, DefaultZstdBufferOperations)
        }

        internal fun forTesting(level: Int, bufferOperations: ZstdBufferOperations): ZstdCompressor =
            ZstdCompressor(level, bufferOperations)
    }

    /**
     * 호출자가 제공한 [target]에 [source]의 압축 결과를 기록합니다.
     *
     * heap끼리 또는 direct끼리 조합하고 target이 `compressBound + 4` 안전 상한을 수용하면
     * zstd-jni offset API를 사용합니다. 그 밖의 조합은 호환성 경로로 처리합니다. 성공할 때만
     * target position을 기록량만큼 이동합니다.
     */
    override fun compress(source: ByteBuffer, target: ByteBuffer): Int =
        when {
            source.hasArray() && target.hasArray() && hasNativeCompressionCapacity(source, target) ->
                compressOptimized(source, target, direct = false)
            source.isDirect && target.isDirect && hasNativeCompressionCapacity(source, target) ->
                compressOptimized(source, target, direct = true)
            else -> super.compress(source, target)
        }

    private fun hasNativeCompressionCapacity(source: ByteBuffer, target: ByteBuffer): Boolean {
        if (target.remaining() < MAGIC_NUMBER_SIZE) return false
        val payloadCapacity = target.remaining() - MAGIC_NUMBER_SIZE
        return payloadCapacity.toLong() >= Zstd.compressBound(source.remaining().toLong())
    }

    /**
     * [source]의 압축 데이터를 호출자가 제공한 [target]에 복원합니다.
     *
     * heap끼리 또는 direct끼리 조합하면 zstd-jni offset API를 사용합니다. 그 밖의 조합은
     * 호환성 경로로 처리합니다. native 출력 길이는 wire header에 선언된 크기로 제한합니다.
     */
    override fun decompress(source: ByteBuffer, target: ByteBuffer): Int =
        when {
            source.hasArray() && target.hasArray() -> decompressOptimized(source, target, direct = false)
            source.isDirect && target.isDirect -> decompressOptimized(source, target, direct = true)
            else -> super.decompress(source, target)
        }

    private fun compressOptimized(source: ByteBuffer, target: ByteBuffer, direct: Boolean): Int =
        writeToCallerBufferViews(source, target) {
                sourceView,
                targetView,
                sourcePosition,
                sourceRemaining,
                targetPosition,
                targetRemaining,
            ->
            if (targetRemaining < MAGIC_NUMBER_SIZE) throw BufferOverflowException()

            putIntBigEndian(targetView, targetPosition, sourceRemaining)
            val payloadCapacity = targetRemaining - MAGIC_NUMBER_SIZE
            val payloadWritten = try {
                if (direct) {
                    bufferOperations.compressDirect(
                        targetView,
                        targetPosition + MAGIC_NUMBER_SIZE,
                        payloadCapacity,
                        sourceView,
                        sourcePosition,
                        sourceRemaining,
                        level,
                    )
                } else {
                    bufferOperations.compressHeap(
                        targetView.array(),
                        targetView.arrayOffset() + targetPosition + MAGIC_NUMBER_SIZE,
                        payloadCapacity,
                        sourceView.array(),
                        sourceView.arrayOffset() + sourcePosition,
                        sourceRemaining,
                        level,
                    )
                }
            } catch (failure: ZstdException) {
                if (failure.errorCode == Zstd.errDstSizeTooSmall()) throw BufferOverflowException()
                throw failure
            }
            check(payloadWritten in 1L..payloadCapacity.toLong()) {
                "Zstd compression returned invalid size=$payloadWritten, payloadCapacity=$payloadCapacity"
            }
            Math.addExact(MAGIC_NUMBER_SIZE, Math.toIntExact(payloadWritten))
        }

    private fun decompressOptimized(source: ByteBuffer, target: ByteBuffer, direct: Boolean): Int =
        writeToCallerBufferViews(source, target) {
                sourceView,
                targetView,
                sourcePosition,
                sourceRemaining,
                targetPosition,
                targetRemaining,
            ->
            if (sourceRemaining < MAGIC_NUMBER_SIZE) {
                throw IndexOutOfBoundsException("Zstd header requires 4 bytes")
            }

            val declaredSize = getIntBigEndian(sourceView, sourcePosition)
            require(declaredSize >= 0) { "sourceSize must not be negative: $declaredSize" }
            require(declaredSize <= MAX_DECOMPRESSED_SIZE) {
                "sourceSize exceeds 256 MiB: $declaredSize"
            }
            if (declaredSize > targetRemaining) throw BufferOverflowException()

            val payloadOffset = sourcePosition + MAGIC_NUMBER_SIZE
            val payloadLength = sourceRemaining - MAGIC_NUMBER_SIZE
            val actual = try {
                if (direct) {
                    bufferOperations.decompressDirect(
                        targetView,
                        targetPosition,
                        declaredSize,
                        sourceView,
                        payloadOffset,
                        payloadLength,
                    )
                } else {
                    bufferOperations.decompressHeap(
                        targetView.array(),
                        targetView.arrayOffset() + targetPosition,
                        declaredSize,
                        sourceView.array(),
                        sourceView.arrayOffset() + payloadOffset,
                        payloadLength,
                    )
                }
            } catch (failure: ZstdException) {
                if (failure.errorCode == Zstd.errDstSizeTooSmall()) {
                    throw decompressedPayloadExceedsDeclaredSize(declaredSize)
                }
                throw failure
            }
            requireExactDecompressedSize(declaredSize, actual)
        }

    private fun decompressedPayloadExceedsDeclaredSize(declaredSize: Int): IllegalStateException =
        IllegalStateException("Zstd decompressed payload exceeds declared size=$declaredSize")

    private fun requireExactDecompressedSize(declaredSize: Int, actualSize: Long): Int {
        if (actualSize != declaredSize.toLong()) {
            throw IllegalStateException(
                "Zstd decompressed size mismatch: expected=$declaredSize, actual=$actualSize"
            )
        }
        return Math.toIntExact(actualSize)
    }

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val sourceSize = plain.size
        val maxOutputSize = Zstd.compressBound(sourceSize.toLong()).toInt()

        val output = ByteArray(MAGIC_NUMBER_SIZE + maxOutputSize)
        sourceSize.toByteArray().copyInto(output, 0)

        val compressedSize = Zstd.compressByteArray(
            output,
            MAGIC_NUMBER_SIZE,
            maxOutputSize,      // output 배열에서 MAGIC_NUMBER_SIZE 이후 사용 가능한 최대 공간
            plain,
            0,
            plain.size,
            level
        )

        check(!Zstd.isError(compressedSize)) { "Zstd compression failed: ${Zstd.getErrorName(compressedSize)}" }
        return output.copyOf(MAGIC_NUMBER_SIZE + compressedSize.toInt())
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        val sourceSize = compressed.toInt()
        require(sourceSize >= 0) { "sourceSize가 음수입니다. 손상된 데이터일 수 있습니다. sourceSize=$sourceSize" }
        require(sourceSize <= 256 * 1024 * 1024) {
            "sourceSize가 허용 한도(256MB)를 초과합니다. 손상되거나 악의적인 데이터일 수 있습니다. sourceSize=$sourceSize"
        }
        val output = ByteArray(sourceSize)

        log.trace { "sourceSize = $sourceSize" }

        val actualSize = try {
            Zstd.decompressByteArray(
                output,
                0,
                output.size,
                compressed,
                MAGIC_NUMBER_SIZE,
                compressed.size - MAGIC_NUMBER_SIZE
            )
        } catch (failure: ZstdException) {
            if (failure.errorCode == Zstd.errDstSizeTooSmall()) {
                throw decompressedPayloadExceedsDeclaredSize(sourceSize)
            }
            throw failure
        }
        requireExactDecompressedSize(sourceSize, actualSize)

        return output
    }
}
