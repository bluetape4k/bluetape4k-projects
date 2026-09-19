package io.bluetape4k.io.apache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class ApacheCommonIoTest {

    companion object: KLogging()

    @Test
    fun `ApacheByteArrayOutputStream aliases commons implementation`() {
        val out = ApacheByteArrayOutputStream()
        val data = "hello".toByteArray()
        out.write(data)

        out.toByteArray().decodeToString() shouldBeEqualTo "hello"
        out.reset()
        out.size() shouldBeEqualTo 0
        out.close()
    }
}
