package io.bluetape4k.okio.base64

import io.bluetape4k.logging.KLogging
import io.bluetape4k.okio.bufferOf
import okio.Buffer
import okio.ByteString
import okio.Sink

/**
 * 데이터를 Okio 내장 Base64로 인코딩하여 [Sink]에 쓰는 [Sink] 구현체.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = OkioBase64Sink(output)
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * val encoded = output.readUtf8()
 * // encoded는 Base64로 인코딩된 문자열 (예: "aGVsbG8=")
 * ```
 *
 * @see OkioBase64Source
 * @see ApacheBase64Sink
 */
class OkioBase64Sink(delegate: Sink): AbstractBase64Sink(delegate) {

    companion object: KLogging()

    /**
     * Okio Base64에서 `getEncodedBuffer` 함수를 제공합니다.
     */
    override fun getEncodedBuffer(plainByteString: ByteString): Buffer {
        return bufferOf(plainByteString.base64())
    }
}

/**
 * [Sink]를 Okio Base64 인코딩하는 [OkioBase64Sink]로 변환합니다.
 * 이미 [OkioBase64Sink]인 경우 그대로 반환합니다.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink = (output as Sink).asBase64Sink()
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * val encoded = output.readUtf8()
 * // encoded == "aGVsbG8="
 * ```
 */
fun Sink.asBase64Sink(): OkioBase64Sink = when (this) {
    is OkioBase64Sink -> this
    else -> OkioBase64Sink(this)
}
