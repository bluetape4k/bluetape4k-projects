package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toByteArray
import io.bluetape4k.support.toInt
import net.jpountz.lz4.LZ4Exception
import net.jpountz.lz4.LZ4Factory
import java.nio.BufferOverflowException
import java.nio.ByteBuffer

internal interface LZ4BufferOperations {
    fun compress(
        source: ByteBuffer,
        sourceOffset: Int,
        sourceLength: Int,
        target: ByteBuffer,
        targetOffset: Int,
        targetLength: Int,
    ): Int

    fun decompress(
        source: ByteBuffer,
        sourceOffset: Int,
        target: ByteBuffer,
        targetOffset: Int,
        targetLength: Int,
    ): Int
}

/**
 * LZ4 알고리즘을 사용한 고성능 압축기
 *
 * ## 압축 형식
 * ```
 * [원본 크기: 4 bytes] [압축된 데이터: N bytes]
 * ```
 * 압축된 데이터 앞에 원본 크기(4바이트)를 저장하여 복원 시 정확한 버퍼 할당이 가능합니다.
 *
 * ## 성능 특성
 * - 압축 속도: ~500 MB/s
 * - 해제 속도: ~2 GB/s
 * - 압축률: 50-60%
 * - 실시간 처리에 적합
 *
 * ## 사용 예시
 * ```kotlin
 * val compressor = Compressors.LZ4
 * val compressed = compressor.compress("Hello, LZ4!".toByteArray())
 * val decompressed = compressor.decompress(compressed)
 * ```
 *
 * @see [lz4-java yawkat fork](https://github.com/yawkat/lz4-java) — CVE-2025-12183/CVE-2025-66566 패치 포함 유지보수 버전
 * @throws IllegalArgumentException when the stored source-size header is negative or exceeds the 256 MB limit.
 * @throws LZ4Exception when LZ4 block compression or decompression fails.
 */
class LZ4Compressor private constructor(
    private val bufferOperations: LZ4BufferOperations,
): AbstractCompressor() {

    constructor(): this(defaultBufferOperations)

    companion object: KLogging() {
        /**
         * 압축 데이터 헤더 크기 (원본 크기 저장용, 4 bytes)
         */
        private const val MAGIC_NUMBER_SIZE: Int = Int.SIZE_BYTES
        private const val MAX_DECOMPRESSED_SIZE: Int = 256 * 1024 * 1024

        private val factory: LZ4Factory by lazy { LZ4Factory.fastestInstance() }
        private val compressor by lazy { factory.fastCompressor() }
        private val decompressor by lazy { factory.fastDecompressor() }
        private val defaultBufferOperations: LZ4BufferOperations = object: LZ4BufferOperations {
            override fun compress(
                source: ByteBuffer,
                sourceOffset: Int,
                sourceLength: Int,
                target: ByteBuffer,
                targetOffset: Int,
                targetLength: Int,
            ): Int = compressor.compress(
                source,
                sourceOffset,
                sourceLength,
                target,
                targetOffset,
                targetLength,
            )

            override fun decompress(
                source: ByteBuffer,
                sourceOffset: Int,
                target: ByteBuffer,
                targetOffset: Int,
                targetLength: Int,
            ): Int = decompressor.decompress(
                source,
                sourceOffset,
                target,
                targetOffset,
                targetLength,
            )
        }

        internal fun forTesting(bufferOperations: LZ4BufferOperations): LZ4Compressor =
            LZ4Compressor(bufferOperations)
    }

    override fun compress(source: ByteBuffer, target: ByteBuffer): Int =
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
                bufferOperations.compress(
                    sourceView,
                    sourcePosition,
                    sourceRemaining,
                    targetView,
                    targetPosition + MAGIC_NUMBER_SIZE,
                    payloadCapacity,
                )
            } catch (failure: LZ4Exception) {
                if (failure.message == "maxDestLen is too small") {
                    throw BufferOverflowException()
                }
                throw failure
            }
            if (payloadWritten !in 1..payloadCapacity) {
                throw IllegalStateException(
                    "LZ4 payload write count out of range: " +
                            "written=$payloadWritten, capacity=$payloadCapacity"
                )
            }
            Math.addExact(MAGIC_NUMBER_SIZE, payloadWritten)
        }

    override fun decompress(source: ByteBuffer, target: ByteBuffer): Int =
        writeToCallerBufferViews(source, target) {
                sourceView,
                targetView,
                sourcePosition,
                sourceRemaining,
                targetPosition,
                targetRemaining,
            ->
            if (sourceRemaining < MAGIC_NUMBER_SIZE) {
                throw IndexOutOfBoundsException("LZ4 header requires 4 bytes")
            }

            val declaredSize = getIntBigEndian(sourceView, sourcePosition)
            require(declaredSize >= 0) {
                "sourceSize must not be negative: $declaredSize"
            }
            require(declaredSize <= MAX_DECOMPRESSED_SIZE) {
                "sourceSize exceeds 256 MiB: $declaredSize"
            }
            if (declaredSize > targetRemaining) throw BufferOverflowException()

            val payload = sourceView.duplicate().apply {
                position(sourcePosition + MAGIC_NUMBER_SIZE)
                limit(sourcePosition + sourceRemaining)
            }.slice()
            val consumed = bufferOperations.decompress(
                payload,
                0,
                targetView,
                targetPosition,
                declaredSize,
            )
            if (consumed != payload.remaining()) {
                throw LZ4Exception(
                    "LZ4 compressed payload length mismatch: " +
                            "consumed=$consumed, remaining=${payload.remaining()}"
                )
            }
            declaredSize
        }

    /**
     * 데이터를 압축합니다.
     *
     * 압축 형식: [원본 크기 4바이트][압축 데이터]
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val sourceSize = plain.size
        val maxOutputSize = compressor.maxCompressedLength(sourceSize)

        // 헤더(원본 크기) + 압축 데이터를 담을 버퍼 생성
        val output = ByteArray(maxOutputSize + MAGIC_NUMBER_SIZE)

        // 헤더: 원본 크기를 4바이트로 저장 (복원 시 사용)
        sourceSize.toByteArray().copyInto(output, destinationOffset = 0)

        // 압축 데이터는 헤더 이후부터 저장
        val compressedSize = compressor.compress(
            plain,
            0,
            sourceSize,
            output,
            MAGIC_NUMBER_SIZE,  // 헤더 이후부터
            maxOutputSize
        )

        // 실제 사용한 크기만큼만 반환 (메모리 절약)
        return output.copyOf(MAGIC_NUMBER_SIZE + compressedSize)
    }

    /**
     * 압축된 데이터를 복원합니다.
     *
     * 헤더에서 원본 크기를 읽어 정확한 버퍼를 할당합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        // 헤더에서 원본 크기 추출 (처음 4바이트)
        val sourceSize = compressed.toInt()
        require(sourceSize >= 0) { "sourceSize가 음수입니다. 손상된 데이터일 수 있습니다. sourceSize=$sourceSize" }
        require(sourceSize <= 256 * 1024 * 1024) {
            "sourceSize가 허용 한도(256MB)를 초과합니다. 손상되거나 악의적인 데이터일 수 있습니다. sourceSize=$sourceSize"
        }

        // 원본 크기만큼 버퍼 할당
        val output = ByteArray(sourceSize)

        // 헤더 이후의 압축 데이터를 복원
        decompressor.decompress(
            compressed,
            MAGIC_NUMBER_SIZE,  // 헤더 건너뛰기
            output,
            0,
            sourceSize
        )

        return output
    }
}
