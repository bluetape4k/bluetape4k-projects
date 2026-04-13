package io.bluetape4k.rule.engines.janino

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.support.requireNotBlank
import org.codehaus.janino.ExpressionEvaluator

/**
 * Janino 컴파일러를 사용하는 [Condition] 구현체입니다.
 *
 * Java 표현식을 바이트코드로 컴파일하여 네이티브 속도로 실행합니다.
 * 대량의 룰을 반복 평가할 때 인터프리터 기반 엔진 대비 우수한 성능을 제공합니다.
 *
 * ```kotlin
 * val condition = JaninoCondition("((Integer)facts.get(\"amount\")).intValue() > 1000")
 * val result = condition.evaluate(facts)
 * ```
 *
 * @property expression Janino Java 표현식 (boolean 반환)
 */
class JaninoCondition(val expression: String): Condition {

    companion object: KLogging()

    init {
        expression.requireNotBlank("expression")
    }

    private val evaluator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ExpressionEvaluator().apply {
            setExpressionType(Boolean::class.javaPrimitiveType)
            setParameters(arrayOf("facts"), arrayOf(Map::class.java))
            cook(expression)
        }
    }

    override fun evaluate(facts: Facts): Boolean {
        return try {
            evaluator.evaluate(arrayOf<Any?>(facts.asMap())) as Boolean
        } catch (e: Exception) {
            log.warn(e) { "Fail to evaluate Janino expression '$expression' with facts=$facts" }
            false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JaninoCondition) return false
        return expression == other.expression
    }

    override fun hashCode(): Int = expression.hashCode()

    override fun toString(): String = "JaninoCondition(expression='$expression')"
}
