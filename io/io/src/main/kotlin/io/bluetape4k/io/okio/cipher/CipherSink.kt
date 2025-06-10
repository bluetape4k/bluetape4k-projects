package io.bluetape4k.io.okio.cipher

import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import okio.ForwardingSink
import okio.Sink
import javax.crypto.Cipher

/**
 * 데이터를 암호화하여 [Sink]에 씁니다.
 */
open class CipherSink(
    delegate: Sink,
    private val cipher: Cipher,
): ForwardingSink(delegate) {

    companion object: KLogging()

    override fun write(source: okio.Buffer, byteCount: Long) {
        val bytesToRead = byteCount.coerceAtMost(source.size)
        log.trace { "Write data from source with cipher. bytes to read=$bytesToRead" }

        val plainBytes = source.readByteArray(bytesToRead)
        log.trace { "Encrypt plain bytes: ${plainBytes.size} bytes" }

        val encryptedBytes = cipher.doFinal(plainBytes)
        val encryptedSink = bufferOf(encryptedBytes)

        super.write(encryptedSink, encryptedSink.size)
    }
}
