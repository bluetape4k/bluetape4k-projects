package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class CastTest: AbstractFlowTest() {

    companion object: KLogging()

    @Test
    fun `cast success`() = runTest {
        flowOf<Any?>(1, 2, 3).cast<Int>().toList() shouldBeEqualTo listOf(1, 2, 3)

        flowOf<Any?>(1, 2, 3)
            .cast<Int>()
            .test {
                awaitItem() shouldBeEqualTo 1
                awaitItem() shouldBeEqualTo 2
                awaitItem() shouldBeEqualTo 3
                awaitComplete()
            }
    }

    @Test
    fun `cast raise ClassCastException`() = runTest {
        assertFailsWith<ClassCastException> {
            flowOf(1, 2, 3).cast<String>().collect()
        }
    }

    @Test
    fun `castNotNull success`() = runTest {
        val flow = flowOf(1, 2, null, 3)
        flow.castNotNull<Int>().toList() shouldBeEqualTo listOf(1, 2, 3)
    }

    @Test
    fun `castNullable success`() = runTest {
        val flow = flowOf(1, 2, 3)
        flow.castNullable() shouldBeEqualTo flow
    }
}
