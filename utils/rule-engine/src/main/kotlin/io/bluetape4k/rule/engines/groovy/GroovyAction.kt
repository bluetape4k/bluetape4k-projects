package io.bluetape4k.rule.engines.groovy

import groovy.lang.GString
import groovy.lang.GroovyShell
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.rule.api.Action
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.exception.RuleException
import io.bluetape4k.support.requireNotBlank
import org.codehaus.groovy.control.CompilerConfiguration

/**
 * Groovy 스크립트를 사용하는 [Action] 구현체입니다.
 *
 * Groovy의 동적 타이핑, 클로저, Java 호환 문법을 활용하여
 * 복잡한 액션 로직을 간결하게 표현할 수 있습니다.
 * 바이트코드로 컴파일되어 실행됩니다.
 *
 * ```kotlin
 * val action = GroovyAction("discount = true")
 * val facts = Facts.of("amount" to 1500)
 * action.execute(facts)
 * ```
 *
 * @property script Groovy 스크립트
 */
class GroovyAction(val script: String): Action {

    companion object: KLogging() {
        private val compilerConfig = CompilerConfiguration().apply {
            sourceEncoding = "UTF-8"
        }
    }

    init {
        script.requireNotBlank("script")
    }

    private val parsedScript by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        GroovyShell(compilerConfig).parse(script)
    }

    override fun execute(facts: Facts) {
        try {
            val binding = NullSafeBinding(facts.asMap())
            parsedScript.binding = binding
            parsedScript.run()
            // 스크립트에서 변경/추가한 변수를 Facts에 반영
            // GString → String 자동 변환으로 ClassCastException 방지
            binding.variables.forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                val converted = if (value is GString) value.toString() else value
                facts[key as String] = converted
            }
        } catch (e: Exception) {
            log.error(e) { "Fail to execute Groovy script '$script' on facts=$facts" }
            throw RuleException("Fail to execute Groovy script", e)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroovyAction) return false
        return script == other.script
    }

    override fun hashCode(): Int = script.hashCode()

    override fun toString(): String = "GroovyAction(script='$script')"
}
