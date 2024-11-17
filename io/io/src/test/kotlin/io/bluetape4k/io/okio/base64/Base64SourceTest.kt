package io.bluetape4k.io.okio.base64

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import okio.ByteString.Companion.toByteString
import okio.Source

class Base64SourceTest: AbstractBaseNSourceTest() {

    companion object: KLogging()

    override fun getSource(delegate: Source): Source {
        return Base64Source(delegate)
    }

    override fun getEncodedString(plainString: String): String {
        return plainString.toUtf8Bytes().toByteString().base64()
    }
}
