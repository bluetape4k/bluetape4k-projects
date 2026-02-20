package io.bluetape4k.exceptions

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class ExceptionSupportTest {

    @Test
    fun `BluetapeException should keep message and cause`() {
        val cause = IllegalArgumentException("cause")
        val exception = BluetapeException("message", cause)

        exception.message shouldBeEqualTo "message"
        exception.cause.shouldNotBeNull()
        exception.cause shouldBeInstanceOf IllegalArgumentException::class
    }

    @Test
    fun `NotSupportedException should extend BluetapeException`() {
        val exception = NotSupportedException("not supported")

        exception shouldBeInstanceOf BluetapeException::class
        exception.message shouldBeEqualTo "not supported"
    }
}
