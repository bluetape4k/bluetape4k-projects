package io.bluetape4k.collections.eclipse.parallel

/**
 * Map 확장 함수의 병렬 처리 기능을 테스트한다.
 *
 * - JUnit 5, MockK, Kluent 사용
 */
import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue

class ParallelMapIterateSupportTest: AbstractCollectionTest() {

    companion object: KLogging() {
        private const val COUNT = 1000
        private val testMap = (0 until COUNT).associateWith { it * 2 }
    }

    @Test
    fun `병렬 forEach로 값 누적`() {
        val result = ConcurrentLinkedQueue<Int>()
        testMap.parForEach { k, v ->
            if (v % 4 == 0) result.add(v)
        }
        result.sumOf { it } shouldBeEqualTo List(COUNT) { it * 2 }.sumOf { if (it % 4 == 0) it else 0 }
    }

    @Test
    fun `병렬 map으로 값 변환`() {
        val mapped = testMap.parMap { k, v -> "$k-$v" }
        mapped.size shouldBeEqualTo COUNT
        mapped.first() shouldBeEqualTo "0-0"
    }

    @Test
    fun `병렬 flatMap으로 값 확장`() {
        val flatMapped = testMap.parFlatMap { k, v -> listOf(k, v) }
        flatMapped.size shouldBeEqualTo COUNT * 2
    }
}
