package io.bluetape4k.okio.base64

import io.bluetape4k.logging.KLogging
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.Source

/**
 * Okio 내장 Base64로 인코딩된 [Source]를 읽어 디코딩하는 [Source] 구현체.
 *
 * ```kotlin
 * val encoded = bufferOf("aGVsbG8=")  // "hello" in Base64
 * val source = OkioBase64Source(encoded)
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == "hello"
 * ```
 *
 * @see OkioBase64Sink
 * @see ApacheBase64Source
 */
class OkioBase64Source(delegate: Source): AbstractBase64Source(delegate) {

    companion object: KLogging()

    /**
     * Okio Base64에서 `decodeBase64Bytes` 함수를 제공합니다.
     */
    override fun decodeBase64Bytes(encodedString: String): ByteString? {
        return encodedString.decodeBase64()
    }
}

/**
 * [Source]를 Okio Base64 디코딩하는 [OkioBase64Source]로 변환합니다.
 * 이미 [OkioBase64Source]인 경우 그대로 반환합니다.
 *
 * ```kotlin
 * val encoded = bufferOf("aGVsbG8=")  // "hello" in Base64
 * val source = (encoded as Source).asBase64Source()
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == "hello"
 * ```
 */
fun Source.asBase64Source(): OkioBase64Source = when (this) {
    is OkioBase64Source -> this
    else -> OkioBase64Source(this)
}
