package io.bluetape4k.rule.engines.groovy

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
 * Groovy 스크립트로 정의된 Rule입니다.
 *
 * Groovy의 동적 타이핑과 클로저를 활용하여 복잡한 룰 로직을
 * Java 호환 문법으로 간결하게 작성할 수 있습니다.
 *
 * ```kotlin
 * val rule = GroovyRule(name = "discount")
 *     .whenever("amount > 1000")
 *     .then("discount = true")
 * ```
 */
class GroovyRule(
    name: String = DEFAULT_RULE_NAME,
    description: String = DEFAULT_RULE_DESCRIPTION,
    priority: Int = DEFAULT_RULE_PRIORITY,
): AbstractRule(name, description, priority) {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    private var condition: Condition = Condition.FALSE
    private val actions = LinkedList<GroovyAction>()

    /**
     * Groovy 표현식으로 조건을 설정합니다.
     */
    fun whenever(conditionExpr: String) = apply {
        conditionExpr.requireNotBlank("conditionExpr")
        log.debug { "Set rule condition. condition=$conditionExpr" }
        this.condition = GroovyCondition(conditionExpr)
    }

    /**
     * [GroovyCondition]으로 조건을 설정합니다.
     */
    fun whenever(condition: GroovyCondition) = apply {
        log.debug { "Set rule condition. condition=$condition" }
        this.condition = condition
    }

    /**
     * Groovy 스크립트로 액션을 추가합니다.
     */
    fun then(script: String) = apply {
        script.requireNotBlank("script")
        log.debug { "Add rule action. script=$script" }
        actions.add(GroovyAction(script))
    }

    /**
     * [GroovyAction]을 추가합니다.
     */
    fun then(action: GroovyAction) = apply {
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
