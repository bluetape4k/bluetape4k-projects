package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ScanWithTest: AbstractStepTest() {

    companion object: KLoggingChannel()

    @Test
    fun `call initial supplier per collection`() = runTest {
        var initial = 100
        var nextIndex = 0

        val flow = flowRangeOf(1, 4)
            .scanWith(
                initialSupplier = {
                    expect(nextIndex)
                    delay(100)
                    initial++
                }
            ) { acc, item -> acc + item }

        expect(1)
        nextIndex = 2


        flow.test {
            awaitItem() shouldBeEqualTo 100
            awaitItem() shouldBeEqualTo 100 + 1
            awaitItem() shouldBeEqualTo 100 + 1 + 2
            awaitItem() shouldBeEqualTo 100 + 1 + 2 + 3
            awaitItem() shouldBeEqualTo 100 + 1 + 2 + 3 + 4
            awaitComplete()
        }

        expect(3)
        nextIndex = 4

        flow.test {
            awaitItem() shouldBeEqualTo 101
            awaitItem() shouldBeEqualTo 101 + 1
            awaitItem() shouldBeEqualTo 101 + 1 + 2
            awaitItem() shouldBeEqualTo 101 + 1 + 2 + 3
            awaitItem() shouldBeEqualTo 101 + 1 + 2 + 3 + 4
            awaitComplete()
        }

        finish(5)
    }

    @Test
    fun `when failure upstream`() = runTest {
        assertFailsWith<RuntimeException> {
            flow<Int> { throw RuntimeException("BAM!") }
                .scanWith({ 0 }) { acc, item -> acc + item }
                .collect()
        }
    }

    @Test
    fun `when failure in operation`() = runTest {
        assertFailsWith<RuntimeException> {
            flowOf(1, 2, 3)
                .scanWith({ 0 }) { _, _ -> throw RuntimeException("BAM!") }
                .collect()
        }
    }

    @Test
    fun `when take only initial value`() = runTest {
        var initial = 0
        // 하나만 받고 끝내므로, initial 값만 emit 하고 중단합니다.
        val flow = flow<Int> { fail("Should not be called") }
            .scanWith({ initial++ }) { acc, item -> acc + item }
            .take(1)

        repeat(10) {
            flow.last() shouldBeEqualTo it
        }
    }
}
