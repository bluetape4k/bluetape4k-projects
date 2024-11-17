package io.bluetape4k.io.okio.base64

import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.ByteString
import okio.ForwardingSink
import okio.Sink

/**
 * 데이터를 Base64로 인코딩하여 [Sink]에 쓰는 [Sink] 구현체.
 * NOTE: Apache Commons의 Base64 인코딩은 okio의 Base64 인코딩과 다르다. (특히 한글의 경우)
 *
 * ```
 * val output = Buffer()
 * val sink = createSink(output)  // Base64Sink(output) or ApacheBase64Sink(output)
 *
 * val expected = faker.lorem().paragraph()
 * log.debug { "Plain string: $expected" }
 *
 * val source = bufferOf(expected)
 * sink.write(source, source.size)
 *
 * val base64Encoded = output.readUtf8()   // base64 encoded string
 * ```
 *
 * @see ApacheBase64Sink
 * @see ApacheBase64Source
 * @see Base64Sink
 * @see Base64Source
 */
abstract class AbstractBase64Sink(delegate: Sink): ForwardingSink(delegate) {

    companion object: KLogging()

    protected abstract fun getEncodedBuffer(plainByteString: ByteString): Buffer

    override fun write(source: Buffer, byteCount: Long) {
        val bytesToRead = byteCount.coerceAtMost(source.size)
        val readByteString = source.readByteString(bytesToRead)

        // Base64 encode
        val encodedSink = getEncodedBuffer(readByteString)
        super.write(encodedSink, encodedSink.size)
    }
}
