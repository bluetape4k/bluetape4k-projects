package io.bluetape4k.collections.permutations

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Cons의 tail 동시 평가 thread-safety 검증 테스트
 */
class ConcurrentTest: AbstractPermutationTest() {

    @RepeatedTest(3)
    fun `동시 tail 평가는 thread-safe 해야 한다`() {
        val evaluated = AtomicInteger(0)
        val perm = cons(1) {
            evaluated.incrementAndGet()
            cons(2) { Nil.instance() }
        }

        val threads = (1..20).map {
            Thread { perm.tail }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        evaluated.get() shouldBeEqualTo 1 // tailFunc는 정확히 1번만 호출
    }

    @RepeatedTest(3)
    fun `동시 접근 후에도 올바른 값을 반환해야 한다`() {
        val perm = cons(1) {
            cons(2) {
                cons(3) {
                    emptyPermutation()
                }
            }
        }

        val lock = ReentrantLock()
        val results = mutableListOf<Int>()
        val threads = (1..10).map {
            Thread {
                lock.withLock {
                    results.add(perm.tail.head)
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        results.all { it == 2 }.shouldBeTrue()
        results.size shouldBeEqualTo 10
    }
}
