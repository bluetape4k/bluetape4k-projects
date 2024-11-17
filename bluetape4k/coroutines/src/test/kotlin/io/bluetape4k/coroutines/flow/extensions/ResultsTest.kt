package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class ResultsTest: AbstractFlowTest() {

    companion object: KLogging()

    private val testException = RuntimeException()

    @Test
    fun `mapToResult - 성공 case`() = runTest {
        flowOf(1, 2, 3)
            .mapToResult()
            .test {
                awaitItem() shouldBeEqualTo Result.success(1)
                awaitItem() shouldBeEqualTo Result.success(2)
                awaitItem() shouldBeEqualTo Result.success(3)
                awaitComplete()
            }
    }

    @Test
    fun `mapToResult - 실패 case`() = runTest {
        flow {
            emit(1)
            throw testException
        }
            .mapToResult()
            .test {
                awaitItem() shouldBeEqualTo Result.success(1)
                awaitItem() shouldBeEqualTo Result.failure(testException)
                awaitComplete()
            }
    }

    @Test
    fun `mapResultCatching - 성공 case`() = runTest {
        flowOf(1, 2, 3)
            .mapToResult()
            .mapResultCatching { it * 2 }
            .test {
                awaitItem() shouldBeEqualTo Result.success(2)
                awaitItem() shouldBeEqualTo Result.success(4)
                awaitItem() shouldBeEqualTo Result.success(6)
                awaitComplete()
            }
    }

    @Test
    fun `mapResultCatching - 실패 case`() = runTest {
        flow {
            emit(1)
            throw testException
        }
            .mapToResult()
            .mapResultCatching { it * 2 }
            .test {
                awaitItem() shouldBeEqualTo Result.success(2)
                awaitItem() shouldBeEqualTo Result.failure(testException)
                awaitComplete()
            }
    }

    @Test
    fun `mapResultCatching with exception`() = runTest {
        flowOf(1, 2, 3)
            .mapToResult()
            .mapResultCatching { throw testException }
            .test {
                awaitItem() shouldBeEqualTo Result.failure(testException)
                awaitItem() shouldBeEqualTo Result.failure(testException)
                awaitItem() shouldBeEqualTo Result.failure(testException)
                awaitComplete()
            }

        // Does not catch CancellationException
        val receivedFirst = CompletableDeferred<Unit>()
        val job = launch {
            val receivedResults = mutableListOf<Result<String>>()

            flowOf(1, 2, 3).mapToResult()
                .mapResultCatching {
                    if (it == 2) awaitCancellation()
                    else it.toString()
                }
                .collect {
                    receivedResults += it
                    receivedFirst.complete(Unit)
                }
        }

        receivedFirst.await()
        job.cancelAndJoin()
    }

    @Test
    fun `mapResultCatching - forward failure values`() = runTest {
        val result = Result.failure<Int>(testException)

        flowOf(result)
            .mapResultCatching<Int, String> { fail("Should not be called.") }
            .test {
                awaitItem() shouldBeEqualTo Result.failure(testException)
                awaitComplete()
            }

    }

    @Test
    fun `throwFailure - 성공 case`() = runTest {
        flowOf(1, 2, 3)
            .mapToResult()
            .throwFailure()
            .test {
                awaitItem() shouldBeEqualTo 1
                awaitItem() shouldBeEqualTo 2
                awaitItem() shouldBeEqualTo 3
                awaitComplete()
            }
    }

    @Test
    fun `throwFailure - 예외 case`() = runTest {
        flow {
            emit(1)
            throw testException
        }
            .mapToResult()
            .throwFailure()
            .test {
                awaitItem() shouldBeEqualTo 1
                awaitError() shouldBeEqualTo testException
            }
    }

    @Test
    fun `getOrDefault - 성공 case`() = runTest {
        flowOf(1, 2, 3)
            .mapToResult()
            .getOrDefault(-1)
            .test {
                awaitItem() shouldBeEqualTo 1
                awaitItem() shouldBeEqualTo 2
                awaitItem() shouldBeEqualTo 3
                awaitComplete()
            }
    }

    @Test
    fun `getOrDefault - 예외 case`() = runTest {
        flow {
            emit(1)
            throw testException
        }
            .mapToResult()
            .getOrDefault(-1)
            .test {
                awaitItem() shouldBeEqualTo 1
                awaitItem() shouldBeEqualTo -1
                awaitComplete()
            }
    }
}
