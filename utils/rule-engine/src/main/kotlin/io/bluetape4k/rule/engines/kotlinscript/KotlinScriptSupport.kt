package io.bluetape4k.rule.engines.kotlinscript

import io.bluetape4k.rule.api.RuleDefinition

/**
 * [KotlinScriptCondition]을 생성합니다.
 *
 * ```kotlin
 * val condition = kotlinScriptConditionOf("val amount = bindings[\"amount\"] as Int; amount > 1000")
 * val facts = Facts.of("amount" to 1500)
 * condition.evaluate(facts) // true
 * ```
 */
fun kotlinScriptConditionOf(script: String): KotlinScriptCondition = KotlinScriptCondition(script)

/**
 * [KotlinScriptAction]을 생성합니다.
 *
 * ```kotlin
 * val action = kotlinScriptActionOf("bindings[\"discount\"] = 10")
 * val facts = Facts.of("amount" to 1500)
 * action.execute(facts)
 * ```
 */
fun kotlinScriptActionOf(script: String): KotlinScriptAction = KotlinScriptAction(script)

/**
 * [RuleDefinition]으로부터 [KotlinScriptRule]을 빌드합니다.
 *
 * ```kotlin
 * val definition = RuleDefinition(
 *     name = "calcRule",
 *     condition = "val amount = bindings[\"amount\"] as Int; amount > 1000",
 *     actions = listOf("bindings[\"discount\"] = true")
 * )
 * val rule = definition.toKotlinScriptRule()
 * ```
 */
fun RuleDefinition.toKotlinScriptRule(): KotlinScriptRule {
    return KotlinScriptRule(name, description, priority)
        .also { rule ->
            rule.whenever(condition)
            this@toKotlinScriptRule.actions.forEach { action -> rule.then(action) }
        }
}
