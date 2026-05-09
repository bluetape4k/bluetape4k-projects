package io.bluetape4k.spring.mongodb.query

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.query.Update

/**
 * [UpdateExtensions]의 단위 테스트입니다.
 *
 * MongoDB 연결 없이 [Update] 객체의 구조를 비교하여 확장 함수의 정확성을 검증합니다.
 */
class UpdateExtensionsTest {
    companion object: KLoggingChannel()

    // ====================================================
    // updateOf
    // ====================================================

    @Test
    fun `updateOf - 여러 필드를 set으로 생성한다`() {
        val update = updateOf("name" to "Alice", "age" to 31)
        val expected = Update().set("name", "Alice").set("age", 31)

        update.updateObject shouldBeEqualTo expected.updateObject
    }

    @Test
    fun `updateOf - 단일 필드를 set으로 생성한다`() {
        val update = updateOf("city" to "Seoul")
        val expected = Update().set("city", "Seoul")

        update.updateObject shouldBeEqualTo expected.updateObject
    }

    @Test
    fun `updateOf - null 값도 set 할 수 있다`() {
        val update = updateOf("deletedAt" to null)
        val expected = Update().set("deletedAt", null)

        update.updateObject shouldBeEqualTo expected.updateObject
    }

    // ====================================================
    // setTo (infix)
    // ====================================================

    @Test
    fun `setTo - Update update와 동일한 결과를 반환한다`() {
        val actual = "name" setTo "Alice"
        val expected = Update.update("name", "Alice")

        actual.updateObject shouldBeEqualTo expected.updateObject
    }

    @Test
    fun `setTo - null 값도 설정한다`() {
        val actual = "field" setTo null
        val expected = Update.update("field", null)

        actual.updateObject shouldBeEqualTo expected.updateObject
    }

    // ====================================================
    // incBy (infix)
    // ====================================================

    @Test
    fun `incBy - inc 연산을 생성한다`() {
        val actual = "score" incBy 10
        val expected = Update().inc("score", 10)

        actual.updateObject shouldBeEqualTo expected.updateObject
    }

    @Test
    fun `incBy - 음수 inc도 동작한다`() {
        val actual = "balance" incBy -5
        val expected = Update().inc("balance", -5)

        actual.updateObject shouldBeEqualTo expected.updateObject
    }

    // ====================================================
    // unsetField
    // ====================================================

    @Test
    fun `unsetField - unset 연산을 생성한다`() {
        val actual = "tempField".unsetField()
        val expected = Update().unset("tempField")

        actual.updateObject shouldBeEqualTo expected.updateObject
    }

    // ====================================================
    // pushValue / pullValue (infix)
    // ====================================================

    @Test
    fun `pushValue - push 연산을 생성한다`() {
        val actual = "tags" pushValue "kotlin"
        val expected = Update().push("tags", "kotlin")

        actual.updateObject shouldBeEqualTo expected.updateObject
    }

    @Test
    fun `pullValue - pull 연산을 생성한다`() {
        val actual = "tags" pullValue "deprecated"
        val expected = Update().pull("tags", "deprecated")

        actual.updateObject shouldBeEqualTo expected.updateObject
    }

    // ====================================================
    // 체이닝 확장
    // ====================================================

    @Test
    fun `andSet - 추가 set 연산을 체이닝한다`() {
        val actual = ("name" setTo "Alice").andSet("age", 31)
        val expected = Update.update("name", "Alice").set("age", 31)

        actual.updateObject shouldBeEqualTo expected.updateObject
    }

    @Test
    fun `andInc - 추가 inc 연산을 체이닝한다`() {
        val actual = ("score" incBy 10).andInc("level", 1)

        actual.updateObject.shouldNotBeNull()
        @Suppress("UNCHECKED_CAST")
        val incOps = actual.updateObject["\$inc"] as Map<String, Any>
        incOps["score"] shouldBeEqualTo 10
        incOps["level"] shouldBeEqualTo 1
    }

    @Test
    fun `andUnset - 추가 unset 연산을 체이닝한다`() {
        val actual = ("name" setTo "Alice").andUnset("tempField")

        actual.updateObject.shouldNotBeNull()
        actual.updateObject.containsKey("\$set") shouldBeEqualTo true
        actual.updateObject.containsKey("\$unset") shouldBeEqualTo true
    }

    @Test
    fun `andPush - 추가 push 연산을 체이닝한다`() {
        val actual = ("score" incBy 10).andPush("history", 100)

        actual.updateObject.shouldNotBeNull()
        actual.updateObject.containsKey("\$inc") shouldBeEqualTo true
        actual.updateObject.containsKey("\$push") shouldBeEqualTo true
    }
}
