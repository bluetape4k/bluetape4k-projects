package io.bluetape4k.coroutines.tests

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * Deprecated bridge 함수가 WARNING만 발생하고 정상 동작함을 검증합니다.
 *
 * 각 deprecated 함수를 한 번씩 호출하여 예외 없이 통과하는지 확인합니다.
 */
@Suppress("DEPRECATION")
class FlowAssertionsBridgeTest {

    @Test
    fun `assertEmpty - deprecated 함수가 정상 동작한다`() = runTest {
        emptyFlow<Int>().assertEmpty()
    }

    @Test
    fun `assertResult with Flow - deprecated 함수가 정상 동작한다`() = runTest {
        flowOf(1, 2, 3).assertResult(flowOf(1, 2, 3))
    }

    @Test
    fun `assertResult with vararg - deprecated 함수가 정상 동작한다`() = runTest {
        flowOf(1, 2, 3).assertResult(1, 2, 3)
    }

    @Test
    fun `assertResultSet with vararg - deprecated 함수가 정상 동작한다`() = runTest {
        flowOf(2, 1, 2).assertResultSet(1, 2)
    }

    @Test
    fun `assertResultSet with Iterable - deprecated 함수가 정상 동작한다`() = runTest {
        flowOf(2, 1, 2).assertResultSet(listOf(1, 2))
    }

    @Test
    fun `assertFailure - deprecated 함수가 정상 동작한다`() = runTest {
        val failingFlow = flow {
            emit(1)
            emit(2)
            throw IllegalStateException("expected failure")
        }
        failingFlow.assertFailure<Int, IllegalStateException>(1, 2)
    }

    @Test
    fun `assertError - deprecated 함수가 정상 동작한다`() = runTest {
        val failingFlow = flow<Int> {
            throw IllegalStateException("expected error")
        }
        failingFlow.assertError<IllegalStateException>()
    }
}
