package io.bluetape4k.okio.base64

import io.bluetape4k.logging.KLogging
import io.bluetape4k.okio.byteStringOf
import io.bluetape4k.assertions.shouldBeTrue
import okio.Buffer
import okio.Sink
import org.junit.jupiter.api.Test

class OkioBase64SinkTest: AbstractBaseNSinkTest() {

    companion object: KLogging()

    override fun createSink(delegate: Sink): Sink = OkioBase64Sink(delegate)

    override fun getEncodedString(expectedBytes: ByteArray): String {
        return byteStringOf(expectedBytes).base64()
    }

    @Test
    fun `factory wraps delegates and reuses an existing wrapper`() {
        val delegate = Buffer()
        val wrapped = delegate.asBase64Sink()

        ((wrapped as Any) !== (delegate as Any)).shouldBeTrue()
        (wrapped.asBase64Sink() === wrapped).shouldBeTrue()
    }
}
