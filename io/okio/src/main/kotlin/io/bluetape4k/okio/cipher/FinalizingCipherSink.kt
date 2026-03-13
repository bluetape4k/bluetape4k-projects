package io.bluetape4k.okio.cipher

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.okio.bufferOf
import io.bluetape4k.support.requireInRange
import okio.Buffer
import okio.ForwardingSink
import okio.Sink
import javax.crypto.Cipher

/**
 * [Cipher.update]로 스트리밍 암호화를 수행하고, close 시점에 [Cipher.doFinal]로 마무리하는 [Sink] 구현입니다.
 */
@Deprecated(
    message = "javax.crypto.Cipher 기반 스트리밍 암호화는 deprecated 되었습니다. Google Tink 기반 TinkEncryptSink 사용을 권장합니다.",
    replaceWith =
        ReplaceWith(
            "TinkEncryptSink(delegate, encryptor)",
            "io.bluetape4k.okio.tink.TinkEncryptSink",
        ),
    level = DeprecationLevel.WARNING,
)
open class FinalizingCipherSink(
    delegate: Sink,
    private val cipher: Cipher,
) : ForwardingSink(delegate) {
    companion object : KLogging()

    private var closed = false

    /**
     * [source]의 [byteCount] 바이트를 읽어 암호화한 뒤 delegate로 전달합니다.
     */
    override fun write(
        source: Buffer,
        byteCount: Long,
    ) {
        if (closed) {
            throw IllegalStateException("Cannot write to closed CipherSink")
        }

        if (byteCount == 0L) {
            return // 0 바이트는 유효하지만 처리할 필요 없음
        }

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
@Deprecated(
    message = "javax.crypto.Cipher 기반 암호화는 deprecated 되었습니다. Google Tink 기반 asTinkEncryptSink 사용을 권장합니다.",
    replaceWith =
        ReplaceWith(
            "asTinkEncryptSink(encryptor)",
            "io.bluetape4k.okio.tink.asTinkEncryptSink",
        ),
    level = DeprecationLevel.WARNING,
)
@Suppress("DEPRECATION")
fun Sink.asFinalizingCipherSink(cipher: Cipher): FinalizingCipherSink = FinalizingCipherSink(this, cipher)
