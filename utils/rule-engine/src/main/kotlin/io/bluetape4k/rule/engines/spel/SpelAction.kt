package io.bluetape4k.rule.engines.spel

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.rule.api.Action
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.exception.RuleException
import io.bluetape4k.support.requireNotBlank
import org.springframework.expression.BeanResolver
import org.springframework.expression.Expression
import org.springframework.expression.ParserContext
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext

/**
 * Spring Expression Language(SpEL)을 이용한 [Action] 구현체입니다.
 *
 * @property expression SpEL 표현식
 */
class SpelAction private constructor(
    val expression: String,
    private val parserContext: ParserContext?,
    private val beanResolver: BeanResolver?,
): Action {

    companion object: KLogging() {
        @JvmOverloads
        operator fun invoke(
            expression: String,
            parserContext: ParserContext? = null,
            beanResolver: BeanResolver? = null,
        ): SpelAction {
            expression.requireNotBlank("expression")
            return SpelAction(expression, parserContext, beanResolver)
        }
    }

    private val compiledExpr: Expression by lazy {
        SpelExpressionParser().parseExpression(expression, parserContext)
    }

    override fun execute(facts: Facts) {
        try {
            val context = StandardEvaluationContext().apply {
                setRootObject(facts.asMap())
                @Suppress("UNCHECKED_CAST")
                setVariables(facts.asMap() as Map<String, Any>)
            }
            beanResolver?.run { context.setBeanResolver(this) }
            compiledExpr.getValue(context)
        } catch (e: Exception) {
            log.error(e) { "Fail to execute SpEL expression '$expression' on facts=$facts" }
            throw RuleException("Fail to execute SpEL expression", e)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpelAction) return false
        return expression == other.expression
    }

    override fun hashCode(): Int = expression.hashCode()

    override fun toString(): String = "SpelAction(expression='$expression')"
}
