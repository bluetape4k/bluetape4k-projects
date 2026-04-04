package io.bluetape4k.rule.engines.kotlinscript

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.rule.api.Action
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.exception.RuleException
import io.bluetape4k.support.requireNotBlank

/**
 * Kotlin Script를 이용한 [Action] 구현체입니다.
 *
 * ```kotlin
 * val action = KotlinScriptAction("bindings[\"discount\"] = true")
 * val facts = Facts.of("amount" to 1500)
 * action.execute(facts)
 * facts.get<Boolean>("discount") // true
 * ```
 *
 * @property script Kotlin 스크립트 코드
 */
class KotlinScriptAction private constructor(val script: String): Action {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(script: String): KotlinScriptAction {
            script.requireNotBlank("script")
            return KotlinScriptAction(script)
        }
    }

    override fun execute(facts: Facts) {
        try {
            KotlinScriptEngine.evaluate(script, facts.asMap())
        } catch (e: Exception) {
            log.error(e) { "Unable to execute kotlin script '$script' on facts=$facts" }
            throw RuleException("Fail to execute kotlin script '$script' on facts=$facts", e)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KotlinScriptAction) return false
        return script == other.script
    }

    override fun hashCode(): Int = script.hashCode()

    override fun toString(): String = "KotlinScriptAction(script='$script')"
}
