package io.bluetape4k.rule.engines.janino

import io.bluetape4k.rule.api.RuleDefinition

/**
 * [JaninoCondition]을 생성합니다.
 *
 * ```kotlin
 * val condition = janinoConditionOf("((Integer)facts.get(\"amount\")).intValue() > 1000")
 * val facts = Facts.of("amount" to 1500)
 * condition.evaluate(facts) // true
 * ```
 */
fun janinoConditionOf(expression: String): JaninoCondition = JaninoCondition(expression)

/**
 * [JaninoAction]을 생성합니다.
 *
 * ```kotlin
 * val action = janinoActionOf("facts.put(\"discount\", 10);")
 * val facts = Facts.of("amount" to 1500)
 * action.execute(facts)
 * ```
 */
fun janinoActionOf(script: String): JaninoAction = JaninoAction(script)

/**
 * [RuleDefinition]으로부터 [JaninoRule]을 빌드합니다.
 *
 * ```kotlin
 * val definition = RuleDefinition(
 *     name = "discountRule",
 *     condition = "((Integer)facts.get(\"amount\")).intValue() > 1000",
 *     actions = listOf("facts.put(\"discount\", true);")
 * )
 * val rule = definition.toJaninoRule()
 * ```
 */
fun RuleDefinition.toJaninoRule(): JaninoRule {
    return JaninoRule(name, description, priority)
        .also { rule ->
            rule.whenever(condition)
            this@toJaninoRule.actions.forEach { action -> rule.then(action) }
        }
}
