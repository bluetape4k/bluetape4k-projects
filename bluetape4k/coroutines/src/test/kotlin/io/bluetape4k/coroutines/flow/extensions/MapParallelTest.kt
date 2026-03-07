package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MapParallelTest {

    @Test
    fun `parallelism 1 maintains order`() = runTest {
        val result = (1..5).asFlow()
            .mapParallel(parallelism = 1) { it * 2 }
            .toList()

        result shouldBeEqualTo listOf(2, 4, 6, 8, 10)
    }

    @Test
    fun `parallelism 0 coerces to sequential mapping`() = runTest {
        val result = (1..5).asFlow()
            .mapParallel(parallelism = 0) { it * 3 }
            .toList()

        result shouldBeEqualTo listOf(3, 6, 9, 12, 15)
    }

    @Test
    fun `negative parallelism coerces to sequential mapping`() = runTest {
        val result = (1..4).asFlow()
            .mapParallel(parallelism = -10) { it + 1 }
            .toList()

        result shouldBeEqualTo listOf(2, 3, 4, 5)
    }
}
