package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toByteArray
import io.bluetape4k.support.toInt
import net.jpountz.lz4.LZ4Factory

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
 * @see [lz4-java](https://github.com/lz4/lz4-java)
 */
class LZ4Compressor: AbstractCompressor() {

    companion object: KLogging() {
        /**
         * 압축 데이터 헤더 크기 (원본 크기 저장용, 4 bytes)
         */
        private const val MAGIC_NUMBER_SIZE: Int = Int.SIZE_BYTES

        private val factory: LZ4Factory by lazy { LZ4Factory.fastestInstance() }
        private val compressor by lazy { factory.fastCompressor() }
        private val decompressor by lazy { factory.fastDecompressor() }
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
            src = plain,
            srcOff = 0,
            srcLen = sourceSize,
            dest = output,
            destOff = MAGIC_NUMBER_SIZE,  // 헤더 이후부터
            maxDestLen = maxOutputSize
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

        // 원본 크기만큼 버퍼 할당
        val output = ByteArray(sourceSize)

        // 헤더 이후의 압축 데이터를 복원
        decompressor.decompress(
            src = compressed,
            srcOff = MAGIC_NUMBER_SIZE,  // 헤더 건너뛰기
            dest = output,
            destOff = 0,
            destLen = sourceSize
        )

        return output
    }
}
