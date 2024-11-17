package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ErrorsTest: AbstractFlowTest() {

    companion object: KLogging()

    val testException = RuntimeException("Boom!")

    @Test
    fun `예외 발생 시 fallback으로 대체`() = runTest {
        flow {
            emit(42)
            throw testException
            emit(43)
        }
            .catchAndReturn(-1)             // 예외 발생 시, -1 로 대체
            .test {
                awaitItem() shouldBeEqualTo 42
                awaitItem() shouldBeEqualTo -1
                awaitComplete()
            }
    }

    @Test
    fun `예외가 없는 경우는 그대로`() = runTest {
        flowOf(1, 2, 3)
            .catchAndReturn(-1)
            .test {
                awaitItem() shouldBeEqualTo 1
                awaitItem() shouldBeEqualTo 2
                awaitItem() shouldBeEqualTo 3
                awaitComplete()
            }
    }

    @Test
    fun `예외 발생 시 fallback으로 대체 (람다 사용)`() = runTest {
        var count = 42

        flow {
            emit(1)
            throw testException
        }
            .catchAndReturn { count++ }
            .test {
                awaitItem() shouldBeEqualTo 1
                awaitItem() shouldBeEqualTo 42
                awaitComplete()
            }

        count shouldBeEqualTo 43
    }

    @Test
    fun `예외가 없을 시 fallback으로 대체 (람다 사용) 안함`() = runTest {
        var count = 42

        flowOf(1, 2, 3)
            .catchAndReturn { count++ }
            .test {
                awaitItem() shouldBeEqualTo 1
                awaitItem() shouldBeEqualTo 2
                awaitItem() shouldBeEqualTo 3
                awaitComplete()
            }

        count shouldBeEqualTo 42
    }

    @Test
    fun `예외 발생 시 fallback flow로 대체`() = runTest {
        var count = 2

        val flow = flow {
            emit(1)
            throw testException
            emit(2)
        }
            .catchAndResume { flowOf(count, count + 1).also { count++ } }


        flow.test {
            awaitItem() shouldBeEqualTo 1
            awaitItem() shouldBeEqualTo 2
            awaitItem() shouldBeEqualTo 3
            awaitComplete()
        }
        count shouldBeEqualTo 3

        flow.test {
            awaitItem() shouldBeEqualTo 1
            awaitItem() shouldBeEqualTo 3
            awaitItem() shouldBeEqualTo 4
            awaitComplete()
        }

        count shouldBeEqualTo 4
    }

    @Test
    fun `정상상태에서는 catch 안함`() = runTest {
        val flow = flowOf(1, 2, 3)
            .catchAndResume { flow { fail("Should be unreached.") } }

        flow.test {
            awaitItem() shouldBeEqualTo 1
            awaitItem() shouldBeEqualTo 2
            awaitItem() shouldBeEqualTo 3
            awaitComplete()
        }
    }

    @Test
    fun `catch and resume with flow`() = runTest {
        var count = 2

        val flow = flow {
            emit(1)
            throw testException
        }.catchAndResume { error ->
            error shouldBeInstanceOf RuntimeException::class
            delay(100L)
            flowOf(count, count + 1).also { count++ }
        }

        flow.test {
            awaitItem() shouldBeEqualTo 1
            awaitItem() shouldBeEqualTo 2
            awaitItem() shouldBeEqualTo 3
            awaitComplete()
        }
        count shouldBeEqualTo 3

        flow.test {
            awaitItem() shouldBeEqualTo 1
            awaitItem() shouldBeEqualTo 3
            awaitItem() shouldBeEqualTo 4
            awaitComplete()
        }

        count shouldBeEqualTo 4
    }

    @Test
    fun `catch and resume with flow without exception`() = runTest {
        var count = 2

        val flow = flowOf(1, 2, 3)
            .catchAndResume { error ->
                error shouldBeInstanceOf RuntimeException::class
                delay(100L)
                flowOf(count, count + 1).also { count++ }
            }

        flow.test {
            awaitItem() shouldBeEqualTo 1
            awaitItem() shouldBeEqualTo 2
            awaitItem() shouldBeEqualTo 3
            awaitComplete()
        }
        count shouldBeEqualTo 2
    }
}
