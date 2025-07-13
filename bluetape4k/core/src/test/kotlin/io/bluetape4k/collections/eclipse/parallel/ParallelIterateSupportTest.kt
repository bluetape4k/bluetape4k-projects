package io.bluetape4k.collections.eclipse.parallel

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.stream.toFastList
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.system.measureTimeMillis

class ParallelIterateSupportTest: AbstractCollectionTest() {

    companion object: KLogging() {
        private const val COUNT = DEFAULT_PARALLEL_BATCH_SIZE * 16
        private val intRange = 0 until COUNT
        private const val LIST_COUNT = 1000

        private val xs = fastList(COUNT) { it }
        private val xss = (0..8).chunked(3).toFastList()
    }

    @Test
    fun `병렬 방식으로 필터링하기`() {
        val even = xs.parFilter { it % 2 == 0 }
        val odd = xs.parFilter { it % 2 == 1 }

        even.size shouldBeEqualTo xs.size / 2
        odd.size shouldBeEqualTo xs.size / 2
    }

    @Test
    fun `병렬 방식으로 필터링하기 - filterNot`() {
        val even = xs.parFilterNot { it % 2 == 1 }
        val odd = xs.parFilterNot { it % 2 == 0 }

        even.size shouldBeEqualTo xs.size / 2
        odd.size shouldBeEqualTo xs.size / 2
    }

    @Test
    fun `병렬 방식으로 숫자 세기`() {
        val xs = fastList(LIST_COUNT) { it }

        val fastTime = measureTimeMillis {
            val count = xs.parCount(LIST_COUNT) { Thread.sleep(1); it % 2 == 0 }
            count shouldBeEqualTo xs.size / 2
        }

        val slowTime = measureTimeMillis {
            val count = xs.count { Thread.sleep(1); it % 2 == 0 }
            count shouldBeEqualTo xs.size / 2
        }

        log.debug { "Fast count took $fastTime ms, slow count took $slowTime ms" }
        fastTime shouldBeLessThan slowTime
    }

    @Test
    fun `병렬 방식으로 forEach 실행하기`() {
        val even = ConcurrentLinkedQueue<Int>()

        xs.parForEach(LIST_COUNT) {
            if (it % 2 == 0) {
                even.add(it)
            }
        }
        even.size shouldBeEqualTo xs.size / 2
    }

    @Test
    fun `병렬 방식으로 forEach 실행하기 with index`() {
        val even = ConcurrentLinkedQueue<Int>()

        xs.parForEachWithIndex(LIST_COUNT) { index, element ->
            if (element % 2 == 0) {
                even.add(element)
            }
        }
        even.size shouldBeEqualTo xs.size / 2
    }

    @Test
    fun `병렬 방식으로 map 하기`() {
        val times = xs.parMap(LIST_COUNT) { it * 2 }

        times.size shouldBeEqualTo xs.size
    }

    @Test
    fun `병렬 방식으로 flatMap 하기`() {
        val flatMapped = xs.parFlatMap(LIST_COUNT) { x ->
            fastList(3) { x.toLong() + it }
        }

        flatMapped.size shouldBeEqualTo xs.size * 3
    }

    @Test
    fun `병렬 방식으로 filter map 하기`() {
        val fm = xs.parFilterMap(LIST_COUNT, predicate = { it % 2 == 0 }) { it * 2 }

        fm.size shouldBeEqualTo xs.size / 2
    }

    @Test
    fun `병렬 방식으로 groupBy 하기`() {
        val grouped = xs.parGroupBy(LIST_COUNT) { it % 2 }

        grouped.keysView().size() shouldBeEqualTo 2
        grouped[0].size shouldBeEqualTo xs.size / 2
        grouped[1].size shouldBeEqualTo xs.size / 2
    }

    @Test
    fun `병렬 방식으로 aggregate 하기`() {
        val aggregated = xs.parAggregateBy(
            LIST_COUNT,
            groupBy = { 1 },
            zeroValueFactory = { 0L },
            nonMutatingAggregator = { acc: Long, item -> acc + item + 1L }
        )
        aggregated.keys.size shouldBeEqualTo 1
        aggregated[1] shouldBeEqualTo xs.sumOf { it + 1L }
    }

    @Test
    fun `병렬 방식으로 aggregate 하기 - 10개로 묶음`() {
        val aggregated = xs.parAggregateBy(
            LIST_COUNT,
            groupBy = { it % 10 },
            zeroValueFactory = { 0L },
            nonMutatingAggregator = { acc: Long, item -> acc + item + 1L }
        )
        aggregated.keys.size shouldBeEqualTo 10
        aggregated.forEach { (key, value) ->
            value shouldBeEqualTo xs.filter { it % 10 == key }.sumOf { it + 1L }
        }
    }

    @Test
    fun `java parallel stream 과 비교`() {
        val suffix = "value"
        val xs = fastList(COUNT) { "$suffix-$it" }
        val mapper: (String) -> Int = { it.drop(suffix.length + 1).toInt() }
        val batchSize = COUNT / (Runtimex.availableProcessors * 2)

        repeat(3) {
            xs.parallelStream().map(mapper)
            xs.parMap { mapper(it) }
        }

        val javaMap = measureTimeMillis {
            repeat(100) {
                xs.parallelStream().map(mapper).toFastList()
            }
        }
        log.info { "Java parallel stream took $javaMap ms" }

        val ecMap = measureTimeMillis {
            repeat(100) {
                xs.parMap(batchSize) { mapper(it) }.toFastList()
            }
        }
        log.info { "Eclipse Collections parallel map took $ecMap ms" }
    }
}
