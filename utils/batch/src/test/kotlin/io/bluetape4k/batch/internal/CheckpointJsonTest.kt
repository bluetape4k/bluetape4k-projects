package io.bluetape4k.batch.internal

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [CheckpointJson] round-trip 및 classpath 오류 테스트.
 *
 * ## 검증 항목 (T30, T31)
 * T30 - round-trip:
 *   - Long → write → read → Long (정확한 타입 복원)
 *   - String, data class, Map 등 다양한 타입
 * T31 - jackson3 classpath:
 *   - tools.jackson이 classpath에 있으면 성공
 *   - 없으면 IllegalStateException (테스트 환경에서는 있으므로 성공 경로만 검증)
 */
class CheckpointJsonTest {

    private val sut: CheckpointJson = CheckpointJson.jackson3()

    // ─── T30: round-trip 테스트 ───────────────────────────────────────────

    @Test
    fun `Long round-trip - Integer 타입 혼선 없이 Long으로 복원`() {
        val original = 42L

        val json = sut.write(original)
        val restored = sut.read(json)

        restored shouldBeInstanceOf Long::class
        restored shouldBeEqualTo 42L
    }

    @Test
    fun `Int round-trip`() {
        val original = 123

        val json = sut.write(original)
        val restored = sut.read(json)

        restored shouldBeInstanceOf Int::class
        restored shouldBeEqualTo 123
    }

    @Test
    fun `String round-trip`() {
        val original = "hello checkpoint"

        val json = sut.write(original)
        val restored = sut.read(json)

        restored shouldBeInstanceOf String::class
        restored shouldBeEqualTo "hello checkpoint"
    }

    @Test
    fun `data class round-trip`() {
        data class KeyState(val lastId: Long, val pageIndex: Int)
        val original = KeyState(lastId = 999L, pageIndex = 5)

        val json = sut.write(original)
        val restored = sut.read(json)

        restored shouldBeInstanceOf KeyState::class
        val restoredState = restored as KeyState
        restoredState.lastId shouldBeEqualTo 999L
        restoredState.pageIndex shouldBeEqualTo 5
    }

    @Test
    fun `Double round-trip`() {
        val original = 3.14159

        val json = sut.write(original)
        val restored = sut.read(json)

        restored shouldBeInstanceOf Double::class
        (restored as Double) shouldBeEqualTo 3.14159
    }

    @Test
    fun `Boolean round-trip`() {
        val json = sut.write(true)
        val restored = sut.read(json)

        restored shouldBeInstanceOf Boolean::class
        restored shouldBe true
    }

    @Test
    fun `큰 Long 값 round-trip - Long MAX_VALUE`() {
        val original = Long.MAX_VALUE

        val json = sut.write(original)
        val restored = sut.read(json)

        restored shouldBeInstanceOf Long::class
        restored shouldBeEqualTo Long.MAX_VALUE
    }

    @Test
    fun `TypedCheckpoint 봉투 포함 검증 - className이 JSON에 있음`() {
        val original = 42L

        val json = sut.write(original)

        json.contains("className") shouldBe true
        json.contains("java.lang.Long") shouldBe true
        json.contains("payload") shouldBe true
    }

    // ─── T31: classpath 검증 ─────────────────────────────────────────────

    @Test
    fun `jackson3 팩토리 - tools_jackson이 있으면 성공`() {
        // 테스트 환경에는 bluetape4k-jackson3가 있으므로 성공해야 함
        val result = runCatching { CheckpointJson.jackson3() }

        result.isSuccess shouldBe true
        result.getOrNull().shouldNotBeNull()
    }

    @Test
    fun `jackson3 팩토리 - 반환 타입이 CheckpointJson`() {
        val json = CheckpointJson.jackson3()
        json shouldBeInstanceOf CheckpointJson::class
    }

    @Test
    fun `Jackson3CheckpointJson - write는 non-null JSON 문자열 반환`() {
        val json = sut.write(100L)
        json.isNotBlank() shouldBe true
    }

    @Test
    fun `Jackson3CheckpointJson - read는 write와 동일 값 반환`() {
        val values: List<Any> = listOf(1L, 2, "hello", true, 3.14)
        for (v in values) {
            val json = sut.write(v)
            val restored = sut.read(json)
            restored shouldBeEqualTo v
        }
    }
}
