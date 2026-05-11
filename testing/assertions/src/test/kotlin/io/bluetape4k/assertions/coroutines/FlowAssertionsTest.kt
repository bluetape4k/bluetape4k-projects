package io.bluetape4k.assertions.coroutines

import io.bluetape4k.assertions.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class FlowAssertionsTest {

    @Test
    fun `assertEmpty passes for empty flow`() = runTest {
        flowOf<Int>().assertEmpty()
    }

    @Test
    fun `assertEmpty fails for non-empty flow`() = runTest {
        assertFailsWith<AssertionFailedError> {
            flowOf(1, 2, 3).assertEmpty()
        }
    }

    @Test
    fun `assertResult with vararg passes for matching sequence`() = runTest {
        flowOf(1, 2, 3).assertResult(1, 2, 3)
    }

    @Test
    fun `assertResult with vararg fails for wrong order`() = runTest {
        assertFailsWith<AssertionFailedError> {
            flowOf(1, 2, 3).assertResult(3, 2, 1)
        }
    }

    @Test
    fun `assertResult with Flow passes for matching sequence`() = runTest {
        flowOf(1, 2, 3).assertResult(flowOf(1, 2, 3))
    }

    @Test
    fun `assertResultSet passes regardless of order`() = runTest {
        flowOf(3, 1, 2).assertResultSet(1, 2, 3)
    }

    @Test
    fun `assertResultSet compares duplicate counts`() = runTest {
        assertFailsWith<AssertionFailedError> {
            flowOf(1, 1, 2).assertResultSet(1, 2, 2)
        }
    }

    @Test
    fun `assertResultSet fails when elements differ`() = runTest {
        assertFailsWith<AssertionFailedError> {
            flowOf(1, 2).assertResultSet(1, 99)
        }
    }

    @Test
    fun `assertResultSet with Iterable passes`() = runTest {
        flowOf("b", "a").assertResultSet(listOf("a", "b"))
    }

    @Test
    fun `assertFailure passes when flow emits values then throws`() = runTest {
        val errorFlow = flow {
            emit(1)
            emit(2)
            throw IllegalStateException("oops")
        }
        errorFlow.assertFailure<Int, IllegalStateException>(1, 2)
    }

    @Test
    fun `assertFailure fails when exception type does not match`() = runTest {
        val errorFlow = flow<Int> {
            throw IllegalStateException("oops")
        }
        assertFailsWith<AssertionFailedError> {
            errorFlow.assertFailure<Int, IllegalArgumentException>()
        }
    }

    @Test
    fun `assertError passes when flow throws expected exception`() = runTest {
        val errorFlow = flow<Int> {
            throw RuntimeException("boom")
        }
        errorFlow.assertError<RuntimeException>()
    }

    @Test
    fun `assertError fails when flow completes normally`() = runTest {
        assertFailsWith<AssertionFailedError> {
            flowOf(1, 2).assertError<RuntimeException>()
        }
    }

    @Test
    fun `assertError fails when wrong exception type`() = runTest {
        val errorFlow = flow<Int> {
            throw IllegalStateException("state")
        }
        assertFailsWith<AssertionFailedError> {
            errorFlow.assertError<IllegalArgumentException>()
        }
    }

    @Test
    fun `assertFailure rethrows CancellationException`() = runTest {
        val cancelledFlow = flow<Int> {
            emit(1)
            throw CancellationException("cancelled")
        }

        assertFailsWith<CancellationException> {
            cancelledFlow.assertFailure<Int, IllegalStateException>(1)
        }
    }

    @Test
    fun `assertError rethrows CancellationException`() = runTest {
        val cancelledFlow = flow<Int> {
            throw CancellationException("cancelled")
        }

        assertFailsWith<CancellationException> {
            cancelledFlow.assertError<IllegalStateException>()
        }
    }
}
