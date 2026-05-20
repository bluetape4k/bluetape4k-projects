package io.bluetape4k.junit5.coroutines

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test

class CancellationContractsTest {

    @Test
    fun `resultOfNonCancellation returns failure for non cancellation exception`() {
        val result = resultOfNonCancellation {
            throw IllegalStateException("boom")
        }

        result.isFailure.shouldBeTrue()
        result.exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
    }

    @Test
    fun `resultOfNonCancellation rethrows CancellationException`() {
        assertFailsWith<CancellationException> {
            resultOfNonCancellation {
                throw CancellationException("cancel")
            }
        }
    }

    @Test
    fun `runCatchingNonCancellation returns failure for non cancellation exception`() = runTest {
        val result = runCatchingNonCancellation {
            throw IllegalStateException("boom")
        }

        result.isFailure.shouldBeTrue()
        result.exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
    }

    @Test
    fun `runCatchingNonCancellation rethrows CancellationException`() = runTest {
        assertFailsWith<CancellationException> {
            runCatchingNonCancellation {
                throw CancellationException("cancel")
            }
        }
    }

    @Test
    fun `assertCancellationPropagates passes when operation rethrows cancellation`() = runTest {
        assertCancellationPropagates {
            delay(Long.MAX_VALUE)
        }
    }

    @Test
    fun `assertCancellationPropagates fails when operation swallows cancellation`() = runTest {
        assertFailsWith<AssertionError> {
            assertCancellationPropagates {
                try {
                    delay(Long.MAX_VALUE)
                } catch (e: CancellationException) {
                    // Swallowing cancellation must fail the contract.
                }
            }
        }
    }

    @Test
    fun `assertCancellationPropagates fails when operation converts cancellation`() = runTest {
        assertFailsWith<AssertionError> {
            assertCancellationPropagates {
                try {
                    delay(Long.MAX_VALUE)
                } catch (e: CancellationException) {
                    throw IllegalStateException("converted", e)
                }
            }
        }
    }

    @Test
    fun `assertCancellationClearsWaiter completes second waiter`() = runTest {
        val release = CompletableDeferred<Unit>()

        assertCancellationClearsWaiter(
            awaiter = { release.await() },
            releaser = { release.complete(Unit) },
        )
    }

    @Test
    fun `assertResourceCancelledOnCoroutineCancellation verifies resource cancellation`() = runTest {
        var cancelled = false

        assertResourceCancelledOnCoroutineCancellation(
            beforeCancel = { yield() },
            resourceCancelled = { cancelled },
        ) {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                cancelled = true
            }
        }
    }
}
