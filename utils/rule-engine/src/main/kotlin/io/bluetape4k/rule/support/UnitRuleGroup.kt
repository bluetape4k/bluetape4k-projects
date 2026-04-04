package io.bluetape4k.rule.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import io.bluetape4k.rule.api.Facts

/**
 * 소속된 모든 Rule에 대해 evaluate가 모두 성공해야 모든 action을 수행하고,
 * 하나라도 evaluation에 실패하면 action을 수행하지 않습니다.
 *
 * ```kotlin
 * val rule1 = rule { name = "r1"; condition { facts -> facts.get<Int>("age")!! >= 18 }; action { facts -> facts["adult"] = true } }
 * val rule2 = rule { name = "r2"; condition { facts -> facts.get<Boolean>("active") == true }; action { facts -> facts["granted"] = true } }
 *
 * val group = UnitRuleGroup(name = "allRequired")
 * group.addRule(rule1)
 * group.addRule(rule2)
 * // rule1과 rule2 모두 evaluate=true 일 때만 두 action 모두 실행됩니다.
 * engine.fire(ruleSetOf(group), facts)
 * ```
 */
open class UnitRuleGroup(
    name: String = DEFAULT_RULE_NAME,
    description: String = DEFAULT_RULE_DESCRIPTION,
    priority: Int = DEFAULT_RULE_PRIORITY,
): CompositeRule(name, description, priority) {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    override fun evaluate(facts: Facts): Boolean {
        return rules.isNotEmpty() && rules.all { it.evaluate(facts) }
    }

    override fun execute(facts: Facts) {
        rules.forEach { it.execute(facts) }
    }
}
