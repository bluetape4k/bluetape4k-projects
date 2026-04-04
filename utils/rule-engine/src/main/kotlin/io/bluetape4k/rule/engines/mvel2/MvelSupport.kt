package io.bluetape4k.rule.engines.mvel2

import io.bluetape4k.rule.api.RuleDefinition

/**
 * [MvelCondition]을 생성합니다.
 *
 * ```kotlin
 * val condition = mvelConditionOf("amount > 1000")
 * val facts = Facts.of("amount" to 1500)
 * condition.evaluate(facts) // true
 * ```
 */
fun mvelConditionOf(expression: String): MvelCondition = MvelCondition(expression)

/**
 * [MvelAction]을 생성합니다.
 *
 * ```kotlin
 * val action = mvelActionOf("discount = 10")
 * val facts = Facts.of("amount" to 1500)
 * action.execute(facts)
 * ```
 */
fun mvelActionOf(expression: String): MvelAction = MvelAction(expression)

/**
 * [RuleDefinition]으로부터 [MvelRule]을 빌드합니다.
 *
 * ```kotlin
 * val definition = RuleDefinition(
 *     name = "discountRule",
 *     condition = "amount > 1000",
 *     actions = listOf("discount = true")
 * )
 * val rule = definition.toMvelRule()
 * ```
 */
fun RuleDefinition.toMvelRule(): MvelRule {
    return MvelRule(name, description, priority)
        .also { mvel ->
            mvel.whenever(condition)
            this@toMvelRule.actions.forEach { action ->
                mvel.then(action)
            }
        }
}
