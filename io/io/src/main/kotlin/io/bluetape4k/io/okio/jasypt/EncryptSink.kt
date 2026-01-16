package io.bluetape4k.io.okio.jasypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.ForwardingSink
import okio.Sink

/**
 * 데이터를 암호화하여 [Sink]에 쓰는 [Sink] 구현체.
 *
 * @see DecryptSource
 */
open class EncryptSink(
    delegate: Sink,
    private val encryptor: Encryptor,
): ForwardingSink(delegate) {

    companion object: KLogging()

    override fun write(source: Buffer, byteCount: Long) {
        // Jasypt 는 Cipher랑 달리 한번에 모두 써야 한다.
        // 요청한 바이트 수(또는 가능한 모든 바이트) 반환
        val plainBytes = source.readByteArray()

        // 암호화
        val encryptedBytes = encryptor.encrypt(plainBytes)
        val sink = bufferOf(encryptedBytes)
        super.write(sink, sink.size)
    }
}

fun Sink.asEncryptSink(encryptor: Encryptor): EncryptSink =
    EncryptSink(this, encryptor)
