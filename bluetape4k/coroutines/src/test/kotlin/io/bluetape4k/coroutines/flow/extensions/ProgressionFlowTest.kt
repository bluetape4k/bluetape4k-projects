package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ProgressionFlowTest {

    @Test
    fun `progression flow는 step이 양수여야 한다`() = runTest {
        assertFailsWith<IllegalArgumentException> { intFlowOf(1, 3, 0).toList() }
        assertFailsWith<IllegalArgumentException> { longFlowOf(1L, 3L, 0L).toList() }
        assertFailsWith<IllegalArgumentException> { floatFlowOf(1f, 3f, 0f).toList() }
        assertFailsWith<IllegalArgumentException> { doubleFlowOf(1.0, 3.0, -1.0).toList() }
        assertFailsWith<IllegalArgumentException> { charFlowOf('a', 'c', -1).toList() }
        assertFailsWith<IllegalArgumentException> { byteFlowOf(1.toByte(), 3.toByte(), 0.toByte()).toList() }
    }

    @Test
    fun `progression flow는 시작부터 끝까지 순서대로 발행한다`() = runTest {
        intFlowOf(1, 5, 2).toList() shouldBeEqualTo listOf(1, 3, 5)
        charFlowOf('a', 'e', 2).toList() shouldBeEqualTo listOf('a', 'c', 'e')
    }

    @Test
    fun `progression flow는 overflow 혹은 정밀도 한계에서 무한 루프 없이 종료한다`() = runTest {
        intFlowOf(Int.MAX_VALUE - 1, Int.MAX_VALUE, 2).toList() shouldBeEqualTo listOf(Int.MAX_VALUE - 1)
        longFlowOf(Long.MAX_VALUE - 1, Long.MAX_VALUE, 2).toList() shouldBeEqualTo listOf(Long.MAX_VALUE - 1)
        charFlowOf('\uFFFE', '\uFFFF', 2).toList() shouldBeEqualTo listOf('\uFFFE')
        byteFlowOf(126.toByte(), 127.toByte(), 2.toByte()).toList() shouldBeEqualTo listOf(126.toByte())
        floatFlowOf(Float.MAX_VALUE, Float.MAX_VALUE, 1.0f).toList() shouldBeEqualTo listOf(Float.MAX_VALUE)
        doubleFlowOf(Double.MAX_VALUE, Double.MAX_VALUE, 1.0).toList() shouldBeEqualTo listOf(Double.MAX_VALUE)
    }
}
