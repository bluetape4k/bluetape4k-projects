package io.bluetape4k.io.okio.cipher

import okio.Sink
import javax.crypto.Cipher

/**
 * [FinalizingCipherSink]의 이전 이름입니다.
 *
 * @see FinalizingCipherSink
 */
@Deprecated(
    message = "명확한 동작 표현을 위해 FinalizingCipherSink 사용을 권장합니다.",
    replaceWith = ReplaceWith(
        "FinalizingCipherSink(delegate, cipher)",
        "io.bluetape4k.io.okio.cipher.FinalizingCipherSink"
    )
)
open class CipherSink(
    delegate: Sink,
    cipher: Cipher,
): FinalizingCipherSink(delegate, cipher)

/**
 * [Sink]를 [CipherSink]로 변환합니다.
 */
@Deprecated(
    message = "명확한 동작 표현을 위해 asFinalizingCipherSink 사용을 권장합니다.",
    replaceWith = ReplaceWith(
        "asFinalizingCipherSink(cipher)",
        "io.bluetape4k.io.okio.cipher.asFinalizingCipherSink"
    )
)
@Suppress("DEPRECATION")
fun Sink.asCipherSink(cipher: Cipher): CipherSink =
    CipherSink(this, cipher)
