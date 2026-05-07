package io.bluetape4k.assertions.coroutines

import app.cash.turbine.test
import io.bluetape4k.assertions.assertFailsWith
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class TurbineSupportTest {

    @Test
    fun `awaitItemAndAssert passes when item matches`() = runTest {
        flowOf(42).test {
            awaitItemAndAssert(42)
            awaitComplete()
        }
    }

    @Test
    fun `awaitItemAndAssert fails when item does not match`() = runTest {
        assertFailsWith<AssertionFailedError> {
            flowOf(1).test {
                awaitItemAndAssert(99)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `awaitItemMatching passes when predicate holds`() = runTest {
        flowOf(10).test {
            awaitItemMatching { it > 5 }
            awaitComplete()
        }
    }

    @Test
    fun `awaitItemMatching fails when predicate does not hold`() = runTest {
        assertFailsWith<AssertionFailedError> {
            flowOf(3).test {
                awaitItemMatching { it > 100 }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `awaitErrorOfType passes for matching error type`() = runTest {
        val errorFlow = flow<Int> { throw IllegalStateException("boom") }
        errorFlow.test {
            val error = awaitErrorOfType<IllegalStateException>()
            assert(error.message == "boom")
        }
    }

    @Test
    fun `awaitErrorOfType fails for wrong error type`() = runTest {
        val errorFlow = flow<Int> { throw IllegalStateException("boom") }
        assertFailsWith<AssertionFailedError> {
            errorFlow.test {
                awaitErrorOfType<IllegalArgumentException>()
            }
        }
    }
}
