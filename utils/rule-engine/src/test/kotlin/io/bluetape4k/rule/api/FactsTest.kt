package io.bluetape4k.rule.api

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class FactsTest {

    companion object: KLogging()

    @Test
    fun `빈 Facts 생성`() {
        val facts = Facts.empty()
        facts.isEmpty().shouldBeTrue()
        facts.size shouldBeEqualTo 0
    }

    @Test
    fun `of 팩토리로 Facts 생성`() {
        val facts = Facts.of("name" to "debop", "age" to 30)
        facts.size shouldBeEqualTo 2
        facts.get<String>("name") shouldBeEqualTo "debop"
        facts.get<Int>("age") shouldBeEqualTo 30
    }

    @Test
    fun `put과 get으로 Fact 추가 및 조회`() {
        val facts = Facts.empty()
        facts["score"] = 100
        facts.get<Int>("score") shouldBeEqualTo 100
    }

    @Test
    fun `remove로 Fact 제거`() {
        val facts = Facts.of("key" to "value")
        facts.containsKey("key").shouldBeTrue()
        facts.remove("key")
        facts.containsKey("key").shouldBeFalse()
    }

    @Test
    fun `asMap으로 읽기 전용 Map 반환`() {
        val facts = Facts.of("a" to 1, "b" to 2)
        val map = facts.asMap()
        map.size shouldBeEqualTo 2
        map["a"] shouldBeEqualTo 1
    }

    @Test
    fun `존재하지 않는 키 조회 시 null 반환`() {
        val facts = Facts.empty()
        facts.get<String>("missing").shouldBeNull()
    }
}
