package io.bluetape4k.rule.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.Rule

/**
 * evaluation이 성공한 첫 번째 Rule만 실행하고, 나머지 Rule들은 무시합니다.
 */
class ActivationRuleGroup(
    name: String = DEFAULT_RULE_NAME,
    description: String = DEFAULT_RULE_DESCRIPTION,
    priority: Int = DEFAULT_RULE_PRIORITY,
): CompositeRule(name, description, priority) {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    private var selectedRule: Rule? = null

    override fun evaluate(facts: Facts): Boolean {
        selectedRule = rules.firstOrNull { it.evaluate(facts) }
        if (selectedRule != null) {
            log.debug { "Find successfully evaluated rule. selectedRule=$selectedRule" }
        }
        return selectedRule != null
    }

    override fun execute(facts: Facts) {
        selectedRule?.run {
            log.debug { "Execute selected rule... rule=$this, facts=$facts" }
            execute(facts)
        }
    }
}
