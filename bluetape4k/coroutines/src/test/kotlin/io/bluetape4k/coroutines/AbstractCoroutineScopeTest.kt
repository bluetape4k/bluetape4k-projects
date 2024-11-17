package io.bluetape4k.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import kotlin.random.Random

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
abstract class AbstractCoroutineScopeTest {

    companion object: KLogging()

    abstract val coroutineScope: CoroutineScope

    private suspend fun add(x: Int, y: Int): Int {
        delay(Random.nextLong(100))
        log.trace { "add($x, $y)" }
        return x + y
    }

    @Test
    @Order(1)
    fun `기본 CoroutineScope 사용`() = runTest {
        val result1 = coroutineScope.async { add(1, 3) }
        val result2 = coroutineScope.async { add(2, 4) }

        val sum = result1.await() + result2.await()
        sum shouldBeEqualTo 10
    }

    @Test
    @Order(2)
    fun `CoroutineScope 취소`() = runTest {
        coroutineScope.launch {
            delay(2000)
            fail("작업 중간에 취소되어야 합니다.")
        }
        coroutineScope.launch {
            delay(2000)
            fail("작업 중간에 취소되어야 합니다.")
        }

        delay(100)
        coroutineScope.cancel()
        coroutineScope.coroutineContext.cancelChildren()

        yield()
        coroutineScope.isActive.shouldBeFalse()
    }
}
