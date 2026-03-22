package io.bluetape4k.collections

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.NoSuchElementException

class BoundedStackTest {

    companion object: KLogging()

    @Test
    fun `maxSize가 0 이하이면 예외 발생`() {
        assertThrows<IllegalArgumentException> { BoundedStack<String>(0) }
        assertThrows<IllegalArgumentException> { BoundedStack<String>(-1) }
    }

    @Test
    fun `빈 스택 상태 확인`() {
        val stack = BoundedStack<String>(4)

        stack.size shouldBeEqualTo 0
        stack.isEmpty.shouldBeTrue()
    }

    @Test
    fun `빈 스택에서 get 시 IndexOutOfBoundsException 발생`() {
        val stack = BoundedStack<String>(4)
        assertThrows<IndexOutOfBoundsException> { stack[0] }
    }

    @Test
    fun `빈 스택에서 pop 시 NoSuchElementException 발생`() {
        val stack = BoundedStack<String>(4)
        assertThrows<NoSuchElementException> { stack.pop() }
    }

    @Test
    fun `빈 스택에서 peek 시 NoSuchElementException 발생`() {
        val stack = BoundedStack<String>(4)
        assertThrows<NoSuchElementException> { stack.peek() }
    }

    @Test
    fun `단일 요소 push 및 peek, pop`() {
        val stack = BoundedStack<String>(4)

        stack.push("a")
        stack.size shouldBeEqualTo 1
        stack.isEmpty.shouldBeFalse()
        stack.peek() shouldBeEqualTo "a"
        stack[0] shouldBeEqualTo "a"
        stack.toList() shouldBeEqualTo listOf("a")

        stack.pop() shouldBeEqualTo "a"
        stack.size shouldBeEqualTo 0
        stack.isEmpty.shouldBeTrue()
    }

    @Test
    fun `여러 요소 push 후 LIFO 순서 확인`() {
        val stack = BoundedStack<String>(4)

        stack.pushAll(listOf("a", "b", "c"))
        stack.size shouldBeEqualTo 3
        stack[0] shouldBeEqualTo "c"
        stack[1] shouldBeEqualTo "b"
        stack[2] shouldBeEqualTo "a"
        stack.toList() shouldBeEqualTo listOf("c", "b", "a")

        stack.pop() shouldBeEqualTo "c"
        stack.size shouldBeEqualTo 2
        stack.pop() shouldBeEqualTo "b"
        stack.size shouldBeEqualTo 1
        stack.pop() shouldBeEqualTo "a"
        stack.size shouldBeEqualTo 0
    }

    @Test
    fun `maxSize 초과 시 가장 오래된 요소 제거`() {
        val stack = BoundedStack<String>(4)

        stack.pushAll(listOf("a", "b", "c", "d", "e", "f"))
        stack.size shouldBeEqualTo 4
        stack[0] shouldBeEqualTo "f"
        stack.toList() shouldBeEqualTo listOf("f", "e", "d", "c")
    }

    @Test
    fun `update로 특정 인덱스 요소 교체`() {
        val stack = BoundedStack<String>(4)

        stack.pushAll(listOf("a", "b", "c", "d", "e", "f"))
        for (i in 0 until stack.size) {
            val old = stack[i]
            val updated = old + "2"
            stack.update(i, updated)
            stack[i] shouldBeEqualTo updated
        }
        stack.toList() shouldBeEqualTo listOf("f2", "e2", "d2", "c2")
    }

    @Test
    fun `insert at index 0 은 push와 동일`() {
        val stack = BoundedStack<String>(3)

        stack.insert(0, "a")
        stack.size shouldBeEqualTo 1
        stack[0] shouldBeEqualTo "a"

        stack.insert(0, "b")
        stack.size shouldBeEqualTo 2
        stack[0] shouldBeEqualTo "b"
        stack[1] shouldBeEqualTo "a"

        stack.insert(0, "c")
        stack.size shouldBeEqualTo 3
        stack[0] shouldBeEqualTo "c"
        stack[1] shouldBeEqualTo "b"
        stack[2] shouldBeEqualTo "a"

        // maxSize 초과 시
        stack.insert(0, "d")
        stack.size shouldBeEqualTo 3
        stack[0] shouldBeEqualTo "d"
        stack[1] shouldBeEqualTo "c"
        stack[2] shouldBeEqualTo "b"
    }

    @Test
    fun `insert at count - 끝에 추가`() {
        val stack = BoundedStack<String>(3)

        stack.insert(0, "a")
        stack.insert(1, "b")
        stack.insert(2, "c")

        stack[0] shouldBeEqualTo "a"
        stack[1] shouldBeEqualTo "b"
        stack[2] shouldBeEqualTo "c"
        stack.toList() shouldBeEqualTo listOf("a", "b", "c")
    }

    @Test
    fun `clear로 모든 요소 제거`() {
        val stack = BoundedStack<String>(4)

        stack.pushAll(listOf("a", "b", "c"))
        stack.size shouldBeEqualTo 3

        stack.clear()
        stack.size shouldBeEqualTo 0
        stack.isEmpty.shouldBeTrue()
    }

    @Test
    fun `iterator를 통한 순회`() {
        val stack = BoundedStack<Int>(5)
        stack.pushAll(1, 2, 3)

        val collected = stack.toList()
        collected shouldBeEqualTo listOf(3, 2, 1)
    }

    @RepeatedTest(3)
    fun `동시 push 및 pop - thread safety 검증`() {
        val stack = BoundedStack<Int>(100)
        val threads = (1..10).map { threadId ->
            Thread {
                repeat(100) { i ->
                    stack.push(threadId * 1000 + i)
                    if (i % 2 == 0) runCatching { stack.pop() }
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        // 예외 없이 완료되면 성공
    }
}
