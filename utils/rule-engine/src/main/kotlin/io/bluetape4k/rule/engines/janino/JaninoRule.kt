package io.bluetape4k.rule.engines.janino

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
 * Janino 컴파일러로 정의된 Rule입니다.
 *
 * Condition은 Java 표현식, Action은 Java 스크립트로 작성합니다.
 * 바이트코드로 컴파일되어 네이티브 속도로 실행됩니다.
 *
 * ```kotlin
 * val rule = JaninoRule(name = "discount")
 *     .whenever("((Integer)facts.get(\"amount\")).intValue() > 1000")
 *     .then("facts.put(\"discount\", true);")
 * ```
 */
class JaninoRule(
    name: String = DEFAULT_RULE_NAME,
    description: String = DEFAULT_RULE_DESCRIPTION,
    priority: Int = DEFAULT_RULE_PRIORITY,
): AbstractRule(name, description, priority) {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    private var condition: Condition = Condition.FALSE
    private val actions = LinkedList<JaninoAction>()

    /**
     * Janino Java 표현식으로 조건을 설정합니다.
     */
    fun whenever(conditionExpr: String) = apply {
        conditionExpr.requireNotBlank("conditionExpr")
        log.debug { "Set rule condition. condition=$conditionExpr" }
        this.condition = JaninoCondition(conditionExpr)
    }

    /**
     * [JaninoCondition]으로 조건을 설정합니다.
     */
    fun whenever(condition: JaninoCondition) = apply {
        log.debug { "Set rule condition. condition=$condition" }
        this.condition = condition
    }

    /**
     * Janino Java 스크립트로 액션을 추가합니다.
     */
    fun then(script: String) = apply {
        script.requireNotBlank("script")
        log.debug { "Add rule action. script=$script" }
        actions.add(JaninoAction(script))
    }

    /**
     * [JaninoAction]을 추가합니다.
     */
    fun then(action: JaninoAction) = apply {
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
