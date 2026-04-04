package io.bluetape4k.rule.engines.mvel2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.rule.api.Action
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.exception.RuleException
import io.bluetape4k.support.requireNotBlank
import org.mvel2.MVEL

/**
 * MVEL2 문법으로 표현하는 [Action] 구현체입니다.
 *
 * ```kotlin
 * val action = MvelAction("discount = true")
 * val facts = Facts.of("amount" to 1500)
 * action.execute(facts)
 * ```
 *
 * @property expression MVEL2 표현식
 */
class MvelAction(val expression: String): Action {

    companion object: KLogging()

    init {
        expression.requireNotBlank("expression")
    }

    private val compiledExpression by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MVEL.compileExpression(expression)
    }

    override fun execute(facts: Facts) {
        try {
            MVEL.executeExpression(compiledExpression, facts.asMap())
        } catch (e: Exception) {
            log.error(e) { "Fail to execute expression '$expression' on facts=$facts" }
            throw RuleException("Fail to execute MVEL expression", e)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MvelAction) return false
        return expression == other.expression
    }

    override fun hashCode(): Int = expression.hashCode()

    override fun toString(): String = "MvelAction(expression='$expression')"
}
