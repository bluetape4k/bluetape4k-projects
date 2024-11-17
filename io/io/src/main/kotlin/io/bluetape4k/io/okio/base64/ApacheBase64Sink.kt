package io.bluetape4k.io.okio.base64

import io.bluetape4k.codec.encodeBase64ByteArray
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.ByteString
import okio.Sink

/**
 * 데이터를 Apache Commons의 Base64로 인코딩하여 [Sink]에 쓰는 [Sink] 구현체.
 * NOTE: Apache Commons의 Base64 인코딩은 okio의 Base64 인코딩과 다르다. (특히 한글의 경우)
 *
 * @see ApacheBase64Source
 */
class ApacheBase64Sink(delegate: Sink): AbstractBase64Sink(delegate) {

    companion object: KLogging()

    override fun getEncodedBuffer(plainByteString: ByteString): Buffer {
        return bufferOf(plainByteString.toByteArray().encodeBase64ByteArray())
    }

}
