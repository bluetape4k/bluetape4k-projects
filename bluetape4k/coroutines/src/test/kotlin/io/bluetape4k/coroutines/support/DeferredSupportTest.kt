package io.bluetape4k.coroutines.support

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DeferredSupportTest {

    @Test
    fun `zip은 두 deferred 결과를 결합한다`() = runTest {
        val d1 = async { 10 }
        val d2 = async { 20 }

        val zipped = zip(d1, d2) { a, b -> a + b }
        zipped.await() shouldBeEqualTo 30
    }

    @Test
    fun `map, mapAll, concatMap은 deferred 결과를 변환한다`() = runTest {
        val source: Deferred<List<Int>> = async { listOf(1, 2, 3) }

        source.mapAll { listOf(it, it * 10) }.await() shouldBeEqualTo listOf(1, 10, 2, 20, 3, 30)
        source.concatMap { it * 2 }.await() shouldBeEqualTo listOf(2, 4, 6)
        source.map { it.sum() }.await() shouldBeEqualTo 6
    }

    @Test
    fun `awaitAny는 먼저 완료되는 deferred 값을 반환한다`() = runTest {
        val slow = async { delay(100); 2 }
        val fast = async { delay(10); 1 }

        awaitAny(slow, fast) shouldBeEqualTo 1
        listOf(slow, fast).awaitAny() shouldBeEqualTo 1
    }

    @Test
    fun `awaitAny는 단일 deferred인 경우 바로 await 한다`() = runTest {
        val only = async { 7 }

        listOf(only).awaitAny() shouldBeEqualTo 7
        listOf(only).awaitAnyAndCancelOthers() shouldBeEqualTo 7
    }

    @Test
    fun `awaitAny 계열은 빈 입력을 허용하지 않는다`() = runTest {
        assertFailsWith<IllegalArgumentException> { awaitAny<Int>() }
        assertFailsWith<IllegalArgumentException> { emptyList<CompletableDeferred<Int>>().awaitAny() }
        assertFailsWith<IllegalArgumentException> { emptyList<CompletableDeferred<Int>>().awaitAnyAndCancelOthers() }
    }

    @Test
    fun `awaitAnyAndCancelOthers는 첫 완료값 반환 후 나머지를 취소한다`() = runTest {
        val first = CompletableDeferred(1)
        val second = CompletableDeferred<Int>()
        val third = CompletableDeferred<Int>()

        val result = listOf(first, second, third).awaitAnyAndCancelOthers()
        result shouldBeEqualTo 1

        second.isCancelled.shouldBeTrue()
        third.isCancelled.shouldBeTrue()
        assertTrue(first.isCompleted)
    }
}
