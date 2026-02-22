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
        check(!closed) { "Sink is already closed." }
        if (byteCount <= 0L) return
        byteCount.requireInRange(1, source.size, "byteCount")
        val bytesToRead = byteCount
        log.debug { "소스 데이터를 암호화하여 씁니다. 암호화할 데이터 크기=$bytesToRead" }

        val plainBytes = source.readByteArray(bytesToRead)
        log.trace { "암호화할 바이트 수: ${plainBytes.size} bytes" }

        val encryptedBytes = cipher.update(plainBytes)
        log.debug { "암호화한 바이트 수: ${encryptedBytes.size} bytes" }
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

        cipher.doFinal()?.let { tail ->
            if (tail.isNotEmpty()) {
                val buffer = bufferOf(tail)
                super.write(buffer, buffer.size)
            }
        }
        super.close()
    }
}

/**
 * [Sink]를 [FinalizingCipherSink]로 변환합니다.
 */
fun Sink.asFinalizingCipherSink(cipher: Cipher): FinalizingCipherSink =
    FinalizingCipherSink(this, cipher)
