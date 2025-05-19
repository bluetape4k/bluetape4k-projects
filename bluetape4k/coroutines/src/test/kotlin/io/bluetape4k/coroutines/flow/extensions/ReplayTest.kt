package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

class ReplayTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `replay one consumer`() = runTest {
        flowRangeOf(1, 5)
            .replay { shared ->
                shared.filter { it % 2 == 0 }.log("even", log)
            }
            .assertResult(2, 4)
    }

    @Test
    fun `replay multiple consumers`() = runTest {
        flowRangeOf(1, 5)
            .replay { shared ->
                merge(
                    shared.filter { it % 2 == 1 }.log("odd", log),
                    shared.filter { it % 2 == 0 }.log("even", log)
                )
            }
            .assertResult(1, 3, 5, 2, 4)
    }

    @Test
    fun `replay size bound`() = runTest {
        flowRangeOf(1, 5)
            .replay(2) { shared ->
                shared
                    .log("filter")                          // 1,2,3,4,5
                    .filter { it % 2 == 0 }                       // 2, 4
                    .concatWith(shared.log("replay 2"))     // 4, 5 (replay : 마지막 2개)
            }
            .assertResult(2, 4, 4, 5)    // filters: 2, 4   || concatWith : 4, 5
    }

    @Test
    fun `replay time bound`() = runTest {
        val timeout = 1.minutes

        flowRangeOf(1, 5)
            .onEach { delay(100) }
            .replay(timeout) { shared ->
                shared
                    .log("filter")  // 1,2,3,4,5
                    .filter { it % 2 == 0 }              // 2, 4
                    .concatWith(shared.log("replay timeout[$timeout]"))  // 1,2,3,4,5 (replay : timeout 만)
            }
            .assertResult(2, 4, 1, 2, 3, 4, 5) // filters : 2, 4 || concatWith : 1,2,3,4,5
    }

    @Test
    fun `replay size and time bound`() = runTest {
        val timeout = 1.minutes

        flowRangeOf(1, 5)
            .replay(2, timeout) { shared ->
                shared
                    .log("filter")  // 1,2,3,4,5
                    .filter { it % 2 == 0 }              // 2, 4
                    .concatWith(shared.log("replay 2"))  // 4, 5 (replay : 마지막 2개)
            }
            .assertResult(2, 4, 4, 5)    // filters: 2, 4   || concatWith : 4, 5
    }

    @Test
    fun `replay size and time and custom time source bound`() = runTest {
        val timeout = 1.minutes
        val timeSource: (TimeUnit) -> Long = { System.currentTimeMillis() }

        flowRangeOf(1, 5)
            .replay(2, timeout, timeSource) { shared ->
                shared
                    .log("filter")
                    .filter { it % 2 == 0 }                  // 2, 4
                    .concatWith(shared.log("replay 2"))  // 4, 5 (replay : 마지막 2개)
            }
            .assertResult(2, 4, 4, 5)    // filters: 2, 4   || concatWith : 4, 5
    }

}
