package io.bluetape4k.io.okio.base64

import io.bluetape4k.codec.decodeBase64String
import io.bluetape4k.logging.KLogging
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.Source

/**
 * 데이터를 Apache Commons의 Base64로 인코딩하여 [Source]에 쓰는 [Source] 구현체.
 * NOTE: Apache Commons의 Base64 인코딩은 okio의 Base64 인코딩과 다르다. (특히 한글의 경우)
 *
 * @see ApacheBase64Sink
 */
class ApacheBase64Source(delegate: Source): AbstractBase64Source(delegate) {

    companion object: KLogging() {
        const val MAX_REQUEST_LENGTH = 9223372036854775804L // 4 * (Long.MAX_VALUE / 4)
        const val BASE64_BLOCK = 4 // 4바이트 블록을 읽어 3바이트 디코딩
    }

    override fun decodeBase64Bytes(encodedString: String): ByteString? {
        return encodedString.decodeBase64String().encodeUtf8()
    }
}
