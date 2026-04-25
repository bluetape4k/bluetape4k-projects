package io.bluetape4k.batch.api

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * [SkipPolicy] 엣지 케이스 및 경계값 테스트.
 *
 * 특이한 예외 타입, 극단값(Long.MAX_VALUE), 부동소수점 경계 등을 검증.
 */
class SkipPolicyEdgeCaseTest {

    companion object : KLogging()

    // ─── maxSkips(Long.MAX_VALUE) ──────────────────────────────────────────────

    @Test
    fun `maxSkips(Long MAX_VALUE) 는 Long MAX_VALUE 미만 모든 skipCount 에서 true`() {
        val policy = SkipPolicy.maxSkips(Long.MAX_VALUE)
        policy.shouldSkip(RuntimeException("any"), 0L).shouldBeTrue()
        policy.shouldSkip(RuntimeException("any"), Long.MAX_VALUE - 1).shouldBeTrue()
    }

    @Test
    fun `maxSkips(Long MAX_VALUE) 는 skipCount = Long MAX_VALUE 에서 false`() {
        val policy = SkipPolicy.maxSkips(Long.MAX_VALUE)
        policy.shouldSkip(RuntimeException("any"), Long.MAX_VALUE).shouldBeFalse()
    }

    // ─── 다양한 Throwable 타입 ───────────────────────────────────────────────

    @Test
    fun `NONE 은 Throwable 의 모든 서브타입에서 false`() {
        SkipPolicy.NONE.shouldSkip(RuntimeException("runtime"), 0L).shouldBeFalse()
        SkipPolicy.NONE.shouldSkip(IllegalArgumentException("argument"), 0L).shouldBeFalse()
        SkipPolicy.NONE.shouldSkip(OutOfMemoryError("memory"), 0L).shouldBeFalse()
        SkipPolicy.NONE.shouldSkip(IllegalStateException("state"), 100L).shouldBeFalse()
    }

    @Test
    fun `ALL 은 Throwable 의 모든 서브타입에서 true`() {
        SkipPolicy.ALL.shouldSkip(RuntimeException("runtime"), 0L).shouldBeTrue()
        SkipPolicy.ALL.shouldSkip(IllegalArgumentException("argument"), 99L).shouldBeTrue()
        SkipPolicy.ALL.shouldSkip(OutOfMemoryError("memory"), Long.MAX_VALUE).shouldBeTrue()
        SkipPolicy.ALL.shouldSkip(IllegalStateException("state"), 1L).shouldBeTrue()
    }

    // ─── null 메시지를 가진 예외 ────────────────────────────────────────────

    @Test
    fun `예외의 message 가 null 이어도 정책은 정상 작동`() {
        val exceptionWithoutMessage = RuntimeException(null as String?)
        val policy = SkipPolicy.maxSkips(5L)
        policy.shouldSkip(exceptionWithoutMessage, 3L).shouldBeTrue()
        policy.shouldSkip(exceptionWithoutMessage, 5L).shouldBeFalse()
    }

    // ─── skipCount = 0 인 경계 ─────────────────────────────────────────────

    @Test
    fun `maxSkips(1) 은 skipCount = 0 일 때만 true`() {
        val policy = SkipPolicy.maxSkips(1L)
        policy.shouldSkip(RuntimeException("any"), 0L).shouldBeTrue()
        policy.shouldSkip(RuntimeException("any"), 1L).shouldBeFalse()
    }

    // ─── 커스텀 정책의 예외 무시 ────────────────────────────────────────────

    @Test
    fun `커스텀 정책이 exception 무시하고 skipCount 만 사용`() {
        val policy = SkipPolicy { _, skipCount -> skipCount < 10L }
        val result0 = policy.shouldSkip(RuntimeException("ex1"), 5L)
        val result1 = policy.shouldSkip(IllegalArgumentException("ex2"), 5L)
        result0.shouldBeTrue()
        result1.shouldBeTrue()
    }

    @Test
    fun `커스텀 정책이 skipCount 무시하고 exception 타입 만 확인`() {
        val policy = SkipPolicy { e, _ ->
            e !is IllegalArgumentException
        }
        policy.shouldSkip(IllegalArgumentException("specific"), 0L).shouldBeFalse()
        policy.shouldSkip(IllegalArgumentException("specific"), 999L).shouldBeFalse()
        policy.shouldSkip(RuntimeException("other"), 0L).shouldBeTrue()
        policy.shouldSkip(RuntimeException("other"), 999L).shouldBeTrue()
    }

    // ─── Nested Exception 처리 ──────────────────────────────────────────────

    @Test
    fun `cause 가 있는 중첩 예외도 정책 검증`() {
        val nested = IllegalArgumentException("nested cause")
        val wrapped = RuntimeException("wrapped", nested)
        val policy = SkipPolicy.maxSkips(3L)
        policy.shouldSkip(wrapped, 2L).shouldBeTrue()
        policy.shouldSkip(wrapped, 3L).shouldBeFalse()
    }

    // ─── 극단값 조합 ───────────────────────────────────────────────────────

    @Test
    fun `maxSkips(1) 과 maxSkips(Long MAX_VALUE) 의 보수 관계`() {
        val restrictive = SkipPolicy.maxSkips(1L)
        val permissive = SkipPolicy.maxSkips(Long.MAX_VALUE)

        restrictive.shouldSkip(RuntimeException("any"), 0L).shouldBeTrue()
        permissive.shouldSkip(RuntimeException("any"), 0L).shouldBeTrue()

        restrictive.shouldSkip(RuntimeException("any"), 1L).shouldBeFalse()
        permissive.shouldSkip(RuntimeException("any"), 1L).shouldBeTrue()

        restrictive.shouldSkip(RuntimeException("any"), Long.MAX_VALUE).shouldBeFalse()
        permissive.shouldSkip(RuntimeException("any"), Long.MAX_VALUE).shouldBeFalse()
    }
}
