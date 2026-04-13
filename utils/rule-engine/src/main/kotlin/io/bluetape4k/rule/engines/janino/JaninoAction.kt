package io.bluetape4k.rule.engines.janino

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.rule.api.Action
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.exception.RuleException
import io.bluetape4k.support.requireNotBlank
import org.codehaus.janino.ScriptEvaluator

/**
 * Janino 컴파일러를 사용하는 [Action] 구현체입니다.
 *
 * Java 스크립트를 바이트코드로 컴파일하여 네이티브 속도로 실행합니다.
 * 대량의 룰을 반복 실행할 때 인터프리터 기반 엔진 대비 우수한 성능을 제공합니다.
 *
 * ```kotlin
 * val action = JaninoAction("facts.put(\"discount\", true);")
 * val facts = Facts.of("amount" to 1500)
 * action.execute(facts)
 * ```
 *
 * @property script Janino Java 스크립트 (문장 블록)
 */
class JaninoAction(val script: String): Action {

    companion object: KLogging()

    init {
        script.requireNotBlank("script")
    }

    private val evaluator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ScriptEvaluator().apply {
            setParameters(arrayOf("facts"), arrayOf(MutableMap::class.java))
            cook(script)
        }
    }

    override fun execute(facts: Facts) {
        try {
            // Facts.asMap()은 읽기 전용이므로 mutable copy를 전달하고 결과를 다시 반영
            val mutableMap = facts.asMap().toMutableMap()
            evaluator.evaluate(arrayOf<Any?>(mutableMap))
            // 스크립트에서 변경한 값을 Facts에 반영
            mutableMap.forEach { (key, value) ->
                facts[key] = value
            }
        } catch (e: Exception) {
            log.error(e) { "Fail to execute Janino script '$script' on facts=$facts" }
            throw RuleException("Fail to execute Janino script", e)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JaninoAction) return false
        return script == other.script
    }

    override fun hashCode(): Int = script.hashCode()

    override fun toString(): String = "JaninoAction(script='$script')"
}
