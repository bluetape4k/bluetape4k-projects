package io.bluetape4k.assertions

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class ExceptionsTest {

    // ── invoking { }.shouldThrow ──────────────────────────────────────────

    @Test
    fun `invoking shouldThrow catches expected exception type`() {
        val ex = invoking { throw IllegalStateException("oops") }
            .shouldThrow(IllegalStateException::class)
        ex.message shouldBeEqualTo "oops"
    }

    @Test
    fun `invoking shouldThrow catches subclass of expected type`() {
        invoking { throw IllegalArgumentException("arg") }
            .shouldThrow(RuntimeException::class)
    }

    @Test
    fun `invoking shouldThrow fails when type does not match`() {
        assertFailsWith<AssertionFailedError> {
            invoking { throw IllegalStateException("oops") }
                .shouldThrow(IllegalArgumentException::class)
        }
    }

    @Test
    fun `invoking shouldThrow fails when no exception is thrown`() {
        assertFailsWith<AssertionFailedError> {
            invoking { 42 }
                .shouldThrow(IllegalStateException::class)
        }
    }

    // ── invoking { }.shouldNotThrow ───────────────────────────────────────

    @Test
    fun `invoking shouldNotThrow passes when block executes normally`() {
        val result = invoking { 42 }.shouldNotThrow()
        result shouldBeEqualTo 42
    }

    @Test
    fun `invoking shouldNotThrow fails when exception is thrown`() {
        assertFailsWith<AssertionFailedError> {
            invoking { throw IllegalStateException("oops") }.shouldNotThrow()
        }
    }

    // ── withMessage ──────────────────────────────────────────────────────

    @Test
    fun `invoking withMessage passes when message matches exactly`() {
        invoking { throw IllegalStateException("exact message") }
            .withMessage("exact message")
    }

    @Test
    fun `invoking withMessage fails when message does not match exactly`() {
        assertFailsWith<AssertionFailedError> {
            invoking { throw IllegalStateException("actual message") }
                .withMessage("expected message")
        }
    }

    // ── withMessageContaining ────────────────────────────────────────────

    @Test
    fun `invoking withMessageContaining passes for substring match`() {
        invoking { throw IllegalStateException("the actual error happened") }
            .withMessageContaining("actual error")
    }

    @Test
    fun `invoking withMessageContaining fails when substring not present`() {
        assertFailsWith<AssertionFailedError> {
            invoking { throw IllegalStateException("the message") }
                .withMessageContaining("missing")
        }
    }

    // ── withMessageMatching ──────────────────────────────────────────────

    @Test
    fun `invoking withMessageMatching passes for regex match`() {
        invoking { throw IllegalStateException("error 42 occurred") }
            .withMessageMatching(Regex("""error \d+ occurred"""))
    }

    @Test
    fun `invoking withMessageMatching fails when regex does not match`() {
        assertFailsWith<AssertionFailedError> {
            invoking { throw IllegalStateException("plain message") }
                .withMessageMatching(Regex("""error \d+"""))
        }
    }

    // ── withCause ────────────────────────────────────────────────────────

    @Test
    fun `invoking withCause passes when cause matches`() {
        val rootCause = IllegalArgumentException("root")
        invoking { throw IllegalStateException("wrapper", rootCause) }
            .withCause(IllegalArgumentException::class)
    }

    @Test
    fun `invoking withCause passes for cause subclass`() {
        val rootCause = IllegalArgumentException("root")
        invoking { throw IllegalStateException("wrapper", rootCause) }
            .withCause(RuntimeException::class)
    }

    @Test
    fun `invoking withCause fails when cause is null`() {
        assertFailsWith<AssertionFailedError> {
            invoking { throw IllegalStateException("no cause") }
                .withCause(IllegalArgumentException::class)
        }
    }

    @Test
    fun `invoking withCause fails when cause type does not match`() {
        val rootCause = NullPointerException("npe")
        assertFailsWith<AssertionFailedError> {
            invoking { throw IllegalStateException("wrapper", rootCause) }
                .withCause(IllegalArgumentException::class)
        }
    }

    // ── with (custom block) ───────────────────────────────────────────────

    @Test
    fun `invoking with custom block validates exception properties`() {
        invoking { throw IllegalStateException("custom") }
            .with {
                message shouldBeEqualTo "custom"
            }
    }

    @Test
    fun `invoking with custom block fails when nested assertion fails`() {
        assertFailsWith<AssertionFailedError> {
            invoking { throw IllegalStateException("actual") }
                .with {
                    message shouldBeEqualTo "expected"
                }
        }
    }

    // ── chaining ─────────────────────────────────────────────────────────

    @Test
    fun `invoking supports chaining shouldThrow with message validators`() {
        val rootCause = IllegalArgumentException("root")
        invoking { throw IllegalStateException("wrapper error", rootCause) }
            .shouldThrow(IllegalStateException::class)

        invoking { throw IllegalStateException("wrapper error", rootCause) }
            .withMessage("wrapper error")
            .withMessageContaining("wrapper")
            .withCause(IllegalArgumentException::class)
    }

    // ── coInvoking { }.shouldThrow ────────────────────────────────────────

    @Test
    fun `coInvoking shouldThrow catches expected exception type`() = runTest {
        val ex = coInvoking { throw IllegalStateException("co-oops") }
            .shouldThrow(IllegalStateException::class)
        ex.message shouldBeEqualTo "co-oops"
    }

    @Test
    fun `coInvoking shouldThrow fails when type does not match`() = runTest {
        assertFailsWith<AssertionFailedError> {
            coInvoking { throw IllegalStateException("oops") }
                .shouldThrow(IllegalArgumentException::class)
        }
    }

    @Test
    fun `coInvoking shouldThrow fails when no exception is thrown`() = runTest {
        assertFailsWith<AssertionFailedError> {
            coInvoking { 42 }
                .shouldThrow(IllegalStateException::class)
        }
    }

    // ── coInvoking { }.shouldNotThrow ─────────────────────────────────────

    @Test
    fun `coInvoking shouldNotThrow passes when block executes normally`() = runTest {
        val result = coInvoking { "ok" }.shouldNotThrow()
        result shouldBeEqualTo "ok"
    }

    @Test
    fun `coInvoking shouldNotThrow fails when exception is thrown`() = runTest {
        assertFailsWith<AssertionFailedError> {
            coInvoking { throw IllegalStateException("oops") }.shouldNotThrow()
        }
    }

    // ── coInvoking withMessage / withMessageContaining / withMessageMatching

    @Test
    fun `coInvoking withMessage passes when message matches exactly`() = runTest {
        coInvoking { throw IllegalStateException("exact") }
            .withMessage("exact")
    }

    @Test
    fun `coInvoking withMessage fails when message differs`() = runTest {
        assertFailsWith<AssertionFailedError> {
            coInvoking { throw IllegalStateException("actual") }
                .withMessage("expected")
        }
    }

    @Test
    fun `coInvoking withMessageContaining passes for substring`() = runTest {
        coInvoking { throw IllegalStateException("the actual error happened") }
            .withMessageContaining("actual error")
    }

    @Test
    fun `coInvoking withMessageContaining fails when substring missing`() = runTest {
        assertFailsWith<AssertionFailedError> {
            coInvoking { throw IllegalStateException("the message") }
                .withMessageContaining("missing")
        }
    }

    @Test
    fun `coInvoking withMessageMatching passes for regex match`() = runTest {
        coInvoking { throw IllegalStateException("error 42 occurred") }
            .withMessageMatching(Regex("""error \d+ occurred"""))
    }

    @Test
    fun `coInvoking withMessageMatching fails for regex mismatch`() = runTest {
        assertFailsWith<AssertionFailedError> {
            coInvoking { throw IllegalStateException("plain") }
                .withMessageMatching(Regex("""error \d+"""))
        }
    }

    // ── coInvoking withCause ──────────────────────────────────────────────

    @Test
    fun `coInvoking withCause passes when cause matches`() = runTest {
        val rootCause = IllegalArgumentException("root")
        coInvoking { throw IllegalStateException("wrapper", rootCause) }
            .withCause(IllegalArgumentException::class)
    }

    @Test
    fun `coInvoking withCause fails when cause is null`() = runTest {
        assertFailsWith<AssertionFailedError> {
            coInvoking { throw IllegalStateException("no cause") }
                .withCause(IllegalArgumentException::class)
        }
    }

    // ── coInvoking with (custom block) ────────────────────────────────────

    @Test
    fun `coInvoking with custom block validates exception properties`() = runTest {
        coInvoking { throw IllegalStateException("custom") }
            .with {
                message shouldBeEqualTo "custom"
            }
    }

    // ── CancellationException rethrow (CRITICAL) ──────────────────────────

    @Test
    fun `coInvoking CancellationException is rethrown when different type expected`() = runTest {
        // Note: java.util.concurrent.CancellationException extends IllegalStateException on JVM,
        // so we use IllegalArgumentException (a true sibling under RuntimeException) to verify rethrow.
        val caught = kotlin.runCatching {
            coInvoking { throw CancellationException("cancelled") }
                .shouldThrow(IllegalArgumentException::class)
        }
        // CancellationException 이 AssertionFailedError 가 아닌 CancellationException 으로 전파되어야 함
        assert(caught.exceptionOrNull() is CancellationException) {
            "Expected CancellationException but got: ${caught.exceptionOrNull()}"
        }
    }

    @Test
    fun `coInvoking CancellationException is caught when expected type matches`() = runTest {
        val ex = coInvoking { throw CancellationException("cancelled") }
            .shouldThrow(CancellationException::class)
        ex.message shouldBeEqualTo "cancelled"
    }

    @Test
    fun `coInvoking shouldNotThrow rethrows CancellationException`() = runTest {
        val caught = kotlin.runCatching {
            coInvoking { throw CancellationException("cancelled") }.shouldNotThrow()
        }
        assert(caught.exceptionOrNull() is CancellationException) {
            "Expected CancellationException but got: ${caught.exceptionOrNull()}"
        }
    }

    @Test
    fun `coInvoking withMessage rethrows CancellationException`() = runTest {
        val caught = kotlin.runCatching {
            coInvoking { throw CancellationException("cancelled") }
                .withMessage("cancelled")
        }
        assert(caught.exceptionOrNull() is CancellationException) {
            "Expected CancellationException but got: ${caught.exceptionOrNull()}"
        }
    }
}
