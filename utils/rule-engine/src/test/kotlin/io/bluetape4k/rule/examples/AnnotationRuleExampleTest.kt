package io.bluetape4k.rule.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.ruleSetOf
import io.bluetape4k.rule.core.DefaultRuleEngine
import io.bluetape4k.rule.core.asRule
import io.bluetape4k.rule.core.ruleEngine
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import io.bluetape4k.rule.annotation.Action as ActionAnnotation
import io.bluetape4k.rule.annotation.Condition as ConditionAnnotation
import io.bluetape4k.rule.annotation.Fact as FactAnnotation
import io.bluetape4k.rule.annotation.Rule as RuleAnnotation

class AnnotationRuleExampleTest {

    companion object: KLogging()

    @RuleAnnotation(name = "ageCheck", description = "성인 확인", priority = 1)
    class AgeCheckRule {
        @ConditionAnnotation
        fun isAdult(facts: Facts): Boolean = facts.get<Int>("age")!! >= 18

        @ActionAnnotation
        fun allow(facts: Facts) {
            facts["allowed"] = true
        }
    }

    @RuleAnnotation(name = "discountCheck", description = "VIP 할인")
    class VipDiscountRule {
        @ConditionAnnotation
        fun isVip(@FactAnnotation("isVip") vip: Boolean): Boolean = vip

        @ActionAnnotation(order = 1)
        fun applyDiscount(facts: Facts) {
            facts["discount"] = 20
        }

        @ActionAnnotation(order = 2)
        fun logDiscount(facts: Facts) {
            facts["discountApplied"] = true
        }
    }

    @Test
    fun `어노테이션 기반 Rule 사용 예제`() {
        val rule = AgeCheckRule().asRule()

        rule.name shouldBeEqualTo "ageCheck"
        rule.description shouldBeEqualTo "성인 확인"

        val facts = Facts.of("age" to 20)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)
        facts.get<Boolean>("allowed") shouldBeEqualTo true
    }

    @Test
    fun `어노테이션 기반 Rule을 엔진에서 실행`() {
        val engine = DefaultRuleEngine()
        val rule = AgeCheckRule().asRule()

        val facts = Facts.of("age" to 25)
        engine.fire(ruleSetOf(rule), facts)
        facts.get<Boolean>("allowed") shouldBeEqualTo true
    }

    @Test
    fun `Fact 어노테이션으로 파라미터 주입 예제`() {
        val engine = ruleEngine { }
        val rule = VipDiscountRule().asRule()

        val facts = Facts.of("isVip" to true)
        engine.fire(ruleSetOf(rule), facts)

        facts.get<Int>("discount") shouldBeEqualTo 20
        facts.get<Boolean>("discountApplied") shouldBeEqualTo true
    }

    @Test
    fun `어노테이션 Rule과 DSL Rule 혼합 사용`() {
        val engine = DefaultRuleEngine()

        val annotatedRule = AgeCheckRule().asRule()
        val dslRule = io.bluetape4k.rule.core.rule {
            name = "welcome"
            priority = 100
            condition { facts -> facts.get<Boolean>("allowed") == true }
            action { facts -> facts["welcome"] = "성인 회원입니다" }
        }

        val facts = Facts.of("age" to 20)
        engine.fire(ruleSetOf(annotatedRule, dslRule), facts)

        facts.get<Boolean>("allowed") shouldBeEqualTo true
        facts.get<String>("welcome") shouldBeEqualTo "성인 회원입니다"
    }
}
