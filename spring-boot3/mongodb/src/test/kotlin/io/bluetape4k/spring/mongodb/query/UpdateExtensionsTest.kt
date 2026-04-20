package io.bluetape4k.spring.mongodb.query

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.query.Update

/**
 * [UpdateExtensions]의 단위 테스트입니다.
 *
 * MongoDB 연결 없이 [Update] 객체의 구성을 검증합니다.
 */
class UpdateExtensionsTest {

    companion object: KLogging()

    // ====================================================
    // updateOf 팩토리
    // ====================================================

    @Test
    fun `updateOf - 여러 필드-값 쌍으로 Update를 생성한다`() {
        val update = updateOf("name" to "Alice", "age" to 31)
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        val setClause = updateObject["\$set"] as org.bson.Document
        setClause["name"] shouldBeEqualTo "Alice"
        setClause["age"] shouldBeEqualTo 31
    }

    @Test
    fun `updateOf - 단일 쌍도 올바르게 생성한다`() {
        val update = updateOf("city" to "Seoul")
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        val setClause = updateObject["\$set"] as org.bson.Document
        setClause["city"] shouldBeEqualTo "Seoul"
    }

    @Test
    fun `updateOf - null 값도 처리한다`() {
        val update = updateOf("deletedAt" to null)
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        val setClause = updateObject["\$set"] as org.bson.Document
        setClause.containsKey("deletedAt") shouldBeEqualTo true
    }

    // ====================================================
    // infix setTo
    // ====================================================

    @Test
    fun `setTo - 필드에 값을 설정하는 Update를 생성한다`() {
        val update = "name" setTo "Alice"
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        val setClause = updateObject["\$set"] as org.bson.Document
        setClause["name"] shouldBeEqualTo "Alice"
    }

    // ====================================================
    // infix incBy
    // ====================================================

    @Test
    fun `incBy - 필드 값을 증가시키는 Update를 생성한다`() {
        val update = "score" incBy 10
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        val incClause = updateObject["\$inc"] as org.bson.Document
        incClause["score"] shouldBeEqualTo 10
    }

    @Test
    fun `incBy - 음수로 감소도 가능하다`() {
        val update = "lives" incBy -1
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        val incClause = updateObject["\$inc"] as org.bson.Document
        incClause["lives"] shouldBeEqualTo -1
    }

    // ====================================================
    // unsetField
    // ====================================================

    @Test
    fun `unsetField - 필드를 제거하는 Update를 생성한다`() {
        val update = "tempField".unsetField()
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        updateObject.containsKey("\$unset") shouldBeEqualTo true
    }

    // ====================================================
    // pushValue / pullValue
    // ====================================================

    @Test
    fun `pushValue - 배열에 값을 추가하는 Update를 생성한다`() {
        val update = "tags" pushValue "kotlin"
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        val pushClause = updateObject["\$push"] as org.bson.Document
        pushClause["tags"] shouldBeEqualTo "kotlin"
    }

    @Test
    fun `pullValue - 배열에서 값을 제거하는 Update를 생성한다`() {
        val update = "tags" pullValue "deprecated"
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        val pullClause = updateObject["\$pull"] as org.bson.Document
        pullClause["tags"] shouldBeEqualTo "deprecated"
    }

    // ====================================================
    // 체이닝 확장
    // ====================================================

    @Test
    fun `andSet - 기존 Update에 추가 set을 체이닝한다`() {
        val update = ("name" setTo "Alice").andSet("age", 31).andSet("city", "Seoul")
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        val setClause = updateObject["\$set"] as org.bson.Document
        setClause["name"] shouldBeEqualTo "Alice"
        setClause["age"] shouldBeEqualTo 31
        setClause["city"] shouldBeEqualTo "Seoul"
    }

    @Test
    fun `andInc - 기존 Update에 추가 inc를 체이닝한다`() {
        val update = ("score" incBy 10).andInc("level", 1)
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        val incClause = updateObject["\$inc"] as org.bson.Document
        incClause["score"] shouldBeEqualTo 10
        incClause["level"] shouldBeEqualTo 1
    }

    @Test
    fun `andUnset - 기존 Update에 추가 unset을 체이닝한다`() {
        val update = ("name" setTo "Alice").andUnset("tempField")
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        updateObject.containsKey("\$set") shouldBeEqualTo true
        updateObject.containsKey("\$unset") shouldBeEqualTo true
    }

    @Test
    fun `andPush - 기존 Update에 추가 push를 체이닝한다`() {
        val update = ("score" incBy 10).andPush("history", 100)
        update.shouldNotBeNull()

        val updateObject = update.updateObject
        updateObject.containsKey("\$inc") shouldBeEqualTo true
        val pushClause = updateObject["\$push"] as org.bson.Document
        pushClause["history"] shouldBeEqualTo 100
    }
}
