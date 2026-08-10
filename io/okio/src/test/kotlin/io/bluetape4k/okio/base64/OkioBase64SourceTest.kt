package io.bluetape4k.okio.base64

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.assertions.shouldBeTrue
import okio.ByteString.Companion.toByteString
import okio.Buffer
import okio.Source
import org.junit.jupiter.api.Test

class OkioBase64SourceTest: AbstractBaseNSourceTest() {

    companion object: KLogging()

    override fun getSource(delegate: Source): Source {
        return OkioBase64Source(delegate)
    }

    override fun getEncodedString(plainString: String): String {
        return plainString.toUtf8Bytes().toByteString().base64()
    }

    @Test
    fun `factory wraps delegates and reuses an existing wrapper`() {
        val delegate = Buffer()
        val wrapped = delegate.asBase64Source()

        ((wrapped as Any) !== (delegate as Any)).shouldBeTrue()
        (wrapped.asBase64Source() === wrapped).shouldBeTrue()
    }
}
