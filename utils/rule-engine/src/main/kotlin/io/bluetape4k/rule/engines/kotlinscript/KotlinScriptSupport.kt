package io.bluetape4k.rule.engines.kotlinscript

import io.bluetape4k.rule.api.RuleDefinition

/**
 * [KotlinScriptCondition]을 생성합니다.
 */
fun kotlinScriptConditionOf(script: String): KotlinScriptCondition = KotlinScriptCondition(script)

/**
 * [KotlinScriptAction]을 생성합니다.
 */
fun kotlinScriptActionOf(script: String): KotlinScriptAction = KotlinScriptAction(script)

/**
 * [RuleDefinition]으로부터 [KotlinScriptRule]을 빌드합니다.
 */
fun RuleDefinition.toKotlinScriptRule(): KotlinScriptRule {
    return KotlinScriptRule(name, description, priority)
        .also { rule ->
            rule.whenever(condition)
            this@toKotlinScriptRule.actions.forEach { action -> rule.then(action) }
        }
}
