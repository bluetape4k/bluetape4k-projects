package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.primitives.doubleArrayListOf
import io.bluetape4k.collections.eclipse.primitives.floatArrayListOf
import io.bluetape4k.collections.eclipse.primitives.intArrayListOf
import io.bluetape4k.collections.eclipse.primitives.longArrayListOf
import io.bluetape4k.collections.eclipse.unifiedSetOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class EclipseCollectionsFlowTest {

    @Test
    fun `toFastList와 toUnifiedSet은 flow 요소를 수집한다`() = runTest {
        flowOf(1, 2, 2, 3).toFastList() shouldBeEqualTo fastListOf(1, 2, 2, 3)
        flowOf(1, 2, 2, 3).toUnifiedSet() shouldBeEqualTo unifiedSetOf(1, 2, 3)
    }

    @Test
    fun `toUnifiedMap은 selector 결과로 map을 구성한다`() = runTest {
        val result = flowOf("a", "bb", "ccc")
            .toUnifiedMap(
                keySelector = { it.length },
                valueSelector = { it.uppercase() },
            )

        result.size shouldBeEqualTo 3
        result[1] shouldBeEqualTo "A"
        result[2] shouldBeEqualTo "BB"
        result[3] shouldBeEqualTo "CCC"
    }

    @Test
    fun `toIntArrayList는 primitive list로 수집한다`() = runTest {
        flowOf(1, 2, 3).toIntArrayList() shouldBeEqualTo intArrayListOf(1, 2, 3)
    }

    @Test
    fun `toLongArrayList는 primitive list로 수집한다`() = runTest {
        flowOf(1L, 2L, 3L).toLongArrayList() shouldBeEqualTo longArrayListOf(1, 2, 3)
    }

    @Test
    fun `toFloatArrayList는 primitive list로 수집한다`() = runTest {
        flowOf(1f, 2f, 3f).toFloatArrayList() shouldBeEqualTo floatArrayListOf(1f, 2f, 3f)
    }

    @Test
    fun `toDoubleArrayList는 primitive list로 수집한다`() = runTest {
        flowOf(1.0, 2.0, 3.0).toDoubleArrayList() shouldBeEqualTo doubleArrayListOf(1.0, 2.0, 3.0)
    }
}
