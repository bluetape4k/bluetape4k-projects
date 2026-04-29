package io.bluetape4k.okio.tink

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.tink.daead.TinkDeterministicAead
import okio.Buffer
import okio.ForwardingSink
import okio.Sink
import java.io.IOException

/**
 * 평문을 DAEAD 청크 포맷으로 암호화하여 [Sink]에 기록하는 [Sink] 구현체입니다.
 *
 * 입력은 [chunkSize] 크기의 평문 청크로 나뉘며, 각 청크는
 * `[8-byte big-endian ciphertext length][ciphertext]` 형식으로 기록됩니다.
 * 마지막 partial chunk는 [close] 시점에 기록되므로 반드시 `use {}` 또는 [close]를 호출해야 합니다.
 *
 * [TinkDeterministicAead]는 같은 키, 평문, 연관 데이터에 대해 같은 암호문을 생성합니다.
 * 이 특성 때문에 동일 청크 평문 반복 여부가 노출될 수 있습니다. 결정성이 필요하지 않은 일반
 * 암호화에는 `TinkEncryptSink`를 사용하세요.
 *
 * ```kotlin
 * val daead = TinkDaeads.AES256_SIV
 * val output = Buffer()
 *
 * output.asDaeadChunkEncryptSink(daead).use { sink ->
 *     val plaintext = Buffer().writeUtf8("대용량 스트리밍 평문")
 *     sink.write(plaintext, plaintext.size)
 * }
 * // output 에는 DAEAD 청크 포맷으로 암호화된 데이터가 담겨 있다.
 * // DaeadChunkDecryptSource 로 복호화하면 원본 평문을 얻을 수 있다.
 * ```
 *
 * @param delegate 암호문을 기록할 위임 [Sink]
 * @param daead 청크 암호화에 사용할 DAEAD 래퍼
 * @param chunkSize 평문 청크 크기. 기본값은 [DEFAULT_DAEAD_CHUNK_SIZE]입니다.
 * @param associatedData 청크 인증에 사용할 연관 데이터. 인증되지만 암호화되지는 않습니다.
 * @see DaeadChunkDecryptSource
 * @see TinkEncryptSink
 */
class DaeadChunkEncryptSink(
    delegate: Sink,
    private val daead: TinkDeterministicAead,
    private val chunkSize: Int = DEFAULT_DAEAD_CHUNK_SIZE,
    associatedData: ByteArray = ByteArray(0),
): ForwardingSink(delegate) {

    companion object: KLogging()

    private val associatedData: ByteArray = associatedData.copyOf()
    private val plainBuffer = Buffer()
    private var closed = false

    init {
        chunkSize.requirePositiveNumber("chunkSize")
    }

    /**
     * [source]에서 [byteCount] 바이트를 읽어 내부 버퍼에 누적한 뒤 완성된 청크를 암호화합니다.
     *
     * @param source 평문을 제공하는 버퍼
     * @param byteCount 처리할 바이트 수
     * @throws IOException close 이후 호출 시, 또는 암호화 실패 시
     */
    override fun write(source: Buffer, byteCount: Long) {
        if (closed) {
            throw IOException("closed")
        }
        byteCount.requireInRange(0L, source.size, "byteCount")
        if (byteCount == 0L) {
            return
        }

        plainBuffer.write(source, byteCount)
        emitCompleteChunks()
    }

    /**
     * 완성된 청크만 기록하고 partial chunk는 [close]까지 유지합니다.
     *
     * **주의**: partial chunk는 [close] 시점에만 기록됩니다. `flush()`를 호출해도
     * partial chunk는 기록되지 않습니다.
     */
    override fun flush() {
        if (!closed) {
            emitCompleteChunks()
            super.flush()
        }
    }

    /**
     * 남은 partial chunk를 암호화한 뒤 위임 [Sink]를 닫습니다.
     */
    override fun close() {
        if (closed) {
            return
        }

        var thrown: Throwable? = null
        try {
            emitRemainingChunk()
            super.flush()
        } catch (e: Throwable) {
            thrown = e
        } finally {
            closed = true
            try {
                super.close()
            } catch (closeException: Throwable) {
                if (thrown == null) {
                    thrown = closeException
                } else {
                    thrown.addSuppressed(closeException)
                }
            }
            plainBuffer.close()
        }

        thrown?.let { throw it }
    }

    private fun emitCompleteChunks() {
        while (plainBuffer.size >= chunkSize) {
            emitChunk(chunkSize.toLong())
        }
    }

    private fun emitRemainingChunk() {
        if (plainBuffer.size > 0L) {
            emitChunk(plainBuffer.size)
        }
    }

    private fun emitChunk(byteCount: Long) {
        val plaintext = plainBuffer.readByteArray(byteCount)
        val ciphertext = daead.encryptDeterministically(plaintext, associatedData)
        val frame = Buffer()
            .writeLong(ciphertext.size.toLong())
            .write(ciphertext)

        super.write(frame, frame.size)
    }
}

/**
 * 현재 [Sink]를 DAEAD 청크 암호화 [Sink]로 변환합니다.
 *
 * ```kotlin
 * val daead = TinkDaeads.AES256_SIV
 * val output = Buffer()
 *
 * output.asDaeadChunkEncryptSink(daead, associatedData = "context".toByteArray()).use { sink ->
 *     sink.write(Buffer().writeUtf8("hello"), 5L)
 * }
 * // output 을 DaeadChunkDecryptSource 로 같은 associatedData 를 사용해 복호화하세요.
 * ```
 *
 * @param daead 청크 암호화에 사용할 DAEAD 래퍼
 * @param chunkSize 평문 청크 크기. 기본값은 [DEFAULT_DAEAD_CHUNK_SIZE] (64 KiB)입니다.
 * @param associatedData 청크 인증에 사용할 연관 데이터. 복호화 시 동일한 값을 전달해야 합니다.
 * @return DAEAD 청크 암호화 [Sink]
 * @see DaeadChunkDecryptSource
 */
fun Sink.asDaeadChunkEncryptSink(
    daead: TinkDeterministicAead,
    chunkSize: Int = DEFAULT_DAEAD_CHUNK_SIZE,
    associatedData: ByteArray = ByteArray(0),
): DaeadChunkEncryptSink =
    DaeadChunkEncryptSink(this, daead, chunkSize, associatedData)

/**
 * DAEAD 청크 암호화에서 사용하는 기본 평문 청크 크기 (64 KiB)입니다.
 *
 * 암호화 Sink 생성 시 [chunkSize] 파라미터로 이 값을 재정의할 수 있습니다.
 * [DaeadChunkDecryptSource]는 헤더에서 ciphertext 길이를 읽으므로 복호화 측에서
 * 별도로 chunkSize 를 지정할 필요가 없습니다.
 */
const val DEFAULT_DAEAD_CHUNK_SIZE: Int = 64 * 1024
