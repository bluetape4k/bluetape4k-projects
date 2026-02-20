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
}
