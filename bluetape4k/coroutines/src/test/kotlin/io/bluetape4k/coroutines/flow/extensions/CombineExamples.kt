package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CombineExamples: AbstractFlowTest() {

    companion object: KLogging()

    @Test
    fun `flow들의 요소를 combine 한다`() = runTest {
        // 각 flow 들의 결과를 조합하여 새로운 결과를 만든다.
        combine(
            flow1(),
            flow2(),
            flow3(),
            flow4(),
        ) { f1, f2, f3, f4 ->
            listOf(f1, f2, f3, f4).joinToString("-")
        }.assertResult(
            "1-a-true-a",
            "2-b-false-b",
            "3-c-true-c",
        )
    }


    private fun flow1(): Flow<Int> = flowOf(1, 2, 3).log("#1")
    private fun flow2(): Flow<String> = flowOf("a", "b", "c").log("#2")
    private fun flow3(): Flow<Boolean> = flowOf(true, false, true).log("#3")
    private fun flow4(): Flow<Char> = flowOf('a', 'b', 'c').log("#4")

    @Test
    fun `요소 수가 다른 flow를 combine하면, 마지막 요소를 combine에 사용한다`() = runTest {
        val flow1 = flowOf(1, 2, 3, 4)
        val flow2 = flowOf("a", "b")

        combine(flow1, flow2) { f1, f2 ->
            "$f1-$f2"
        }.assertResult(
            "1-a",
            "2-b",
            "3-b",
            "4-b"
        )
    }
}
