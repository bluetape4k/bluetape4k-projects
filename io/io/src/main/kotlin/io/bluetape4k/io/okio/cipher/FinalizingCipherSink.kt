package io.bluetape4k.io.okio.cipher

import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireInRange
import okio.Buffer
import okio.ForwardingSink
import okio.Sink
import javax.crypto.Cipher

/**
 * [Cipher.update]로 스트리밍 암호화를 수행하고, close 시점에 [Cipher.doFinal]로 마무리하는 [Sink] 구현입니다.
 */
open class FinalizingCipherSink(
    delegate: Sink,
    private val cipher: Cipher,
): ForwardingSink(delegate) {

    companion object: KLogging()

    private var closed = false

    /**
     * [source]의 [byteCount] 바이트를 읽어 암호화한 뒤 delegate로 전달합니다.
     */
    override fun write(source: Buffer, byteCount: Long) {
        if (closed) {
            throw IllegalStateException("Cannot write to closed CipherSink")
        }

        if (byteCount == 0L) {
            return  // 0 바이트는 유효하지만 처리할 필요 없음
        }

        require(byteCount > 0) { "byteCount must be positive: $byteCount" }
        byteCount.requireInRange(1, source.size, "byteCount")

        if (log.isTraceEnabled) {
            log.trace { "Encrypting $byteCount bytes" }
        }

        val plainBytes = source.readByteArray(byteCount)
        val encryptedBytes = cipher.update(plainBytes)

        if (log.isDebugEnabled && encryptedBytes.size != plainBytes.size) {
            log.debug { "Encrypted: ${plainBytes.size} → ${encryptedBytes.size} bytes" }
        }

        val encryptedSink = bufferOf(encryptedBytes)
        super.write(encryptedSink, encryptedSink.size)
    }

    /**
     * 남은 암호화 블록을 finalize 하여 delegate에 기록한 후 닫습니다.
     */
    override fun close() {
        if (closed) {
            return
        }
        closed = true

        try {
            cipher.doFinal()?.let { tail ->
                if (tail.isNotEmpty()) {
                    val buffer = bufferOf(tail)
                    super.write(buffer, buffer.size)
                }
            }
        } finally {
            super.close()
        }
    }
}

/**
 * [Sink]를 [FinalizingCipherSink]로 변환합니다.
 */
fun Sink.asFinalizingCipherSink(cipher: Cipher): FinalizingCipherSink =
    FinalizingCipherSink(this, cipher)
