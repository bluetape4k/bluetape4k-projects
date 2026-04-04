package io.bluetape4k.rule.engines.mvel2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts
import org.mvel2.MVEL

/**
 * MVEL2 문법을 사용하는 [Condition] 구현체입니다.
 *
 * ```kotlin
 * val condition = MvelCondition("amount > 1000")
 * val result = condition.evaluate(facts)
 * ```
 *
 * @property expression MVEL2 표현식
 */
class MvelCondition(val expression: String): Condition {

    companion object: KLogging()

    private val compiledExpression by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MVEL.compileExpression(expression)
    }

    override fun evaluate(facts: Facts): Boolean {
        if (expression.isBlank()) return true

        return try {
            MVEL.executeExpression(compiledExpression, facts.asMap()) as Boolean
        } catch (e: Exception) {
            log.warn(e) { "Fail to evaluate expression '$expression' with facts=$facts" }
            false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MvelCondition) return false
        return expression == other.expression
    }

    override fun hashCode(): Int = expression.hashCode()

    override fun toString(): String = "MvelCondition(expression='$expression')"
}
