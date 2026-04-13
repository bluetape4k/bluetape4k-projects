package io.bluetape4k.rule.engines.groovy

import io.bluetape4k.rule.api.RuleDefinition

/**
 * [GroovyCondition]을 생성합니다.
 *
 * ```kotlin
 * val condition = groovyConditionOf("amount > 1000")
 * val facts = Facts.of("amount" to 1500)
 * condition.evaluate(facts) // true
 * ```
 */
fun groovyConditionOf(expression: String): GroovyCondition = GroovyCondition(expression)

/**
 * [GroovyAction]을 생성합���다.
 *
 * ```kotlin
 * val action = groovyActionOf("discount = 10")
 * val facts = Facts.of("amount" to 1500)
 * action.execute(facts)
 * ```
 */
fun groovyActionOf(script: String): GroovyAction = GroovyAction(script)

/**
 * [RuleDefinition]으로부터 [GroovyRule]을 빌드합니다.
 *
 * ```kotlin
 * val definition = RuleDefinition(
 *     name = "discountRule",
 *     condition = "amount > 1000",
 *     actions = listOf("discount = true")
 * )
 * val rule = definition.toGroovyRule()
 * ```
 */
fun RuleDefinition.toGroovyRule(): GroovyRule {
    return GroovyRule(name, description, priority)
        .also { rule ->
            rule.whenever(condition)
            this@toGroovyRule.actions.forEach { action -> rule.then(action) }
        }
}
