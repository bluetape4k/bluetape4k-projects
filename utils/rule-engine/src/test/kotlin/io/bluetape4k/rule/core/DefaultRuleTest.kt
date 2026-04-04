package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Action
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class DefaultRuleTest {

    companion object: KLogging()

    @Test
    fun `기본 DefaultRule 생성`() {
        val rule = DefaultRule()
        rule.evaluate(Facts.empty()).shouldBeFalse()
    }

    @Test
    fun `condition과 action을 가진 DefaultRule 생성`() {
        val rule = DefaultRule(
            name = "testRule",
            description = "테스트 규칙",
            priority = 1,
            condition = Condition { it.get<Int>("value")!! > 10 },
            actions = listOf(Action { it["result"] = true })
        )

        val facts = Facts.of("value" to 20)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)
        facts.get<Boolean>("result") shouldBeEqualTo true
    }

    @Test
    fun `같은 이름의 Rule은 equals가 true`() {
        val rule1 = DefaultRule(name = "rule1")
        val rule2 = DefaultRule(name = "rule1", priority = 999)
        (rule1 == rule2).shouldBeTrue()
    }

    @Test
    fun `다른 이름의 Rule은 equals가 false`() {
        val rule1 = DefaultRule(name = "rule1")
        val rule2 = DefaultRule(name = "rule2")
        (rule1 == rule2).shouldBeFalse()
    }

    @Test
    fun `Rule 우선순위 비교`() {
        val highPriority = DefaultRule(name = "high", priority = 1)
        val lowPriority = DefaultRule(name = "low", priority = 10)
        (highPriority < lowPriority).shouldBeTrue()
    }
}
