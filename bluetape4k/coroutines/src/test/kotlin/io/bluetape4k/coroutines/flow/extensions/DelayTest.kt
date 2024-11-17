package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.KotlinLogging
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class DelayTest: AbstractFlowTest() {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Test
    fun `delayed flow`() = runTest {
        delayedFlow(1, 1.seconds)
            .assertResult(1)

        delayedFlow(2, 1_000L)
            .assertResult(2)
    }

    @Test
    fun `delayed flow with milliseconds`() = runTest {
        val emitted = atomic(false)

        launch {
            delayedFlow(1, 2_000)
                .collect {
                    it shouldBeEqualTo 1
                    emitted.compareAndSet(expect = false, update = true)
                }
        }

        runCurrent()
        emitted.value.shouldBeFalse()

        advanceTimeBy(1_000)
        emitted.value.shouldBeFalse()

        advanceTimeBy(1_100)
        emitted.value.shouldBeTrue()
    }

    @Test
    fun `delayed flow with kotlin duration`() = runTest {
        val emitted = atomic(false)

        launch {
            delayedFlow(1, 2.seconds)
                .collect {
                    it shouldBeEqualTo 1
                    emitted.compareAndSet(expect = false, update = true)
                }
        }

        runCurrent()
        emitted.value.shouldBeFalse()

        advanceTimeBy(1_000)
        emitted.value.shouldBeFalse()

        advanceTimeBy(1_100)
        emitted.value.shouldBeTrue()
    }
}
