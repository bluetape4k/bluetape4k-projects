package io.bluetape4k.support

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class ResultSupportTest {

    private val mixed: List<Result<Int>> = listOf(
        Result.success(1),
        Result.failure(RuntimeException("fail")),
        Result.success(3)
    )
    private val allSuccesses: List<Result<Int>> = listOf(Result.success(10), Result.success(20))
    private val allFailures: List<Result<Int>> = listOf(
        Result.failure(RuntimeException("e1")),
        Result.failure(IllegalStateException("e2"))
    )

    @Test
    fun `allSuccess 모두 성공이면 true 아니면 false`() {
        allSuccesses.allSuccess.shouldBeTrue()
        mixed.allSuccess.shouldBeFalse()
        allFailures.allSuccess.shouldBeFalse()
    }

    @Test
    fun `allFailure 모두 실패이면 true 아니면 false`() {
        allFailures.allFailure.shouldBeTrue()
        mixed.allFailure.shouldBeFalse()
        allSuccesses.allFailure.shouldBeFalse()
    }

    @Test
    fun `hasFailure 하나라도 실패이면 true`() {
        mixed.hasFailure.shouldBeTrue()
        allFailures.hasFailure.shouldBeTrue()
        allSuccesses.hasFailure.shouldBeFalse()
    }

    @Test
    fun `hasSuccess 하나라도 성공이면 true`() {
        mixed.hasSuccess.shouldBeTrue()
        allSuccesses.hasSuccess.shouldBeTrue()
        allFailures.hasSuccess.shouldBeFalse()
    }

    @Test
    fun `successes 성공 결과 값 리스트를 반환한다`() {
        mixed.successes.sorted() shouldBeEqualTo listOf(1, 3)
        allSuccesses.successes.sorted() shouldBeEqualTo listOf(10, 20)
        allFailures.successes shouldBeEqualTo emptyList()
    }

    @Test
    fun `failures 실패 예외 리스트를 반환한다`() {
        mixed.failures.size shouldBeEqualTo 1
        allFailures.failures.size shouldBeEqualTo 2
        allSuccesses.failures shouldBeEqualTo emptyList()
    }

    @Test
    fun `nullable T 에서 null 성공 결과도 successes 에 포함된다`() {
        val results: List<Result<Int?>> = listOf(
            Result.success(1),
            Result.success(null),
            Result.success(3)
        )
        results.allSuccess.shouldBeTrue()
        results.successes.size shouldBeEqualTo 3
        results.successes.filterNotNull().sorted() shouldBeEqualTo listOf(1, 3)
    }

    @Test
    fun `Set Result T 에도 동일하게 적용된다`() {
        val setResults: Set<Result<Int>> = setOf(Result.success(1), Result.failure(RuntimeException("e")))
        setResults.hasFailure.shouldBeTrue()
        setResults.hasSuccess.shouldBeTrue()
        setResults.allSuccess.shouldBeFalse()
        setResults.successes shouldBeEqualTo listOf(1)
        setResults.failures.size shouldBeEqualTo 1
    }

    @Test
    fun `빈 컬렉션은 allSuccess 와 allFailure 가 모두 true`() {
        // vacuous truth: empty collection — all {} 는 true
        val empty: List<Result<Int>> = emptyList()
        empty.allSuccess.shouldBeTrue()
        empty.allFailure.shouldBeTrue()
        empty.hasSuccess.shouldBeFalse()
        empty.hasFailure.shouldBeFalse()
    }
}
