package io.bluetape4k.io.okio.base64

import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.logging.KLogging
import okio.Sink

class ApacheBase64SinkTest: AbstractBaseNSinkTest() {

    companion object: KLogging()

    override fun createSink(delegate: Sink): Sink = ApacheBase64Sink(delegate)

    override fun getEncodedString(expectedBytes: ByteArray): String {
        return expectedBytes.encodeBase64String()
    }
}
