package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.Rule
import io.bluetape4k.rule.api.RuleEngine
import io.bluetape4k.rule.api.RuleEngineConfig
import io.bluetape4k.rule.api.RuleEngineListener
import io.bluetape4k.rule.api.RuleListener
import io.bluetape4k.rule.api.RuleSet
import java.util.*

/**
 * Forward Chaining 방식의 Rule Engine입니다.
 * evaluate에 성공한 Rule만 반복적으로 실행합니다.
 *
 * ```kotlin
 * val engine = InferenceRuleEngine()
 * engine.fire(ruleSet, facts) // 조건 만족하는 룰이 없을 때까지 반복
 * ```
 */
class InferenceRuleEngine(
    override val config: RuleEngineConfig = RuleEngineConfig.DEFAULT,
): RuleEngine {

    companion object: KLogging()

    private val delegate = DefaultRuleEngine(config)

    override val ruleListeners: List<RuleListener>
        get() = delegate.ruleListeners

    override val ruleEngineListeners: List<RuleEngineListener>
        get() = delegate.ruleEngineListeners

    /**
     * [RuleListener]를 등록합니다.
     */
    fun registerRuleListener(listener: RuleListener) {
        delegate.registerRuleListener(listener)
    }

    /**
     * [RuleEngineListener]를 등록합니다.
     */
    fun registerRuleEngineListener(listener: RuleEngineListener) {
        delegate.registerRuleEngineListener(listener)
    }

    override fun check(rules: RuleSet, facts: Facts): Map<Rule, Boolean> {
        return delegate.check(rules, facts)
    }

    override fun fire(rules: RuleSet, facts: Facts) {
        var selectedRules: Set<Rule>

        do {
            log.debug { "Select candidate rules based on the following facts=$facts" }
            selectedRules = selectCandidates(rules, facts)

            if (selectedRules.isNotEmpty()) {
                delegate.doFire(RuleSet(selectedRules), facts)
            } else {
                log.debug { "No candidate rules found for facts=$facts" }
            }
        } while (selectedRules.isNotEmpty())
    }

    /**
     * evaluate가 성공한 Rule만 반환합니다.
     */
    private fun selectCandidates(rules: RuleSet, facts: Facts): Set<Rule> {
        return TreeSet<Rule>().apply {
            addAll(rules.filter { it.evaluate(facts) })
        }
    }
}
