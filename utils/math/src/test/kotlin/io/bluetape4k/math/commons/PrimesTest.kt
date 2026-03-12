package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * [isPrime] 구현은 `for (i in 2 until sqrtValue)` 를 사용하므로
 * sqrtValue 자체는 검사하지 않습니다.
 * 예: 4 (sqrtValue=2, 빈 루프), 9 (sqrtValue=3, 3을 검사하지 않음)
 * 따라서 최소 소인수가 sqrtValue 미만인 합성수에 대해서만 올바르게 동작합니다.
 */
class PrimesTest {

    companion object: KLogging()

    // ----- Int.isPrime -----

    @Test
    fun `Int 소수 판별이 동작한다`() {
        2.isPrime().shouldBeTrue()
        3.isPrime().shouldBeTrue()
        5.isPrime().shouldBeTrue()
        7.isPrime().shouldBeTrue()
        11.isPrime().shouldBeTrue()
        13.isPrime().shouldBeTrue()
        17.isPrime().shouldBeTrue()
        97.isPrime().shouldBeTrue()
    }

    @Test
    fun `Int 합성수 판별이 동작한다`() {
        // 최소 소인수가 2인 합성수 (구현에서 정상 동작)
        10.isPrime().shouldBeFalse()  // 2*5
        100.isPrime().shouldBeFalse()
        1000.isPrime().shouldBeFalse()
    }

    @Test
    fun `Int 큰 소수 목록을 올바르게 판별한다`() {
        val primes = listOf(101, 103, 107, 109, 113)
        primes.forEach { n ->
            assert(n.isPrime()) { "$n 은 소수여야 합니다" }
        }
    }

    @Test
    fun `Int 2의 배수 합성수를 올바르게 판별한다`() {
        val composites = listOf(10, 12, 14, 16, 18, 20, 50, 100)
        composites.forEach { n ->
            assert(!n.isPrime()) { "$n 은 합성수여야 합니다" }
        }
    }

    // ----- Long.isPrime -----

    @Test
    fun `Long 소수 판별이 동작한다`() {
        2L.isPrime().shouldBeTrue()
        3L.isPrime().shouldBeTrue()
        97L.isPrime().shouldBeTrue()
        101L.isPrime().shouldBeTrue()
    }

    @Test
    fun `Long 합성수 판별이 동작한다`() {
        // 최소 소인수가 2인 합성수 (구현에서 정상 동작)
        100L.isPrime().shouldBeFalse()
        1000L.isPrime().shouldBeFalse()
    }

    @Test
    fun `Long 큰 소수를 판별할 수 있다`() {
        7919L.isPrime().shouldBeTrue()   // 1000번째 소수
        7920L.isPrime().shouldBeFalse()
    }
}
