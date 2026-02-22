package io.bluetape4k.io.okio.cipher

import okio.Source
import javax.crypto.Cipher

/**
 * [StreamingCipherSource]의 이전 이름입니다.
 *
 * @see StreamingCipherSource
 */
@Deprecated(
    message = "명확한 동작 표현을 위해 StreamingCipherSource 사용을 권장합니다.",
    replaceWith = ReplaceWith(
        "StreamingCipherSource(delegate, cipher)",
        "io.bluetape4k.io.okio.cipher.StreamingCipherSource"
    )
)
open class CipherSource(
    delegate: Source,
    cipher: Cipher,
): StreamingCipherSource(delegate, cipher)

/**
 * [Source]를 [CipherSource]로 변환합니다.
 */
@Deprecated(
    message = "명확한 동작 표현을 위해 asStreamingCipherSource 사용을 권장합니다.",
    replaceWith = ReplaceWith(
        "asStreamingCipherSource(cipher)",
        "io.bluetape4k.io.okio.cipher.asStreamingCipherSource"
    )
)
@Suppress("DEPRECATION")
fun Source.asCipherSource(cipher: Cipher): CipherSource =
    CipherSource(this, cipher)
