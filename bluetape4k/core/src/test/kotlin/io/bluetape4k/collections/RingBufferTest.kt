package io.bluetape4k.collections

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RingBufferTest {

    companion object: KLogging()

    @Test
    fun `maxSize가 0 이하이면 예외 발생`() {
        assertThrows<IllegalArgumentException> { RingBuffer<String>(0) }
        assertThrows<IllegalArgumentException> { RingBuffer<String>(-1) }
    }

    @Test
    fun `빈 버퍼 상태 확인`() {
        val ring = RingBuffer<String>(4)

        ring.size shouldBeEqualTo 0
        ring.isEmpty.shouldBeTrue()
        ring.iterator().hasNext().shouldBeFalse()
    }

    @Test
    fun `빈 버퍼에서 get 시 IndexOutOfBoundsException 발생`() {
        val ring = RingBuffer<String>(4)
        assertThrows<IndexOutOfBoundsException> { ring[0] }
    }

    @Test
    fun `빈 버퍼에서 next 시 NoSuchElementException 발생`() {
        val ring = RingBuffer<String>(4)
        assertThrows<NoSuchElementException> { ring.next() }
    }

    @Test
    fun `단일 요소 추가`() {
        val ring = RingBuffer<String>(4)

        ring.add("a")
        ring.isEmpty.shouldBeFalse()
        ring.size shouldBeEqualTo 1
        ring[0] shouldBeEqualTo "a"
        ring.toList() shouldBeEqualTo listOf("a")
    }

    @Test
    fun `여러 요소 추가 및 순서 확인`() {
        val ring = RingBuffer<String>(4)

        ring.addAll(listOf("a", "b", "c"))
        ring.size shouldBeEqualTo 3
        ring[0] shouldBeEqualTo "a"
        ring[1] shouldBeEqualTo "b"
        ring[2] shouldBeEqualTo "c"
        ring.toList() shouldBeEqualTo listOf("a", "b", "c")

        ring.next() shouldBeEqualTo "a"
        ring.size shouldBeEqualTo 2
        ring.next() shouldBeEqualTo "b"
        ring.size shouldBeEqualTo 1
        ring.next() shouldBeEqualTo "c"
        ring.size shouldBeEqualTo 0
    }

    @Test
    fun `capacity 초과 시 가장 오래된 요소 덮어쓰기`() {
        val ring = RingBuffer<String>(4)

        ring.addAll(listOf("a", "b", "c", "d", "t", "f"))
        ring.size shouldBeEqualTo 4
        ring[0] shouldBeEqualTo "c"
        ring[3] shouldBeEqualTo "f"
        ring.toList() shouldBeEqualTo listOf("c", "d", "t", "f")
    }

    @Test
    fun `removeIf - 조건부 제거`() {
        val ring = RingBuffer<Int>(6)

        ring.addAll(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        ring.toList() shouldBeEqualTo listOf(4, 5, 6, 7, 8, 9)

        ring.removeIf { it % 3 == 0 }
        ring.toList() shouldBeEqualTo listOf(4, 5, 7, 8)
    }

    @Test
    fun `drop - 앞에서 n개 제거`() {
        val ring = RingBuffer<Int>(5)

        ring.addAll(1, 2, 3, 4, 5)
        ring.drop(2)
        ring.size shouldBeEqualTo 3
        ring.toList() shouldBeEqualTo listOf(3, 4, 5)
    }

    @Test
    fun `drop - 전체 크기 이상 제거 시 clear`() {
        val ring = RingBuffer<Int>(5)

        ring.addAll(1, 2, 3)
        ring.drop(5)
        ring.size shouldBeEqualTo 0
        ring.isEmpty.shouldBeTrue()
    }

    @Test
    fun `clear로 모든 요소 제거`() {
        val ring = RingBuffer<String>(4)

        ring.addAll(listOf("a", "b", "c"))
        ring.size shouldBeEqualTo 3

        ring.clear()
        ring.size shouldBeEqualTo 0
        ring.isEmpty.shouldBeTrue()
    }

    @Test
    fun `set으로 특정 인덱스 요소 교체`() {
        val ring = RingBuffer<String>(4)

        ring.addAll(listOf("a", "b", "c"))
        ring[1] = "x"
        ring[1] shouldBeEqualTo "x"
        ring.toList() shouldBeEqualTo listOf("a", "x", "c")
    }

    @Test
    fun `toList로 전체 요소 복사`() {
        val ring = RingBuffer<Int>(5)

        ring.addAll(1, 2, 3, 4, 5)
        val list = ring.toList()
        list shouldBeEqualTo listOf(1, 2, 3, 4, 5)
    }

    @Test
    fun `toArray로 배열 변환`() {
        val ring = RingBuffer<Int>(5)

        ring.addAll(1, 2, 3)
        val arr = ring.toArray<Int>()
        arr.size shouldBeEqualTo 3
        arr[0] shouldBeEqualTo 1
        arr[1] shouldBeEqualTo 2
        arr[2] shouldBeEqualTo 3
    }

    @RepeatedTest(3)
    fun `동시 add 및 next - thread safety 검증`() {
        val ring = RingBuffer<Int>(100)
        val threads = (1..10).map { threadId ->
            Thread {
                repeat(100) { i ->
                    ring.add(threadId * 1000 + i)
                    if (i % 3 == 0) runCatching { ring.next() }
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        // 예외 없이 완료되면 성공
    }
}
