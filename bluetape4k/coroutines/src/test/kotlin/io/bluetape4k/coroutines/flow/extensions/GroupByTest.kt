package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.coroutines.flow.exceptions.FlowOperationException
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.coroutines.tests.assertResultSet
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldHaveSize
import org.eclipse.collections.api.multimap.list.ListMultimap
import org.junit.jupiter.api.Test

class GroupByTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `group by with key`() = runTest {
        flowRangeOf(1, 10).log("src")
            .groupBy { it % 2 }
            .flatMapMerge { group -> group.toValues() }.log("flatMapMerge")
            .assertResultSet(listOf(1, 3, 5, 7, 9), listOf(2, 4, 6, 8, 10))
    }

    @Test
    fun `group by with key and values`() = runTest {
        flowRangeOf(1, 10)
            .groupBy { it % 2 }
            .flatMapMerge { group -> group.toGroupItems() }
            .onEach { log.debug { "grouped item=$it" } }
            .assertResultSet(
                GroupItem(1, listOf(1, 3, 5, 7, 9)),
                GroupItem(0, listOf(2, 4, 6, 8, 10)),
            )
    }

    @Test
    fun `group by with value selector`() = runTest {
        flowRangeOf(1, 10).log("source")
            .groupBy({ it % 2 }) { it + 1 }
            .flatMapMerge { it.toValues() }.log("flatMapMerge")
            .assertResultSet(listOf(2, 4, 6, 8, 10), listOf(3, 5, 7, 9, 11))
    }

    @Test
    fun `one of each`() = runTest {
        flowRangeOf(1, 10).log("source")
            .groupBy { it % 2 }
            .flatMapMerge {
                it.take(1).onEach { log.trace { "grouped item=$it" } }
            }.log("flatMapMerge")
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }

    @Test
    fun `max groups`() = runTest {
        flowRangeOf(1, 10)
            .groupBy { it % 3 }
            .flatMapMerge { it.toValues() }
            .assertResultSet(listOf(1, 4, 7, 10), listOf(2, 5, 8), listOf(3, 6, 9))

        flowRangeOf(1, 10)
            .groupBy { it % 3 }
            .take(2)                    // list(3, 6, 9) 는 빠진다
            .flatMapMerge { it.toValues() }
            .assertResultSet(listOf(1, 4, 7, 10), listOf(2, 5, 8))
    }

    @Test
    fun `take items`() = runTest {
        flowRangeOf(1, 10)
            .groupBy { it % 2 }
            .flatMapMerge { it }
            .take(3)
            .onEach { log.trace { "item=$it" } }
            .assertResult(1, 2, 3)
    }

    @Test
    fun `take groups and items`() = runTest {
        flowRangeOf(1, 10)
            .groupBy { it % 3 }
            .take(2)
            .flatMapMerge { it }
            .take(2)
            .assertResult(1, 2)
    }

    @Test
    fun `main errors no items`() = runTest {
        flowRangeOf(1, 10)
            .map { if (it < 5) error("oops") else it }
            .groupBy { it % 2 == 0 }
            .flatMapMerge { it }
            .onEach { log.trace { it } }
            .test {
                awaitError() shouldBeInstanceOf FlowOperationException::class
            }
    }

    @Test
    fun `main errors some items`() = runTest {
        flowRangeOf(1, 10).log("source")
            .map { if (it > 5) error("oops") else it }
            .groupBy { it % 2 == 0 }
            .flatMapMerge { it }.log("flatMapMerge")
            .test {
                awaitItem() shouldBeEqualTo 1
                awaitItem() shouldBeEqualTo 2
                awaitItem() shouldBeEqualTo 3
                awaitError() shouldBeInstanceOf FlowOperationException::class
            }
    }

    @Test
    fun `convert Group to Map`() = runTest {
        val map = flowRangeOf(1, 10)
            .groupBy { it % 2 }
            .toMap()
        map shouldBeEqualTo mapOf(0 to listOf(2, 4, 6, 8, 10), 1 to listOf(1, 3, 5, 7, 9))
    }

    @Test
    fun `convert Group to UnifiedMap`() = runTest {
        val mmap: MutableMap<Int, List<Int>> = flowRangeOf(1, 10)
            .groupBy { it % 2 }
            .toUnifiedMap()

        mmap.size shouldBeEqualTo 2
        mmap.keys shouldHaveSize 2
        mmap[0] shouldBeEqualTo listOf(2, 4, 6, 8, 10)
        mmap[1] shouldBeEqualTo listOf(1, 3, 5, 7, 9)
    }

    @Test
    fun `convert Group to ListMultimap`() = runTest {
        val mmap: ListMultimap<Int, Int> = flowRangeOf(1, 10)
            .groupBy { it % 2 }
            .toListMultiMap()

        mmap.keysView().size() shouldBeEqualTo 2
        mmap.keysView() shouldHaveSize 2
        mmap[0] shouldBeEqualTo listOf(2, 4, 6, 8, 10)
        mmap[1] shouldBeEqualTo listOf(1, 3, 5, 7, 9)
    }
}
