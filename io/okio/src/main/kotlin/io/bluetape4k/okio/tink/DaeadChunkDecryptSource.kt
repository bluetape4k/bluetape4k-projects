package io.bluetape4k.okio.tink

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireZeroOrPositiveNumber
import io.bluetape4k.tink.daead.TinkDeterministicAead
import okio.Buffer
import okio.EOFException
import okio.ForwardingSource
import okio.Source
import java.io.IOException

/**
 * DAEAD 청크 포맷으로 암호화된 데이터를 복호화하여 [Source]로 제공하는 구현체입니다.
 *
 * 입력은 `[8-byte big-endian ciphertext length][ciphertext]` 청크의 반복이어야 합니다.
 * [read]는 필요한 경우 다음 청크 하나만 읽고 복호화하므로 전체 암호문을 메모리에 적재하지 않습니다.
 *
 * [TinkDeterministicAead]는 같은 키, 평문, 연관 데이터에 대해 같은 암호문을 생성합니다.
 * 이 특성 때문에 동일 청크 평문 반복 여부가 노출될 수 있습니다. 또한 [associatedData]는
 * 인증되지만 암호화되지 않으며, 암호화 시 사용한 값과 동일해야 복호화가 성공합니다.
 *
 * @param delegate DAEAD 청크 암호문을 읽을 위임 [Source]
 * @param daead 청크 복호화에 사용할 DAEAD 래퍼
 * @param associatedData 청크 인증에 사용할 연관 데이터
 */
class DaeadChunkDecryptSource(
    delegate: Source,
    private val daead: TinkDeterministicAead,
    associatedData: ByteArray = ByteArray(0),
): ForwardingSource(delegate) {

    companion object: KLogging() {
        private const val CHUNK_HEADER_SIZE = Long.SIZE_BYTES.toLong()
        private const val MAX_NO_PROGRESS_READS = 8
    }

    private val associatedData: ByteArray = associatedData.copyOf()
    private val plainBuffer = Buffer()
    private var closed = false

    /**
     * 복호화된 평문을 [sink]에 최대 [byteCount] 바이트만큼 읽어옵니다.
     *
     * @param sink 복호화된 평문을 받을 버퍼
     * @param byteCount 요청할 최대 바이트 수
     * @return 실제 읽은 바이트 수 또는 EOF 시 -1
     */
    override fun read(sink: Buffer, byteCount: Long): Long {
        if (closed) {
            throw IOException("closed")
        }
        byteCount.requireZeroOrPositiveNumber("byteCount")
        if (byteCount == 0L) {
            return 0L
        }

        if (plainBuffer.size == 0L && !readNextChunk()) {
            return -1L
        }

        val bytesToRead = minOf(byteCount, plainBuffer.size)
        sink.write(plainBuffer, bytesToRead)
        return bytesToRead
    }

    /**
     * 내부 평문 버퍼와 위임 [Source]를 닫습니다.
     */
    override fun close() {
        if (closed) {
            return
        }
        closed = true
        try {
            super.close()
        } finally {
            plainBuffer.close()
        }
    }

    private fun readNextChunk(): Boolean {
        val header = readExactlyOrNull(CHUNK_HEADER_SIZE) ?: return false
        val ciphertextLength = header.readLong()

        if (ciphertextLength <= 0L || ciphertextLength > Int.MAX_VALUE) {
            throw IOException("Invalid DAEAD chunk ciphertext length: $ciphertextLength")
        }

        val ciphertext = readExactlyOrNull(ciphertextLength)
            ?: throw EOFException("Truncated DAEAD chunk ciphertext. expected=$ciphertextLength")

        val plaintext = daead.decryptDeterministically(ciphertext.readByteArray(), associatedData)
        plainBuffer.write(plaintext)
        return true
    }

    private fun readExactlyOrNull(byteCount: Long): Buffer? {
        val buffer = Buffer()
        var noProgressCount = 0

        while (buffer.size < byteCount) {
            val bytesRead = super.read(buffer, byteCount - buffer.size)
            when {
                bytesRead < 0L -> {
                    if (buffer.size == 0L) {
                        return null
                    }
                    throw EOFException("Truncated DAEAD chunk. expected=$byteCount actual=${buffer.size}")
                }

                bytesRead == 0L -> {
                    noProgressCount++
                    if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                        throw IOException("Unable to read DAEAD chunk bytes from source: no progress.")
                    }
                }

                else -> noProgressCount = 0
            }
        }

        return buffer
    }
}

/**
 * 현재 [Source]를 DAEAD 청크 복호화 [Source]로 변환합니다.
 *
 * @param daead 청크 복호화에 사용할 DAEAD 래퍼
 * @param associatedData 청크 인증에 사용할 연관 데이터
 * @return DAEAD 청크 복호화 [Source]
 */
fun Source.asDaeadChunkDecryptSource(
    daead: TinkDeterministicAead,
    associatedData: ByteArray = ByteArray(0),
): DaeadChunkDecryptSource =
    DaeadChunkDecryptSource(this, daead, associatedData)
