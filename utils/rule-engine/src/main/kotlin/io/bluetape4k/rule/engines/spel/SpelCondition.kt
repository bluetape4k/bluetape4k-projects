package io.bluetape4k.rule.engines.spel

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.support.requireNotBlank
import org.springframework.expression.BeanResolver
import org.springframework.expression.Expression
import org.springframework.expression.ParserContext
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext

/**
 * Spring Expression Language(SpEL)을 이용한 [Condition] 구현체입니다.
 *
 * ```kotlin
 * val condition = SpelCondition("#amount > 1000")
 * val result = condition.evaluate(facts)
 * ```
 *
 * @property expression SpEL 표현식
 */
class SpelCondition private constructor(
    val expression: String,
    private val parserContext: ParserContext?,
    private val beanResolver: BeanResolver?,
): Condition {

    companion object: KLogging() {
        @JvmOverloads
        operator fun invoke(
            expression: String,
            parserContext: ParserContext? = null,
            beanResolver: BeanResolver? = null,
        ): SpelCondition {
            expression.requireNotBlank("expression")
            return SpelCondition(expression, parserContext, beanResolver)
        }
    }

    private val compiledExpr: Expression by lazy {
        SpelExpressionParser().parseExpression(expression, parserContext)
    }

    override fun evaluate(facts: Facts): Boolean {
        return try {
            val context = StandardEvaluationContext().apply {
                setRootObject(facts.asMap())
                @Suppress("UNCHECKED_CAST")
                setVariables(facts.asMap() as Map<String, Any>)
            }
            beanResolver?.run { context.setBeanResolver(this) }
            compiledExpr.getValue(context, Boolean::class.java) ?: false
        } catch (e: Exception) {
            log.warn(e) { "Fail to evaluate SpEL expression '$expression' with facts=$facts" }
            false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpelCondition) return false
        return expression == other.expression
    }

    override fun hashCode(): Int = expression.hashCode()

    override fun toString(): String = "SpelCondition(expression='$expression')"
}
