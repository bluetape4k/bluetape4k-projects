package io.bluetape4k.io.okio.base64

import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.logging.KLogging
import okio.Source

class ApacheBase64SourceTest: AbstractBaseNSourceTest() {

    companion object: KLogging()

    override fun getSource(delegate: Source): Source {
        return ApacheBase64Source(delegate)
    }

    override fun getEncodedString(plainString: String): String {
        return plainString.encodeBase64String()
    }
}
