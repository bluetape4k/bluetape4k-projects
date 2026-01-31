package io.bluetape4k.examples.coroutines.flow

import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.examples.coroutines.isEven
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class FlowOperatorExamples {

    companion object: KLoggingChannel()

    @Test
    fun `map elements`() = runTest {
        var sum = 0
        flowOf(1, 2, 3).log("sum")
            .map { it * it }            // [1, 4, 9]
            .collect { sum += it }      // 14

        sum shouldBeEqualTo 1 + 4 + 9
    }

    @Test
    fun `filter elements`() = runTest {
        val evens = flowOf(1, 2, 3, 4).log("source")
            .filter { it.isEven() }     // [2, 4]
            .log("even")
            .toFastList()

        evens shouldBeEqualTo listOf(2, 4)
    }

    @Test
    fun `merge element of flows`() = runTest {
        val ints = flowOf(1, 2, 3)
        val doubles = flowOf(0.1, 0.2, 0.3)

        // merge는 복수 개의 flow의 요소들을 합쳐서 하나의 flow로 만든다
        val togather = merge(ints, doubles)
        togather.toFastList() shouldBeEqualTo listOf(1, 2, 3, 0.1, 0.2, 0.3)
    }

    @Test
    fun `merge elements of flows with time`() = runTest {
        val ints = flowOf(1, 2, 3).onEach { delay(100) }.log("int")
        val doubles = flowOf(0.1, 0.2, 0.3).log("double")

        // merge는 복수 개의 flow의 요소들을 합쳐서 하나의 flow로 만든다
        val togather = merge(ints, doubles)
        togather.toFastList() shouldBeEqualTo listOf(0.1, 0.2, 0.3, 1, 2, 3)
    }

    /**
     * zip 은 각 flow 요소들의 emit 된 순서대로 zip 을 수행합니다.
     */
    @Test
    fun `zip elements of flows`() = runTest {
        /**
         * zip 에서는 전송된 요소들의 순서에 맞게 짝을 지운다
         *
         * .... A .... B .... C
         * .............. 1 ............. 2 .......... 3 .......... 4
         */
        val flow1 = flowOf("A", "B", "C").onEach { delay(400) }.log("char")
        val flow2 = flowOf(1, 2, 3, 4).onEach { delay(1000) }.log("ints")

        val ziped = flow1
            .zip(flow2) { f1, f2 -> "${f1}_${f2}" }
            .log("zip")
            .toFastList()

        ziped shouldBeEqualTo listOf("A_1", "B_2", "C_3")
    }

    @Test
    fun `combine elements of flows`() = runTest {
        /**
         * combine 에서는 A 는 쌍이 없는 상태에서 B가 왔기 때문에 A는 버린다
         *
         * .... A .... B .... C
         * .............. 1 ............. 2 .......... 3 .......... 4
         */
        val flow1 = flowOf("A", "B", "C")
            .onEach { delay(400) }
            .log("chars")
        val flow2 = flowOf(1, 2, 3, 4)
            .onEach { delay(1000) }
            .log("ints")

        val combined = flow1
            .combine(flow2) { f1, f2 -> "${f1}_${f2}" }
            .log("combine")
            .toFastList()

        combined shouldBeEqualTo listOf("B_1", "C_1", "C_2", "C_3", "C_4")
    }

    @Test
    fun `fold - accumulate all values in flow`() = runTest {
        val list = flowOf(1, 2, 3, 4)
            .onEach { delay(10) }
            .log("nums")
        val res = list.fold(0) { acc, i ->
            (acc + i).also {
                log.debug { "acc=$it" }
            }
        }
        res shouldBeEqualTo 10
    }

    @Test
    fun `scan - sliding fold`() = runTest {
        val list = flowOf(1, 2, 3, 4).log("nums")
        val res = list
            .onEach { delay(10) }
            .scan(0) { acc, i ->
                (acc + i).also {
                    log.debug { "acc=$it" }
                }
            }
            .toFastList()
        res shouldBeEqualTo listOf(0, 1, 3, 6, 10)
    }

    @Test
    fun `flatMapConcat - concat two flows`() = runTest {
        fun flowFrom(elem: String) = flowOf(1, 2, 3)
            .onEach { delay(100) }
            .log("nums")
            .map { "${it}_${elem}" }
            .log("map")

        val result = flowOf("A", "B", "C").log("chars")
            .flatMapConcat { flowFrom(it) }
            .log("flatMapConcat")
            .toFastList()

        result shouldBeEqualTo listOf("1_A", "2_A", "3_A", "1_B", "2_B", "3_B", "1_C", "2_C", "3_C")
    }

    @Test
    fun `flatMapMerge - merge two flows`() = runTest {
        fun flowFrom(elem: String) = flowOf(1, 2, 3)
            .onEach { delay(10) }
            .map { "${it}_${elem}" }
            .log("map")

        val result = flowOf("A", "B", "C")
            .flatMapMerge(concurrency = 3) { flowFrom(it) }
            .log("flatMapMerge")
            .toFastList()

        result shouldBeEqualTo listOf("1_A", "1_B", "1_C", "2_A", "2_B", "2_C", "3_A", "3_B", "3_C")
    }

    @Test
    fun `flatMapMerge with concurrency - merge two flows`() = runTest {
        fun flowFrom(elem: String) = flowOf(1, 2, 3)
            .onEach {
                delay(10)
                println("delay 10, $it")
            }
            .map { "${it}_${elem}" }

        val result = flowOf("A", "B", "C")
            .flatMapMerge(concurrency = 2) { flowFrom(it) }
            .onEach { print("$it, ") }
            .toFastList()

        result shouldBeEqualTo listOf("1_A", "1_B", "2_A", "2_B", "3_A", "3_B", "1_C", "2_C", "3_C")
    }

    @Test
    fun `flatMapLatest - latest element with two flows`() = runTest {
        fun flowFrom(elem: String) = flowOf(1, 2, 3)
            .onEach { delay(10) }
            .log("nums")
            .map { "${it}_${elem}" }
            .log("map")

        /**
         * ABC
         * ......1.....2.....3
         */
        val result = flowOf("A", "B", "C").log("chars")
            .flatMapLatest { flowFrom(it) }         // 각 flow의 마지막 요소들만 선택
            .onEach { println(it) }
            .toFastList()

        result shouldBeEqualTo listOf("1_C", "2_C", "3_C")
    }

    @Test
    fun `flatMapLatest - latest element with two flows with different delay`() = runTest {
        fun flowFrom(elem: String) = flowOf(1, 2, 3)
            .onEach { delay(1000) }
            .log("nums")
            .map { "${it}_${elem}" }

        /**
         * A......B......C
         * .....1               // A
         * .....1.|             // B
         * .....1....2.....3    // C
         *
         */
        val result = flowOf("A", "B", "C")
            .onEach { delay(1200) }.log("chars")
            .flatMapLatest { flowFrom(it) }         // 각 flow의 마지막 요소들만 선택
            .log("flatMapLatest")
            .toFastList()

        result shouldBeEqualTo listOf("1_A", "1_B", "1_C", "2_C", "3_C")
    }

    @Test
    fun `reduce flow`() = runTest {
        val flow = flowOf(1, 2, 3, 4)
        flow.reduce { acc, i -> acc + i } shouldBeEqualTo 10
    }
}
