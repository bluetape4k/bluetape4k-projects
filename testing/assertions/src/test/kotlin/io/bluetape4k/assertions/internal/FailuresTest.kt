package io.bluetape4k.assertions.internal

import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import org.opentest4j.MultipleFailuresError

class FailuresTest {

    @Test
    fun `fail throws AssertionFailedError with message`() {
        val ex = assertFailsWith<AssertionFailedError> {
            Failures.fail("something went wrong")
        }
        assert(ex.message == "something went wrong")
    }

    @Test
    fun `failComparison populates expected and actual for IntelliJ diff`() {
        val ex = assertFailsWith<AssertionFailedError> {
            Failures.failComparison("not equal", "hello", "world")
        }
        assert(ex.expected?.value == "hello")
        assert(ex.actual?.value == "world")
    }

    @Test
    fun `failWithCause wraps cause throwable`() {
        val cause = IllegalStateException("root cause")
        val ex = assertFailsWith<AssertionFailedError> {
            Failures.failWithCause("wrapped", cause)
        }
        assert(ex.cause === cause)
    }

    @Test
    fun `failMultiple collects all failures into MultipleFailuresError`() {
        val f1 = AssertionFailedError("first")
        val f2 = AssertionFailedError("second")
        val ex = assertFailsWith<MultipleFailuresError> {
            Failures.failMultiple("2 failures", listOf(f1, f2))
        }
        assert(ex.failures.size == 2)
    }
}
