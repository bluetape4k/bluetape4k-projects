package io.bluetape4k.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
abstract class AbstractCoroutineScopeTest {

    companion object: KLoggingChannel()

    abstract fun getCoroutineScope(): CloseableCoroutineScope

    val scope: CloseableCoroutineScope by lazy { getCoroutineScope() }

    private suspend fun add(x: Int, y: Int): Int {
        delay(Random.nextLong(10).milliseconds)
        log.trace { "add($x, $y)" }
        return x + y
    }

    @Test
    @Order(1)
    fun `기본 CoroutineScope 사용`() = runSuspendIO {
        val result1 = scope.async { add(1, 3) }
        val result2 = scope.async { add(2, 4) }

        val sum = result1.await() + result2.await()
        sum shouldBeEqualTo 10
    }

    @Test
    @Order(2)
    fun `CoroutineScope 취소`() = runSuspendIO {
        scope.launch {
            delay(2000.milliseconds)
            fail("작업1은 중간에 취소되어야 합니다.")
        }
        scope.launch {
            delay(2000.milliseconds)
            fail("작업2는 중간에 취소되어야 합니다.")
        }

        delay(10.milliseconds)
        scope.cancel()

        yield()
        scope.isActive.shouldBeFalse()
    }

    @Test
    @Order(3)
    fun `clearJobs는 자식 작업을 취소하고 cause를 전달한다`() = runSuspendIO {
        getCoroutineScope().use { scope ->
            var caught: CancellationException? = null

            val job: Job = scope.launch {
                try {
                    while (true) {
                        delay(10.milliseconds)
                    }
                } catch (e: CancellationException) {
                    caught = e
                    throw e
                }
            }

            delay(50.milliseconds)  // job이 delay(10) 루프에 진입할 때까지 대기
            scope.clearJobs(CancellationException("stop-by-test"))

            job.join()
            caught?.message shouldBeEqualTo "stop-by-test"
            scope.coroutineContext[Job]!!.isActive.shouldBeFalse()
        }
    }
}
