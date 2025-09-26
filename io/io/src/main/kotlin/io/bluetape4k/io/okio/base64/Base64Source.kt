package io.bluetape4k.io.okio.base64

import io.bluetape4k.logging.KLogging
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.Source

/**
 * Base64로 인코딩된 [Source]를 읽어 디코딩하여 전달하는 [Source] 구현체.
 *
 * @see Base64Sink
 */
class Base64Source(delegate: Source): AbstractBase64Source(delegate) {

    companion object: KLogging()

    override fun decodeBase64Bytes(encodedString: String): ByteString? {
        return encodedString.decodeBase64()
    }

}
