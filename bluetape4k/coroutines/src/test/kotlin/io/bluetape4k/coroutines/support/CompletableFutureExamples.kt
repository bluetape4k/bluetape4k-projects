package io.bluetape4k.coroutines.support

import io.bluetape4k.exceptions.BluetapeException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertFailsWith

class CompletableFutureExamples {

    companion object: KLogging()

    @Test
    fun `성공하는 suspend 함수를 CompletableFuture로 변환하기`() = runTest {
        val future = future {
            delay(Random.nextLong(10))
            42
        }

        future.coAwait() shouldBeEqualTo 42
    }

    @Test
    fun `예외를 발생시키는 suspend 함수를 CompletableFuture로 변환하기`() = runTest {
        // 예외를 Catch하기 위해 SupervisorJob을 사용합니다.
        assertFailsWith<BluetapeException> {
            withContext(SupervisorJob()) {
                val future = future {
                    delay(Random.nextLong(10, 20))
                    log.debug { "예외를 발생시킵니다." }
                    throw BluetapeException("Boom!")
                }

                log.debug { "예외가 발생했는지 확인합니다." }
                future.coAwait()
            }
        }
    }

    @Test
    fun `suspend 함수를 CompletableFuture로 변환하고, 취소하면 CancellationException이 발생한다`() = runTest {
        val future = future {
            delay(100)
            42
        }
        val deferred = async { future.coAwait() }

        future.cancel(false).shouldBeTrue()
        future.isCancelled.shouldBeTrue()

        yield()
        deferred.isCancelled.shouldBeTrue()

        assertFailsWith<CancellationException> {
            deferred.await()
        }
    }
}
