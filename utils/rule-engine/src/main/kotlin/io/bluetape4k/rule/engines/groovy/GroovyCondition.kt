package io.bluetape4k.rule.engines.groovy

import groovy.lang.GroovyShell
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.support.requireNotBlank
import org.codehaus.groovy.control.CompilerConfiguration

/**
 * Groovy 스크립트를 사용하는 [Condition] 구현체입니���.
 *
 * Groovy의 동적 타이핑, 클로저, Java 호환 문법을 활용하여
 * 복잡한 조건 로직을 간결하게 표현할 수 있습니다.
 * 바이트코드로 컴파일되어 실행됩니다.
 *
 * ```kotlin
 * val condition = GroovyCondition("amount > 1000")
 * val result = condition.evaluate(facts)
 * ```
 *
 * @property expression Groovy 표현식 (boolean 반환)
 */
class GroovyCondition(val expression: String): Condition {

    companion object: KLogging() {
        private val compilerConfig = CompilerConfiguration().apply {
            sourceEncoding = "UTF-8"
        }
    }

    init {
        expression.requireNotBlank("expression")
    }

    private val parsedScript by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        GroovyShell(compilerConfig).parse(expression)
    }

    override fun evaluate(facts: Facts): Boolean {
        return try {
            val binding = NullSafeBinding(facts.asMap())
            parsedScript.binding = binding
            parsedScript.run() as Boolean
        } catch (e: Exception) {
            log.warn(e) { "Fail to evaluate Groovy expression '$expression' with facts=$facts" }
            false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroovyCondition) return false
        return expression == other.expression
    }

    override fun hashCode(): Int = expression.hashCode()

    override fun toString(): String = "GroovyCondition(expression='$expression')"
}
