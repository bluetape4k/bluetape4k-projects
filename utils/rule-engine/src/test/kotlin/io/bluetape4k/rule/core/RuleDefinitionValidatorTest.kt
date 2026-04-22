package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.annotation.Action
import io.bluetape4k.rule.annotation.Condition
import io.bluetape4k.rule.annotation.Fact
import io.bluetape4k.rule.annotation.Priority
import io.bluetape4k.rule.annotation.Rule
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.exception.InvalidRuleDefinitionException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class RuleDefinitionValidatorTest {

    companion object: KLogging()

    private val validator = RuleDefinitionValidator()

    @Rule(name = "validRule")
    class ValidRule {
        @Condition
        fun check(): Boolean = true

        @Action
        fun doAction() {}
    }

    @Rule(name = "factParamRule")
    class FactParamRule {
        @Condition
        fun check(facts: Facts): Boolean = facts.get<Int>("val") != null

        @Action
        fun execute(facts: Facts) {
            facts["done"] = true
        }
    }

    @Rule(name = "annotatedFactRule")
    class AnnotatedFactRule {
        @Condition
        fun check(@Fact("score") score: Int): Boolean = score >= 60

        @Action
        fun execute(@Fact("result") result: String?) {}
    }

    @Rule(name = "priorityRule")
    class PriorityRule {
        @Condition
        fun check(): Boolean = true

        @Action
        fun doAction() {}

        @Priority
        fun myPriority(): Int = 10
    }

    class NoAnnotationRule {
        @Condition
        fun check(): Boolean = true

        @Action
        fun doAction() {}
    }

    @Rule(name = "noConditionRule")
    class NoConditionRule {
        @Action
        fun doAction() {}
    }

    @Rule(name = "noActionRule")
    class NoActionRule {
        @Condition
        fun check(): Boolean = true
    }

    @Rule(name = "multipleConditionRule")
    class MultipleConditionRule {
        @Condition
        fun check1(): Boolean = true

        @Condition
        fun check2(): Boolean = false

        @Action
        fun doAction() {}
    }

    @Rule(name = "invalidPriorityRule")
    class InvalidPriorityMethod {
        @Condition
        fun check(): Boolean = true

        @Action
        fun doAction() {}

        @Priority
        fun priority(extra: Int): Int = 0 // invalid: has params
    }

    @Test
    fun `유효한 Rule 검증 통과`() {
        validator.validate(ValidRule())
    }

    @Test
    fun `Facts 파라미터 Rule 검증 통과`() {
        validator.validate(FactParamRule())
    }

    @Test
    fun `@Fact 어노테이션 파라미터 Rule 검증 통과`() {
        validator.validate(AnnotatedFactRule())
    }

    @Test
    fun `@Priority 메서드 포함 Rule 검증 통과`() {
        validator.validate(PriorityRule())
    }

    @Test
    fun `@Rule 어노테이션 없는 클래스 예외 발생`() {
        assertFailsWith<InvalidRuleDefinitionException> {
            validator.validate(NoAnnotationRule())
        }
    }

    @Test
    fun `@Condition 없는 Rule 예외 발생`() {
        assertFailsWith<InvalidRuleDefinitionException> {
            validator.validate(NoConditionRule())
        }
    }

    @Test
    fun `@Action 없는 Rule 예외 발생`() {
        assertFailsWith<InvalidRuleDefinitionException> {
            validator.validate(NoActionRule())
        }
    }

    @Test
    fun `@Condition 여러 개이면 예외 발생`() {
        assertFailsWith<InvalidRuleDefinitionException> {
            validator.validate(MultipleConditionRule())
        }
    }

    @Test
    fun `findRuleAnnotation 정상 동작`() {
        val annotation = ValidRule::class.java.findRuleAnnotation()
        annotation.shouldNotBeNull()
        annotation.name shouldBeEqualTo "validRule"
    }
}

