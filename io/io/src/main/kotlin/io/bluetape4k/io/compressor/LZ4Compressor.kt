package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireLe
import io.bluetape4k.support.toInt
import net.jpountz.lz4.LZ4Exception
import net.jpountz.lz4.LZ4Factory
import java.nio.ByteBuffer

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
class LZ4Compressor: AbstractCompressor() {

    companion object: KLogging() {
        /**
         * 압축 데이터 헤더 크기 (원본 크기 저장용, 4 bytes)
         */
        private const val MAGIC_NUMBER_SIZE: Int = Int.SIZE_BYTES
        private const val MAX_DECOMPRESSED_SIZE: Int = 256 * 1024 * 1024

        private val factory: LZ4Factory = LZ4Factory.fastestInstance()
        private val compressor = factory.fastCompressor()
        private val decompressor = factory.fastDecompressor()
    }

    /**
     * 데이터를 압축합니다.
     *
     * 압축 형식: [원본 크기 4바이트][압축 데이터]
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val sourceSize = plain.size
        val maxOutputSize = compressor.maxCompressedLength(sourceSize)

        val output = ByteArray(maxOutputSize + MAGIC_NUMBER_SIZE)

        // 헤더: 원본 크기를 4바이트로 저장 (복원 시 사용)
        output[0] = (sourceSize ushr 24).toByte()
        output[1] = (sourceSize ushr 16).toByte()
        output[2] = (sourceSize ushr 8).toByte()
        output[3] = sourceSize.toByte()

        // 압축 데이터는 헤더 이후부터 저장
        val compressedSize = compressor.compress(plain, 0, sourceSize, output, MAGIC_NUMBER_SIZE, maxOutputSize)

        // 실제 사용한 크기만큼만 반환 (메모리 절약)
        return output.copyOf(MAGIC_NUMBER_SIZE + compressedSize)
    }

    override fun doCompress(plainBuffer: ByteBuffer): ByteBuffer {
        val sourceSize = plainBuffer.remaining()
        val maxOutputSize = compressor.maxCompressedLength(sourceSize)
        val output = ByteBuffer.allocate(MAGIC_NUMBER_SIZE + maxOutputSize)

        output.putInt(sourceSize)
        val compressedSize = compressor.compress(
            plainBuffer,
            plainBuffer.position(),
            sourceSize,
            output,
            MAGIC_NUMBER_SIZE,
            maxOutputSize
        )

        output.position(0)
        output.limit(MAGIC_NUMBER_SIZE + compressedSize)
        return output.slice()
    }

    /**
     * 압축된 데이터를 복원합니다.
     *
     * 헤더에서 원본 크기를 읽어 정확한 버퍼를 할당합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        // 헤더에서 원본 크기 추출 (처음 4바이트)
        val sourceSize = compressed.toInt()
            .requireGe(0, "sourceSize")
            .requireLe(MAX_DECOMPRESSED_SIZE, "sourceSize")

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

    override fun doDecompress(compressedBuffer: ByteBuffer): ByteBuffer {
        val sourceSize = compressedBuffer.getInt(compressedBuffer.position())
            .requireGe(0, "sourceSize")
            .requireLe(MAX_DECOMPRESSED_SIZE, "sourceSize")

        val output = ByteBuffer.allocate(sourceSize)
        decompressor.decompress(
            compressedBuffer,
            compressedBuffer.position() + MAGIC_NUMBER_SIZE,
            output,
            0,
            sourceSize
        )

        output.position(0)
        output.limit(sourceSize)
        return output.slice()
    }
}
