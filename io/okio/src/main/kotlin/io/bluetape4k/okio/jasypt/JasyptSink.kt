package io.bluetape4k.okio.jasypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.okio.bufferOf
import io.bluetape4k.okio.tink.TinkEncryptSink
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireInRange
import okio.Buffer
import okio.ForwardingSink
import okio.Sink

/**
 * 데이터를 암호화하여 [Sink]에 쓰는 [Sink] 구현체.
 *
 * @see DecryptSource
 */
@Deprecated(
    message = "Jasypt 기반 암호화는 deprecated 되었습니다. Google Tink 기반 TinkEncryptSink 사용을 권장합니다.",
    replaceWith = ReplaceWith(
        "TinkEncryptSink(delegate, encryptor)",
        "io.bluetape4k.okio.tink.TinkEncryptSink"
    ),
    level = DeprecationLevel.WARNING
)
open class EncryptSink(
    delegate: Sink,
    private val encryptor: Encryptor,
): ForwardingSink(delegate) {

    companion object: KLogging()

    /**
     * Okio I/O에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override fun write(source: Buffer, byteCount: Long) {
        if (byteCount <= 0L) return
        byteCount.requireInRange(0, source.size, "byteCount")

        // Jasypt 는 Cipher와 달리 스트림 update를 제공하지 않으므로 요청한 구간만 암호화한다.
        val plainBytes = source.readByteArray(byteCount)

        // 암호화
        val encryptedBytes = encryptor.encrypt(plainBytes)
        val sink = bufferOf(encryptedBytes)
        super.write(sink, sink.size)
    }
}

/**
 * Okio I/O 타입 변환을 위한 `asEncryptSink` 함수를 제공합니다.
 */
@Deprecated(
    message = "Jasypt 기반 암호화는 deprecated 되었습니다. Google Tink 기반 asTinkEncryptSink 사용을 권장합니다.",
    replaceWith = ReplaceWith(
        "asTinkEncryptSink(encryptor)",
        "io.bluetape4k.okio.tink.asTinkEncryptSink"
    ),
    level = DeprecationLevel.WARNING
)
@Suppress("DEPRECATION")
fun Sink.asEncryptSink(encryptor: Encryptor): EncryptSink =
    EncryptSink(this, encryptor)
