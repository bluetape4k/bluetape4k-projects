package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.ruleSetOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class RuleDslTest {

    companion object: KLogging()

    @Test
    fun `rule DSL로 Rule 생성`() {
        val discountRule = rule {
            name = "discount"
            description = "1000원 이상 구매 시 할인 적용"
            priority = 1
            condition { facts -> facts.get<Int>("amount")!! > 1000 }
            action { facts -> facts["discount"] = true }
        }

        discountRule.name shouldBeEqualTo "discount"
        discountRule.priority shouldBeEqualTo 1

        val facts = Facts.of("amount" to 1500)
        discountRule.evaluate(facts).shouldBeTrue()
        discountRule.execute(facts)
        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `rule DSL 조건 불만족 시`() {
        val rule = rule {
            name = "test"
            condition { facts -> facts.get<Int>("value")!! > 100 }
            action { facts -> facts["result"] = true }
        }

        val facts = Facts.of("value" to 50)
        rule.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `suspendRule DSL로 SuspendRule 생성`() = runTest {
        val asyncRule = suspendRule {
            name = "asyncRule"
            description = "비동기 규칙"
            priority = 1
            condition { facts -> facts.get<Int>("value")!! > 0 }
            action { facts -> facts["processed"] = true }
        }

        asyncRule.name shouldBeEqualTo "asyncRule"
        val facts = Facts.of("value" to 10)
        asyncRule.evaluate(facts).shouldBeTrue()
        asyncRule.execute(facts)
        facts.get<Boolean>("processed").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `ruleEngine DSL로 엔진 생성`() {
        val engine = ruleEngine {
            skipOnFirstAppliedRule = true
            priorityThreshold = 100
        }

        engine.config.skipOnFirstAppliedRule.shouldBeTrue()
        engine.config.priorityThreshold shouldBeEqualTo 100
    }

    @Test
    fun `ruleEngine DSL과 rule DSL 통합 테스트`() {
        val engine = ruleEngine { skipOnFirstAppliedRule = true }

        val rule1 = rule {
            name = "rule1"
            priority = 1
            condition { true }
            action { facts -> facts["winner"] = "rule1" }
        }
        val rule2 = rule {
            name = "rule2"
            priority = 2
            condition { true }
            action { facts -> facts["winner"] = "rule2" }
        }

        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule1, rule2), facts)
        facts.get<String>("winner") shouldBeEqualTo "rule1"
    }

    @Test
    fun `rule DSL 여러 action 등록`() {
        val multiRule = rule {
            name = "multi"
            condition { true }
            action { facts -> facts["step1"] = true }
            action { facts -> facts["step2"] = true }
        }

        val facts = Facts.empty()
        multiRule.execute(facts)
        facts.get<Boolean>("step1").shouldNotBeNull().shouldBeTrue()
        facts.get<Boolean>("step2").shouldNotBeNull().shouldBeTrue()
    }
}
