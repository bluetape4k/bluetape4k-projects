package io.bluetape4k.rule.engines.spel

import io.bluetape4k.rule.api.RuleDefinition
import org.springframework.expression.BeanResolver
import org.springframework.expression.ParserContext

/**
 * [SpelCondition]을 생성합니다.
 *
 * ```kotlin
 * val condition = spelConditionOf("#amount > 1000")
 * val facts = Facts.of("amount" to 1500)
 * condition.evaluate(facts) // true
 * ```
 */
@JvmOverloads
fun spelConditionOf(
    expression: String,
    parserContext: ParserContext? = null,
    beanResolver: BeanResolver? = null,
): SpelCondition = SpelCondition(expression, parserContext, beanResolver)

/**
 * [SpelAction]을 생성합니다.
 *
 * ```kotlin
 * val action = spelActionOf("#discount = 10")
 * val facts = Facts.of("discount" to 0)
 * action.execute(facts)
 * ```
 */
@JvmOverloads
fun spelActionOf(
    expression: String,
    parserContext: ParserContext? = null,
    beanResolver: BeanResolver? = null,
): SpelAction = SpelAction(expression, parserContext, beanResolver)

/**
 * [RuleDefinition]으로부터 [SpelRule]을 빌드합니다.
 *
 * ```kotlin
 * val definition = RuleDefinition(
 *     name = "discountRule",
 *     condition = "#amount > 1000",
 *     actions = listOf("#discount = true")
 * )
 * val rule = definition.toSpelRule()
 * ```
 */
fun RuleDefinition.toSpelRule(): SpelRule {
    return SpelRule(name, description, priority)
        .also { rule ->
            rule.whenever(condition)
            this@toSpelRule.actions.forEach { action -> rule.then(action) }
        }
}
