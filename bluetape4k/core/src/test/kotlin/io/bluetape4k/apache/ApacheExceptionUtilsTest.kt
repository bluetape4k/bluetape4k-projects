package io.bluetape4k.apache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ApacheExceptionUtilsTest {

    @Test
    fun `getRootCause returns self when no cause`() {
        val error = IllegalStateException("root")
        error.getRootCause() shouldBeInstanceOf IllegalStateException::class
    }

    @Test
    fun `getRootCause returns deepest cause`() {
        val root = IllegalArgumentException("root")
        val middle = IllegalStateException("middle", root)
        val top = RuntimeException("top", middle)

        top.getRootCause() shouldBeInstanceOf IllegalArgumentException::class
        top.getRootCauseMessage() shouldBeEqualTo "IllegalArgumentException: root"
    }
}
