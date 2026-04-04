package io.bluetape4k.okio.base64

import io.bluetape4k.logging.KLogging
import io.bluetape4k.okio.bufferOf
import okio.Buffer
import okio.ByteString
import okio.Sink
import java.util.*

/**
 * 데이터를 Java 표준 URL-safe Base64로 인코딩하여 [Sink]에 쓰는 [Sink] 구현체.
 * NOTE: Apache Commons의 Base64 인코딩은 okio의 Base64 인코딩과 다르다. (특히 한글의 경우)
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = ApacheBase64Sink(output)
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * val encoded = output.readUtf8()
 * // encoded는 URL-safe Base64로 인코딩된 문자열
 * ```
 *
 * @see ApacheBase64Source
 */
class ApacheBase64Sink(delegate: Sink): AbstractBase64Sink(delegate) {

    companion object: KLogging()

    /**
     * Okio Base64에서 `getEncodedBuffer` 함수를 제공합니다.
     */
    override fun getEncodedBuffer(plainByteString: ByteString): Buffer {
        val encodedBytes = Base64.getUrlEncoder().encode(plainByteString.toByteArray())
        return bufferOf(encodedBytes)
    }
}

/**
 * [Sink]를 URL-safe Base64 인코딩하는 [ApacheBase64Sink]로 변환합니다.
 * 이미 [ApacheBase64Sink]인 경우 그대로 반환합니다.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = (output as Sink).asApacheBase64Sink()
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * val encoded = output.readUtf8()
 * // encoded는 URL-safe Base64로 인코딩된 문자열
 * ```
 */
fun Sink.asApacheBase64Sink(): ApacheBase64Sink = when (this) {
    is ApacheBase64Sink -> this
    else -> ApacheBase64Sink(this)
}
