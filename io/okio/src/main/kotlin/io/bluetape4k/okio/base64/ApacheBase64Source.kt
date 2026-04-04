package io.bluetape4k.okio.base64

import io.bluetape4k.logging.KLogging
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.Source
import java.util.*

/**
 * URL-safe Base64로 인코딩된 [Source]를 읽어 복호화하는 [Source] 구현체.
 * NOTE: Apache Commons의 Base64 인코딩은 okio의 Base64 인코딩과 다르다. (특히 한글의 경우)
 *
 * ```kotlin
 * val encoded = bufferOf(Base64.getUrlEncoder().encodeToString("hello".toByteArray()))
 * val source = ApacheBase64Source(encoded)
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == "hello"
 * ```
 *
 * @see ApacheBase64Sink
 */
class ApacheBase64Source(delegate: Source): AbstractBase64Source(delegate) {

    companion object: KLogging() {
        private val urlDecoder = Base64.getUrlDecoder()
    }

    /**
     * Okio Base64에서 `decodeBase64Bytes` 함수를 제공합니다.
     */
    override fun decodeBase64Bytes(encodedString: String): ByteString {
        return urlDecoder.decode(encodedString).toByteString()
    }
}

/**
 * [Source]를 URL-safe Base64 디코딩하는 [ApacheBase64Source]로 변환합니다.
 * 이미 [ApacheBase64Source]인 경우 그대로 반환합니다.
 *
 * ```kotlin
 * val encoded = bufferOf(Base64.getUrlEncoder().encodeToString("world".toByteArray()))
 * val source = (encoded as Source).asApacheBase64Source()
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == "world"
 * ```
 */
fun Source.asApacheBase64Source(): ApacheBase64Source = when (this) {
    is ApacheBase64Source -> this
    else -> ApacheBase64Source(this)
}
