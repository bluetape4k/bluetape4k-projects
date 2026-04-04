package io.bluetape4k.rule.engines.kotlinscript

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.core.AbstractRule
import io.bluetape4k.support.requireNotBlank
import java.util.*

/**
 * Kotlin Script로 정의된 Rule입니다.
 *
 * ```kotlin
 * val rule = KotlinScriptRule(name = "calc")
 *     .whenever("val amount = bindings[\"amount\"] as Int; amount > 1000")
 *     .then("bindings[\"discount\"] = true")
 * ```
 */
class KotlinScriptRule(
    name: String = DEFAULT_RULE_NAME,
    description: String = DEFAULT_RULE_DESCRIPTION,
    priority: Int = DEFAULT_RULE_PRIORITY,
): AbstractRule(name, description, priority) {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    private var condition: Condition = Condition.FALSE
    private val actions = LinkedList<KotlinScriptAction>()

    /**
     * Kotlin 스크립트 표현식으로 조건을 설정합니다.
     */
    fun whenever(conditionExpr: String) = apply {
        log.debug { "Set rule condition. condition=$conditionExpr" }
        this.condition = KotlinScriptCondition(conditionExpr)
    }

    /**
     * [KotlinScriptCondition]으로 조건을 설정합니다.
     */
    fun whenever(condition: KotlinScriptCondition) = apply {
        log.debug { "Set rule condition. condition=$condition" }
        this.condition = condition
    }

    /**
     * Kotlin 스크립트 표현식으로 액션을 추가합니다.
     */
    fun then(actionExpr: String) = apply {
        actionExpr.requireNotBlank("actionExpr")
        log.debug { "Add rule action. action=$actionExpr" }
        actions.add(KotlinScriptAction(actionExpr))
    }

    /**
     * [KotlinScriptAction]을 추가합니다.
     */
    fun then(action: KotlinScriptAction) = apply {
        log.debug { "Add rule action. action=$action" }
        actions.add(action)
    }

    override fun evaluate(facts: Facts): Boolean {
        log.debug { "Evaluate condition '$condition' with facts=$facts" }
        return condition.evaluate(facts)
    }

    override fun execute(facts: Facts) {
        log.debug { "Execute actions with facts=$facts" }
        actions.forEach { action ->
            log.debug { "Execute action '$action' with facts=$facts" }
            action.execute(facts)
        }
    }
}
