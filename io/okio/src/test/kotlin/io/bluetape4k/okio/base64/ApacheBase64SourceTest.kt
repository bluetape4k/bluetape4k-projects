package io.bluetape4k.okio.base64

import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.Source
import org.junit.jupiter.api.Test

class ApacheBase64SourceTest: AbstractBaseNSourceTest() {

    companion object: KLogging()

    override fun getSource(delegate: Source): Source {
        return ApacheBase64Source(delegate)
    }

    override fun getEncodedString(plainString: String): String {
        return plainString.encodeBase64String()
    }

    @Test
    fun `factory wraps delegates and reuses an existing wrapper`() {
        val delegate = Buffer()
        val wrapped = delegate.asApacheBase64Source()

        ((wrapped as Any) !== (delegate as Any)).shouldBeTrue()
        (wrapped.asApacheBase64Source() === wrapped).shouldBeTrue()
    }
}
