package io.bluetape4k.batch.api

import org.amshove.kluent.shouldBe
import org.junit.jupiter.api.Test

/**
 * [SkipPolicy] 단위 테스트.
 *
 * `NONE`, `ALL`, `maxSkips(n)`, 커스텀 람다 시나리오 검증.
 */
class SkipPolicyTest {

    private val anyException = RuntimeException("테스트 예외")

    // ─── NONE ────────────────────────────────────────────────────────────────

    @Test
    fun `NONE - 모든 예외에 대해 false 반환`() {
        SkipPolicy.NONE.shouldSkip(anyException, 0L) shouldBe false
        SkipPolicy.NONE.shouldSkip(anyException, 100L) shouldBe false
        SkipPolicy.NONE.shouldSkip(IllegalArgumentException("x"), 0L) shouldBe false
    }

    // ─── ALL ─────────────────────────────────────────────────────────────────

    @Test
    fun `ALL - 모든 예외에 대해 true 반환`() {
        SkipPolicy.ALL.shouldSkip(anyException, 0L) shouldBe true
        SkipPolicy.ALL.shouldSkip(anyException, Long.MAX_VALUE) shouldBe true
        SkipPolicy.ALL.shouldSkip(Error("fatal"), 0L) shouldBe true
    }

    // ─── maxSkips(n) ─────────────────────────────────────────────────────────

    @Test
    fun `maxSkips - skipCount가 maxSkips 미만이면 true`() {
        val policy = SkipPolicy.maxSkips(5L)
        policy.shouldSkip(anyException, 0L) shouldBe true
        policy.shouldSkip(anyException, 4L) shouldBe true
    }

    @Test
    fun `maxSkips - skipCount가 maxSkips와 같으면 false`() {
        val policy = SkipPolicy.maxSkips(5L)
        policy.shouldSkip(anyException, 5L) shouldBe false
    }

    @Test
    fun `maxSkips - skipCount가 maxSkips 초과이면 false`() {
        val policy = SkipPolicy.maxSkips(3L)
        policy.shouldSkip(anyException, 10L) shouldBe false
    }

    @Test
    fun `maxSkips 0 - 모든 경우 false (스킵 금지)`() {
        val policy = SkipPolicy.maxSkips(0L)
        policy.shouldSkip(anyException, 0L) shouldBe false
    }

    // ─── 커스텀 람다 ──────────────────────────────────────────────────────────

    @Test
    fun `커스텀 fun interface - IllegalArgumentException만 skip 허용`() {
        val policy = SkipPolicy { e, _ -> e is IllegalArgumentException }
        policy.shouldSkip(IllegalArgumentException("ok"), 0L) shouldBe true
        policy.shouldSkip(RuntimeException("not ok"), 0L) shouldBe false
    }

    @Test
    fun `커스텀 fun interface - skipCount 기반 조건`() {
        val policy = SkipPolicy { _, skipCount -> skipCount < 3L }
        policy.shouldSkip(anyException, 0L) shouldBe true
        policy.shouldSkip(anyException, 2L) shouldBe true
        policy.shouldSkip(anyException, 3L) shouldBe false
    }
}
