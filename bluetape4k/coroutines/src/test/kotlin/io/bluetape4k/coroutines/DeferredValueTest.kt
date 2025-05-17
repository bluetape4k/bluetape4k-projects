package io.bluetape4k.coroutines

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import kotlin.random.Random

@RandomizedTest
class DeferredValueTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `값 계산은 async로 시작합니다`() = runTest {
        // given
        val dv = deferredValueOf {
            log.trace { "Calc deferred value ... " }
            delay(Random.nextLong(10, 20))
            System.currentTimeMillis()
        }
        val createdTime = System.currentTimeMillis()
        yield()

        dv.isActive.shouldBeTrue()
        dv.isCompleted.shouldBeFalse()

        // 초기화 진행 후 반환합니다. 이미 초기화가 끝난 후에는 바로 반환합니다.
        dv.value shouldBeGreaterThan createdTime

        dv.isActive.shouldBeFalse()
        dv.isCompleted.shouldBeTrue()
    }


    @RepeatedTest(REPEAT_SIZE)
    fun `map deferred value`() = runTest {
        val dv1 = deferredValueOf {
            log.trace { "Calc deferred value ... " }
            delay(Random.nextLong(10, 20))
            42
        }

        val dv2 = dv1.map {
            log.trace { "Map deferred value ... " }
            delay(Random.nextLong(10, 20))
            it * 2
        }

        dv1.isCompleted.shouldBeFalse()
        dv2.isCompleted.shouldBeFalse()

        dv1.await() shouldBeEqualTo 42
        dv2.await() shouldBeEqualTo 42 * 2

        dv1.isCompleted.shouldBeTrue()
        dv2.isCompleted.shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `flatmap deferred value`() = runTest {
        val dv1: DeferredValue<DeferredValue<Int>> = deferredValueOf {
            log.trace { "Calc deferred value ... " }
            delay(Random.nextLong(10, 20))

            deferredValueOf { 42 }
        }

        val dv2: DeferredValue<Int> = dv1.flatMap { r ->
            r.map {
                log.trace { "Map deferred value ... " }
                delay(Random.nextLong(10, 20))
                it * 2
            }
        }

        dv2.isCompleted.shouldBeFalse()
        dv2.await() shouldBeEqualTo 42 * 2
        dv2.isCompleted.shouldBeTrue()
    }
}
