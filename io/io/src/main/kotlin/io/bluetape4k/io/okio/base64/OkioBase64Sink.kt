package io.bluetape4k.io.okio.base64

import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.ByteString
import okio.Sink

/**
 * 데이터를 Base64로 인코딩하여 [Sink]에 쓰는 [Sink] 구현체.
 *
 * @see OkioBase64Source
 * @see ApacheBase64Sink
 */
class OkioBase64Sink(delegate: Sink): AbstractBase64Sink(delegate) {

    companion object: KLogging()

    override fun getEncodedBuffer(plainByteString: ByteString): Buffer {
        return bufferOf(plainByteString.base64())
    }
}

fun Sink.asBase64Sink(): OkioBase64Sink = when (this) {
    is OkioBase64Sink -> this
    else -> OkioBase64Sink(this)
}
