package io.bluetape4k.assertions

import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.Test
import org.opentest4j.MultipleFailuresError

class SoftlyTest {

    @Test
    fun `assertSoftly passes when all assertions succeed`() {
        assertSoftly {
            add { 1 shouldBeEqualTo 1 }
            add { "hello" shouldBeEqualTo "hello" }
            add { true.shouldBeTrue() }
        }
    }

    @Test
    fun `assertSoftly collects all failures into MultipleFailuresError`() {
        val ex = assertFailsWith<MultipleFailuresError> {
            assertSoftly {
                add { 1 shouldBeEqualTo 2 }       // fail
                add { "a" shouldBeEqualTo "b" }    // fail
                add { 3 shouldBeEqualTo 3 }        // pass
            }
        }
        assert(ex.failures.size == 2) { "Expected 2 failures, got ${ex.failures.size}" }
    }

    @Test
    fun `assertSoftly reports all failure messages`() {
        val ex = assertFailsWith<MultipleFailuresError> {
            assertSoftly {
                add { 1 shouldBeEqualTo 99 }
                add { "hello" shouldBeEqualTo "world" }
            }
        }
        val messages = ex.failures.joinToString { it.message ?: "" }
        assert(messages.contains("99")) { "Should contain expected value" }
    }

    @Test
    fun `assertSoftly with no failures completes normally`() {
        assertSoftly { }
    }

    @Test
    fun `assertSoftly scope is independent between calls`() {
        assertSoftly {
            add { 1 shouldBeEqualTo 1 }
        }
        assertSoftly {
            add { 2 shouldBeEqualTo 2 }
        }
    }
}
