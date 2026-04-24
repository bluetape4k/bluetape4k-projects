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

    @Test
    fun `null 값 설정 시 키가 제거된다`() {
        val facts = Facts.of("key" to "value")
        facts.containsKey("key").shouldBeTrue()
        facts["key"] = null
        facts.containsKey("key").shouldBeFalse()
        facts.get<String>("key").shouldBeNull()
    }

    @Test
    fun `put으로 null 값 설정 시 키가 제거된다`() {
        val facts = Facts.of("key" to "value")
        facts.put("key", null)
        facts.containsKey("key").shouldBeFalse()
    }

    @Test
    fun `of 팩토리에서 null 값 쌍은 무시된다`() {
        val facts = Facts.of("a" to 1, "b" to null, "c" to 3)
        facts.size shouldBeEqualTo 2
        facts.containsKey("a").shouldBeTrue()
        facts.containsKey("b").shouldBeFalse()
        facts.containsKey("c").shouldBeTrue()
    }

    @Test
    fun `from 팩토리에서 null 값은 무시된다`() {
        val map = mapOf("x" to 1, "y" to null, "z" to "hello")
        val facts = Facts.from(map)
        facts.size shouldBeEqualTo 2
        facts.containsKey("y").shouldBeFalse()
        facts.get<Int>("x") shouldBeEqualTo 1
    }

    @Test
    fun `clear 후 Facts가 비어있다`() {
        val facts = Facts.of("a" to 1, "b" to 2)
        facts.clear()
        facts.isEmpty().shouldBeTrue()
        facts.size shouldBeEqualTo 0
    }

    @Test
    fun `putAll로 여러 Fact를 한번에 추가한다`() {
        val facts = Facts.empty()
        facts.putAll("x" to 10, "y" to 20)
        facts.get<Int>("x") shouldBeEqualTo 10
        facts.get<Int>("y") shouldBeEqualTo 20
    }

    @Test
    fun `equals와 hashCode가 정상 동작한다`() {
        val facts1 = Facts.of("a" to 1, "b" to 2)
        val facts2 = Facts.of("a" to 1, "b" to 2)
        (facts1 == facts2).shouldBeTrue()
        (facts1.hashCode() == facts2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `toString이 정상 동작한다`() {
        val facts = Facts.of("name" to "test")
        val str = facts.toString()
        str.contains("name=test").shouldBeTrue()
    }
}
