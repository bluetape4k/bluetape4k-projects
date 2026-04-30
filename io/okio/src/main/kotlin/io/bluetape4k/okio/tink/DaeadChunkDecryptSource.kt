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
 * ```kotlin
 * val daead = TinkDaeads.AES256_SIV
 * val output = Buffer()
 *
 * // 암호화
 * output.asDaeadChunkEncryptSink(daead).use { sink ->
 *     sink.write(Buffer().writeUtf8("스트리밍 평문"), 14L)
 * }
 *
 * // 복호화 — 한 번에 하나의 청크만 메모리에 적재
 * val decrypted = Buffer()
 * output.asDaeadChunkDecryptSource(daead).use { source ->
 *     source.read(decrypted, Long.MAX_VALUE)
 * }
 * val plaintext = decrypted.readUtf8() // "스트리밍 평문"
 * ```
 *
 * @param delegate DAEAD 청크 암호문을 읽을 위임 [Source]
 * @param daead 청크 복호화에 사용할 DAEAD 래퍼
 * @param associatedData 청크 인증에 사용할 연관 데이터. 암호화 시 사용한 값과 동일해야 합니다.
 * @param maxCiphertextLength 허용하는 청크 ciphertext 최대 길이 (기본값: [DEFAULT_DAEAD_MAX_CIPHERTEXT_LENGTH]).
 *   신뢰할 수 없는 소스에서 읽을 때 대규모 메모리 할당 공격을 방어합니다.
 * @see DaeadChunkEncryptSink
 * @see TinkDecryptSource
 */
class DaeadChunkDecryptSource(
    delegate: Source,
    private val daead: TinkDeterministicAead,
    associatedData: ByteArray = ByteArray(0),
    private val maxCiphertextLength: Long = DEFAULT_DAEAD_MAX_CIPHERTEXT_LENGTH,
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
     * 내부 평문 버퍼가 비어 있으면 다음 청크 하나를 읽고 복호화합니다.
     * 한 번 호출 시 최대 한 청크 분량의 평문만 반환할 수 있습니다.
     *
     * @param sink 복호화된 평문을 받을 버퍼
     * @param byteCount 요청할 최대 바이트 수
     * @return 실제 읽은 바이트 수 또는 EOF 시 -1
     * @throws IOException close 이후 호출 시, 또는 스트림 포맷 오류 시
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
        try {
            super.close()
        } finally {
            closed = true
            plainBuffer.close()
        }
    }

    private fun readNextChunk(): Boolean {
        val header = readExactlyOrNull(CHUNK_HEADER_SIZE) ?: return false
        val ciphertextLength = header.readLong()

        if (ciphertextLength <= 0L || ciphertextLength > Int.MAX_VALUE) {
            throw IOException("Invalid DAEAD chunk ciphertext length: $ciphertextLength")
        }
        if (ciphertextLength > maxCiphertextLength) {
            throw IOException(
                "DAEAD chunk ciphertext length $ciphertextLength exceeds maxCiphertextLength $maxCiphertextLength"
            )
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
 * ```kotlin
 * val daead = TinkDaeads.AES256_SIV
 * val decrypted = Buffer()
 *
 * encryptedSource.asDaeadChunkDecryptSource(
 *     daead,
 *     associatedData = "context".toByteArray(),
 * ).use { source ->
 *     while (source.read(decrypted, DEFAULT_BUFFER_SIZE.toLong()) >= 0L) { /* drain */ }
 * }
 * ```
 *
 * @param daead 청크 복호화에 사용할 DAEAD 래퍼
 * @param associatedData 청크 인증에 사용할 연관 데이터. 암호화 시 사용한 값과 동일해야 합니다.
 * @param maxCiphertextLength 허용하는 청크 ciphertext 최대 길이. 기본값: [DEFAULT_DAEAD_MAX_CIPHERTEXT_LENGTH].
 * @return DAEAD 청크 복호화 [Source]
 * @see DaeadChunkEncryptSink
 */
fun Source.asDaeadChunkDecryptSource(
    daead: TinkDeterministicAead,
    associatedData: ByteArray = ByteArray(0),
    maxCiphertextLength: Long = DEFAULT_DAEAD_MAX_CIPHERTEXT_LENGTH,
): DaeadChunkDecryptSource =
    DaeadChunkDecryptSource(this, daead, associatedData, maxCiphertextLength)

/**
 * DAEAD 청크 복호화에서 허용하는 기본 최대 ciphertext 길이 (16 MiB)입니다.
 *
 * 신뢰할 수 없는 소스로부터 대규모 메모리 할당 공격을 방어하기 위한 상한선입니다.
 * 암호화 측의 `chunkSize` 가 이 값보다 크면 [DaeadChunkDecryptSource] 생성 시
 * `maxCiphertextLength` 파라미터를 명시적으로 지정하세요.
 */
const val DEFAULT_DAEAD_MAX_CIPHERTEXT_LENGTH: Long = 16L * 1024 * 1024
