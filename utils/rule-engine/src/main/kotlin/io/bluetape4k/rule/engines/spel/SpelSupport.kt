package io.bluetape4k.rule.engines.spel

import io.bluetape4k.rule.api.RuleDefinition
import org.springframework.expression.BeanResolver
import org.springframework.expression.ParserContext

/**
 * [SpelCondition]을 생성합니다.
 */
@JvmOverloads
fun spelConditionOf(
    expression: String,
    parserContext: ParserContext? = null,
    beanResolver: BeanResolver? = null,
): SpelCondition = SpelCondition(expression, parserContext, beanResolver)

/**
 * [SpelAction]을 생성합니다.
 */
@JvmOverloads
fun spelActionOf(
    expression: String,
    parserContext: ParserContext? = null,
    beanResolver: BeanResolver? = null,
): SpelAction = SpelAction(expression, parserContext, beanResolver)

/**
 * [RuleDefinition]으로부터 [SpelRule]을 빌드합니다.
 */
fun RuleDefinition.toSpelRule(): SpelRule {
    return SpelRule(name, description, priority)
        .also { rule ->
            rule.whenever(condition)
            this@toSpelRule.actions.forEach { action -> rule.then(action) }
        }
}
