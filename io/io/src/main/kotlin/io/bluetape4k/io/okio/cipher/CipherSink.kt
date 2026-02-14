package io.bluetape4k.io.okio.cipher

import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireInRange
import okio.ForwardingSink
import okio.Sink
import javax.crypto.Cipher

/**
 * 데이터를 암호화하여 [delegate]에 씁니다.
 *
 * @see [CipherSource]
 */
open class CipherSink(
    delegate: Sink,
    private val cipher: Cipher,
): ForwardingSink(delegate) {

    companion object: KLogging()

    /**
     * Okio 암복호화에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override fun write(source: okio.Buffer, byteCount: Long) {
        if (byteCount <= 0L) return
        byteCount.requireInRange(1, source.size, "byteCount")
        val bytesToRead = byteCount
        log.debug { "소스 데이터를 암호화하여 씁니다. 암호화할 데이터 크기=$bytesToRead" }

        val plainBytes = source.readByteArray(bytesToRead)
        log.trace { "암호화할 바이트 수: ${plainBytes.size} bytes" }

        val encryptedBytes = cipher.doFinal(plainBytes)
        log.debug { "암호화한 바이트 수: ${encryptedBytes.size} bytes" }
        val encryptedSink = bufferOf(encryptedBytes)

        super.write(encryptedSink, encryptedSink.size)
    }
}
