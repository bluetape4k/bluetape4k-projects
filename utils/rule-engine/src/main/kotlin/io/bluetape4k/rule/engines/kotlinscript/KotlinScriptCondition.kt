package io.bluetape4k.rule.engines.kotlinscript

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts

/**
 * Kotlin Script를 이용한 [Condition] 구현체입니다.
 *
 * ```kotlin
 * val condition = KotlinScriptCondition(
 *     "val amount = bindings[\"amount\"] as Int; amount > 1000"
 * )
 * val facts = Facts.of("amount" to 1500)
 * condition.evaluate(facts) // true
 * ```
 *
 * @property script Kotlin 스크립트 코드
 */
class KotlinScriptCondition(val script: String): Condition {

    companion object: KLogging()

    override fun evaluate(facts: Facts): Boolean {
        if (script.isBlank()) return true

        return try {
            KotlinScriptEngine.evaluate(script, facts.asMap()) as? Boolean ?: false
        } catch (e: Exception) {
            log.warn(e) { "Unable to evaluate kotlin script '$script' on facts=$facts" }
            false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KotlinScriptCondition) return false
        return script == other.script
    }

    override fun hashCode(): Int = script.hashCode()

    override fun toString(): String = "KotlinScriptCondition(script='$script')"
}
