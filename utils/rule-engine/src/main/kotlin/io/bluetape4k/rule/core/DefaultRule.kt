package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import io.bluetape4k.rule.api.Action
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts

/**
 * 기본 [Condition]과 [Action] 목록을 가지는 Rule 구현체입니다.
 *
 * @property condition 조건
 * @property actions 액션 목록
 */
open class DefaultRule(
    name: String = DEFAULT_RULE_NAME,
    description: String = DEFAULT_RULE_DESCRIPTION,
    priority: Int = DEFAULT_RULE_PRIORITY,
    val condition: Condition = Condition.FALSE,
    val actions: List<Action> = emptyList(),
): AbstractRule(name, description, priority) {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    override fun evaluate(facts: Facts): Boolean {
        return condition.evaluate(facts)
    }

    override fun execute(facts: Facts) {
        actions.forEach { action -> action.execute(facts) }
    }
}
