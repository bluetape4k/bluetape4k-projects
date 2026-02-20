package io.bluetape4k.apache

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
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
