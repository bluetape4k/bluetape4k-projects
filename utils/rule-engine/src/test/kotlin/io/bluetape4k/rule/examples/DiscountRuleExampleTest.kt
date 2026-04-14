package io.bluetape4k.rule.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.ruleSetOf
import io.bluetape4k.rule.core.DefaultRuleEngine
import io.bluetape4k.rule.core.rule
import io.bluetape4k.rule.core.ruleEngine
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class DiscountRuleExampleTest {

    companion object: KLogging()

    @Test
    fun `할인 규칙 적용 예제`() {
        val discountRule = rule {
            name = "discount"
            description = "1000원 이상 구매 시 할인 적용"
            priority = 1
            condition { facts -> facts.get<Int>("amount")!! > 1000 }
            action { facts -> facts["discount"] = true }
        }

        val engine = ruleEngine { skipOnFirstAppliedRule = true }
        val facts = Facts.of("amount" to 1500)
        engine.fire(ruleSetOf(discountRule), facts)

        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `할인 미적용 예제`() {
        val discountRule = rule {
            name = "discount"
            description = "1000원 이상 구매 시 할인 적용"
            priority = 1
            condition { facts -> facts.get<Int>("amount")!! > 1000 }
            action { facts -> facts["discount"] = true }
        }

        val engine = DefaultRuleEngine()
        val facts = Facts.of("amount" to 500)
        engine.fire(ruleSetOf(discountRule), facts)

        facts.containsKey("discount").shouldBeFalse()
    }

    @Test
    fun `여러 규칙 적용 예제`() {
        val discountRule = rule {
            name = "discount"
            priority = 1
            condition { facts -> facts.get<Int>("amount")!! > 1000 }
            action { facts -> facts["discount"] = 10 }
        }

        val freeShippingRule = rule {
            name = "freeShipping"
            priority = 2
            condition { facts -> facts.get<Int>("amount")!! > 5000 }
            action { facts -> facts["freeShipping"] = true }
        }

        val vipRule = rule {
            name = "vip"
            priority = 3
            condition { facts -> facts.get<Boolean>("isVip") == true }
            action { facts ->
                val currentDiscount = facts.get<Int>("discount") ?: 0
                facts["discount"] = currentDiscount + 5
            }
        }

        val engine = DefaultRuleEngine()
        val facts = Facts.of("amount" to 10000, "isVip" to true)
        engine.fire(ruleSetOf(discountRule, freeShippingRule, vipRule), facts)

        facts.get<Int>("discount") shouldBeEqualTo 15
        facts.get<Boolean>("freeShipping").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `check로 규칙 평가만 수행`() {
        val rule1 = rule {
            name = "rule1"
            condition { facts -> facts.get<Int>("value")!! > 10 }
            action { }
        }
        val rule2 = rule {
            name = "rule2"
            condition { facts -> facts.get<Int>("value")!! > 100 }
            action { }
        }

        val engine = DefaultRuleEngine()
        val facts = Facts.of("value" to 50)
        val result = engine.check(ruleSetOf(rule1, rule2), facts)

        result[rule1].shouldNotBeNull().shouldBeTrue()
        result[rule2].shouldNotBeNull().shouldBeFalse()
    }
}
