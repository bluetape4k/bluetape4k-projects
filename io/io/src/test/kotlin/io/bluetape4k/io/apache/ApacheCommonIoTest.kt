package io.bluetape4k.io.apache

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ApacheCommonIoTest {

    @Test
    fun `ApacheByteArrayOutputStream aliases commons implementation`() {
        val out = ApacheByteArrayOutputStream()
        val data = "hello".toByteArray()
        out.write(data)

        out.toByteArray().decodeToString() shouldBeEqualTo "hello"
        out.reset()
        out.size() shouldBeEqualTo 0
    }
}
