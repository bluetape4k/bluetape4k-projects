package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.ruleSetOf
import io.bluetape4k.rule.exception.InvalidRuleDefinitionException
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.rule.annotation.Action as ActionAnnotation
import io.bluetape4k.rule.annotation.Condition as ConditionAnnotation
import io.bluetape4k.rule.annotation.Fact as FactAnnotation
import io.bluetape4k.rule.annotation.Priority as PriorityAnnotation
import io.bluetape4k.rule.annotation.Rule as RuleAnnotation

class RuleProxyTest {

    companion object: KLogging()

    @RuleAnnotation(name = "ageCheck", description = "성인 확인")
    class AgeCheckRule {
        @ConditionAnnotation
        fun isAdult(facts: Facts): Boolean = facts.get<Int>("age")!! >= 18

        @ActionAnnotation
        fun allow(facts: Facts) {
            facts["allowed"] = true
        }
    }

    @RuleAnnotation(name = "factInjection", description = "Fact 주입 테스트")
    class FactInjectionRule {
        @ConditionAnnotation
        fun check(@FactAnnotation("score") score: Int): Boolean = score > 50

        @ActionAnnotation
        fun execute(facts: Facts) {
            facts["passed"] = true
        }
    }

    @RuleAnnotation(name = "priorityRule")
    class CustomPriorityRule {
        @PriorityAnnotation
        fun getPriority(): Int = 5

        @ConditionAnnotation
        fun check(facts: Facts): Boolean = true

        @ActionAnnotation
        fun execute(facts: Facts) {
            facts["executed"] = true
        }
    }

    @RuleAnnotation(name = "multiAction")
    class MultiActionRule {
        @ConditionAnnotation
        fun check(facts: Facts): Boolean = true

        @ActionAnnotation(order = 1)
        fun first(facts: Facts) {
            facts["first"] = true
        }

        @ActionAnnotation(order = 2)
        fun second(facts: Facts) {
            facts["second"] = true
        }
    }

    class NotAnnotatedRule {
        fun check(facts: Facts): Boolean = true
        fun execute(facts: Facts) {}
    }

    open class AnnotatedMethods(private val annotatedPriority: Int = 7) {
        @PriorityAnnotation
        fun priority(): Int = annotatedPriority

        @ConditionAnnotation
        fun matches(@FactAnnotation("score") score: Int): Boolean = score > 50

        @ActionAnnotation
        fun record(@FactAnnotation("events") events: MutableList<String>) {
            events += "annotated"
        }
    }

    @RuleAnnotation(name = "overloaded")
    class OverloadedRule: AnnotatedMethods() {
        fun priority(ignored: Int): Int = ignored

        fun matches(facts: Facts): Boolean = facts.containsKey("wrong-overload")

        fun record(facts: Facts) {
            facts["wrong-overload"] = true
        }
    }

    interface GenericCondition<T> {
        fun matches(value: T): Boolean
    }

    @RuleAnnotation(name = "bridge")
    class BridgeConditionRule: AnnotatedMethods(), GenericCondition<String> {
        override fun matches(value: String): Boolean = value.isNotEmpty()
    }

    @Test
    fun `어노테이션 기반 Rule을 asRule로 변환`() {
        val rule = AgeCheckRule().asRule()

        rule.name shouldBeEqualTo "ageCheck"
        rule.description shouldBeEqualTo "성인 확인"

        val facts = Facts.of("age" to 20)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)
        facts.get<Boolean>("allowed").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `Fact 어노테이션으로 파라미터 주입`() {
        val rule = FactInjectionRule().asRule()

        val facts = Facts.of("score" to 80)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)
        facts.get<Boolean>("passed").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `커스텀 Priority 적용`() {
        val rule = CustomPriorityRule().asRule()
        rule.priority shouldBeEqualTo 5
    }

    @Test
    fun `여러 Action 순서대로 실행`() {
        val rule = MultiActionRule().asRule()

        val facts = Facts.empty()
        rule.execute(facts)
        facts.get<Boolean>("first").shouldNotBeNull().shouldBeTrue()
        facts.get<Boolean>("second").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `Rule 어노테이션이 없는 클래스는 예외 발생`() {
        assertFailsWith<InvalidRuleDefinitionException> {
            NotAnnotatedRule().asRule()
        }
    }

    @Test
    fun `Rule 인터페이스 구현체는 그대로 반환`() {
        val original = rule {
            name = "original"
            condition { true }
            action { }
        }
        val result = RuleProxy.asRule(original)
        (result === original).shouldBeTrue()
    }

    @Test
    fun `Proxy Rule을 RuleSet에 등록하여 실행`() {
        val engine = DefaultRuleEngine()
        val annotatedRule = AgeCheckRule().asRule()

        val facts = Facts.of("age" to 25)
        engine.fire(ruleSetOf(annotatedRule), facts); facts.get<Boolean>("allowed").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `Proxy Rule의 equals와 hashCode 동작`() {
        val rule1 = AgeCheckRule().asRule()
        val rule2 = AgeCheckRule().asRule()
        (rule1 == rule2).shouldBeTrue()
        (rule1.hashCode() == rule2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `Priority는 이름이 같은 overload가 있어도 annotated descriptor를 호출한다`() {
        val rule = OverloadedRule().asRule()

        rule.priority shouldBeEqualTo 7
    }

    @Test
    fun `Condition은 이름이 같은 overload가 있어도 annotated descriptor를 호출한다`() {
        val rule = OverloadedRule().asRule()

        rule.evaluate(Facts.of("score" to 80)).shouldBeTrue()
    }

    @Test
    fun `Action은 이름이 같은 overload가 있어도 annotated descriptor를 호출한다`() {
        val rule = OverloadedRule().asRule()
        val events = mutableListOf<String>()
        val facts = Facts.of("events" to events)

        rule.execute(facts)

        events shouldBeEqualTo listOf("annotated")
    }

    @Test
    fun `Condition은 compiler bridge가 있어도 annotated descriptor를 호출한다`() {
        val bridge = BridgeConditionRule::class.java.methods.single { it.name == "matches" && it.isBridge }
        bridge.parameterTypes.single() shouldBeEqualTo Any::class.java

        val rule = BridgeConditionRule().asRule()

        rule.evaluate(Facts.of("score" to 80)).shouldBeTrue()
    }
}
