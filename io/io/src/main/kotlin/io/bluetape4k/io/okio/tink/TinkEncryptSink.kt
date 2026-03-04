package io.bluetape4k.io.okio.tink

import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireInRange
import io.bluetape4k.tink.encrypt.TinkEncryptor
import okio.Buffer
import okio.ForwardingSink
import okio.Sink

/**
 * 데이터를 암호화하여 [Sink]에 쓰는 [Sink] 구현체.
 *
 * [TinkEncryptor]를 사용하여 바이트 배열 단위로 암호화한 후 위임 [Sink]에 씁니다.
 * Tink는 스트림 업데이트를 제공하지 않으므로 요청한 구간만 한 번에 암호화합니다.
 *
 * @see TinkDecryptSource
 */
open class TinkEncryptSink(
    delegate: Sink,
    private val encryptor: TinkEncryptor,
): ForwardingSink(delegate) {

    companion object: KLogging()

    /**
     * [source]에서 [byteCount] 바이트를 읽어 암호화한 후 위임 [Sink]에 씁니다.
     *
     * @param source 읽을 데이터 버퍼
     * @param byteCount 처리할 바이트 수 (0 이하이면 무시, source.size 초과이면 예외)
     */
    override fun write(source: Buffer, byteCount: Long) {
        if (byteCount <= 0L) return
        byteCount.requireInRange(0, source.size, "byteCount")

        // Tink 는 스트림 update를 제공하지 않으므로 요청한 구간만 암호화한다.
        val plainBytes = source.readByteArray(byteCount)

        // 암호화
        val encryptedBytes = encryptor.encrypt(plainBytes)
        val sink = bufferOf(encryptedBytes)
        super.write(sink, sink.size)
    }
}

/**
 * 현재 [Sink]를 [TinkEncryptor]로 암호화하는 [TinkEncryptSink]로 변환합니다.
 *
 * @param encryptor 암호화에 사용할 [TinkEncryptor]
 * @return 암호화 [TinkEncryptSink] 인스턴스
 */
fun Sink.asTinkEncryptSink(encryptor: TinkEncryptor): TinkEncryptSink =
    TinkEncryptSink(this, encryptor)
