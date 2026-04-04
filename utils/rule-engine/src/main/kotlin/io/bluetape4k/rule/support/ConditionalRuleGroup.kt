package io.bluetape4k.rule.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.Rule
import io.bluetape4k.rule.exception.InvalidRuleDefinitionException
import java.util.*

/**
 * 우선순위가 가장 높은 Rule의 evaluation을 먼저 실행합니다.
 * 실패하면 모든 실행을 취소하고, 성공하��� 나머지 Rule 중 evaluation에 성공한 것들만 실행합니다.
 */
class ConditionalRuleGroup(
    name: String = DEFAULT_RULE_NAME,
    description: String = DEFAULT_RULE_DESCRIPTION,
    priority: Int = DEFAULT_RULE_PRIORITY,
): CompositeRule(name, description, priority) {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    private val successfulEvaluations: MutableSet<Rule> = TreeSet()
    private val conditionalRule: Rule by lazy { getRuleWithHighestPriority() }

    override fun evaluate(facts: Facts): Boolean {
        successfulEvaluations.clear()

        if (conditionalRule.evaluate(facts)) {
            rules.filter { it != conditionalRule && it.evaluate(facts) }
                .forEach { successfulEvaluations.add(it) }
            return true
        }
        return false
    }

    override fun execute(facts: Facts) {
        conditionalRule.execute(facts)
        successfulEvaluations.forEach { it.execute(facts) }
    }

    private fun getRuleWithHighestPriority(): Rule {
        val sorted = rules.toList().sortedBy { it.priority }
        val highest = sorted.first()

        log.debug { "sorted=${sorted.map { it.priority }.joinToString()}" }

        if (sorted.size > 1 && highest.priority == sorted[1].priority) {
            throw InvalidRuleDefinitionException("Only one rule can have highest priority.")
        }
        return highest
    }
}
