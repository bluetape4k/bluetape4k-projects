package io.bluetape4k.collections.permutations

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import java.util.concurrent.atomic.AtomicInteger

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

        val results = mutableListOf<Int>()
        val threads = (1..10).map {
            Thread {
                synchronized(results) {
                    results.add(perm.tail.head)
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        results.all { it == 2 }.shouldBeEqualTo(true)
        results.size shouldBeEqualTo 10
    }
}
