package io.bluetape4k.okio.base64

import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import okio.Sink
import okio.Buffer
import org.junit.jupiter.api.Test

class ApacheBase64SinkTest: AbstractBaseNSinkTest() {

    companion object: KLogging()

    override fun createSink(delegate: Sink): Sink = ApacheBase64Sink(delegate)

    override fun getEncodedString(expectedBytes: ByteArray): String {
        return expectedBytes.encodeBase64String()
    }

    @Test
    fun `factory wraps delegates and reuses an existing wrapper`() {
        val delegate = Buffer()
        val wrapped = delegate.asApacheBase64Sink()

        ((wrapped as Any) !== (delegate as Any)).shouldBeTrue()
        (wrapped.asApacheBase64Sink() === wrapped).shouldBeTrue()
    }
}
