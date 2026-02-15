package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.coroutines.tests.withParallels
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ParallelFlowValidationTest {

    @Test
    fun `parallel rejects non-positive parallelism`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            flowOf(1).parallel(0) { error("unreachable") }
        }
        assertFailsWith<IllegalArgumentException> {
            flowOf(1).parallel(-1) { error("unreachable") }
        }
    }

    @Test
    fun `map collect validates collector count`() = runTest {
        withParallels(1) { execs ->
            val parallel = flowOf(1)
                .parallel(1) { execs[it] }
                .map { it + 1 }

            assertFailsWith<IllegalArgumentException> {
                parallel.collect()
            }
        }
    }

    @Test
    fun `filter collect validates collector count`() = runTest {
        withParallels(1) { execs ->
            val parallel = flowOf(1)
                .parallel(1) { execs[it] }
                .filter { it > 0 }

            assertFailsWith<IllegalArgumentException> {
                parallel.collect()
            }
        }
    }

    @Test
    fun `transform collect validates collector count`() = runTest {
        withParallels(1) { execs ->
            val parallel = flowOf(1)
                .parallel(1) { execs[it] }
                .transform { emit(it + 1) }

            assertFailsWith<IllegalArgumentException> {
                parallel.collect()
            }
        }
    }

    @Test
    fun `reduce collect validates collector count`() = runTest {
        withParallels(1) { execs ->
            val parallel = flowOf(1)
                .parallel(1) { execs[it] }
                .reduce({ 0 }) { acc, item -> acc + item }

            assertFailsWith<IllegalArgumentException> {
                parallel.collect()
            }
        }
    }

    @Test
    fun `parallel collect validates collector count`() = runTest {
        withParallels(2) { execs ->
            val parallel = flowOf(1, 2).parallel(2) { execs[it] }
            val collector = FlowCollector<Int> { }

            assertFailsWith<IllegalArgumentException> {
                parallel.collect(collector)
            }
        }
    }
}
