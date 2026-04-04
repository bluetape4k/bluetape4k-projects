package io.bluetape4k.rule.engines.mvel2

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
 * MVEL2 스크립트로 정의된 Rule입니다.
 *
 * ```kotlin
 * val rule = MvelRule(name = "discount")
 *     .whenever("amount > 1000")
 *     .then("discount = true")
 * ```
 */
class MvelRule(
    name: String = DEFAULT_RULE_NAME,
    description: String = DEFAULT_RULE_DESCRIPTION,
    priority: Int = DEFAULT_RULE_PRIORITY,
): AbstractRule(name, description, priority) {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    private var condition: Condition = Condition.FALSE
    private val actions = LinkedList<MvelAction>()

    /**
     * MVEL2 표현식으로 조건을 설정합니다.
     */
    fun whenever(conditionExpr: String) = apply {
        log.debug { "Set rule condition. condition=$conditionExpr" }
        this.condition = MvelCondition(conditionExpr)
    }

    /**
     * [MvelCondition]으로 조건을 설정합니다.
     */
    fun whenever(condition: MvelCondition) = apply {
        log.debug { "Set rule condition. condition=$condition" }
        this.condition = condition
    }

    /**
     * MVEL2 표현식으로 액션을 추가합니다.
     */
    fun then(actionExpr: String) = apply {
        actionExpr.requireNotBlank("actionExpr")
        log.debug { "Add rule action. action=$actionExpr" }
        actions.add(MvelAction(actionExpr))
    }

    /**
     * [MvelAction]을 추가합니다.
     */
    fun then(action: MvelAction) = apply {
        log.debug { "Add rule action. action=$action" }
        actions.add(action)
    }

    override fun evaluate(facts: Facts): Boolean {
        log.debug { "Evaluate condition '$condition' with facts=$facts" }
        return condition.evaluate(facts)
    }

    override fun execute(facts: Facts) {
        actions.forEach { action ->
            log.debug { "Execute action '$action' with facts=$facts" }
            action.execute(facts)
        }
    }
}
