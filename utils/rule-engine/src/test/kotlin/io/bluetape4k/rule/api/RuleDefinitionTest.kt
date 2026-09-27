package io.bluetape4k.rule.api

import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import org.junit.jupiter.api.Test

class RuleDefinitionTest {

    companion object: KLogging()

    @Test
    fun `기본값으로 RuleDefinition 생성`() {
        val def = RuleDefinition()
        def.name shouldBeEqualTo DEFAULT_RULE_NAME
        def.description shouldBeEqualTo DEFAULT_RULE_DESCRIPTION
        def.priority shouldBeEqualTo DEFAULT_RULE_PRIORITY
        def.condition.shouldBeEmpty()
        def.actions.shouldBeEmpty()
    }

    @Test
    fun `모든 필드 지정하여 RuleDefinition 생성`() {
        val def = RuleDefinition(
            name = "discountRule",
            description = "할인 규칙",
            priority = 1,
            condition = "amount > 1000",
            actions = listOf("discount = true", "notify = true")
        )
        def.name shouldBeEqualTo "discountRule"
        def.description shouldBeEqualTo "할인 규칙"
        def.priority shouldBeEqualTo 1
        def.condition shouldBeEqualTo "amount > 1000"
        def.actions shouldHaveSize 2
        def.actions[0] shouldBeEqualTo "discount = true"
    }

    @Test
    fun `RuleDefinition data class copy`() {
        val original = RuleDefinition(name = "original", priority = 1)
        val copy = original.copy(name = "copy", priority = 2)
        copy.name shouldBeEqualTo "copy"
        copy.priority shouldBeEqualTo 2
        // original unchanged
        original.name shouldBeEqualTo "original"
    }

    @Test
    fun `RuleDefinition equals and hashCode`() {
        val d1 = RuleDefinition(name = "rule", condition = "x > 0")
        val d2 = RuleDefinition(name = "rule", condition = "x > 0")
        d1 shouldBeEqualTo d2
        d1.hashCode() shouldBeEqualTo d2.hashCode()
    }
}
