package io.bluetape4k.rule.engines.mvel2

import io.bluetape4k.rule.api.RuleDefinition

/**
 * [MvelCondition]을 생성합니다.
 */
fun mvelConditionOf(expression: String): MvelCondition = MvelCondition(expression)

/**
 * [MvelAction]을 생성합니다.
 */
fun mvelActionOf(expression: String): MvelAction = MvelAction(expression)

/**
 * [RuleDefinition]으로부터 [MvelRule]을 빌드합니다.
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
