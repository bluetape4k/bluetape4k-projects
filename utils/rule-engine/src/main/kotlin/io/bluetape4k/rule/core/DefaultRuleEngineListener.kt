package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.Rule
import io.bluetape4k.rule.api.RuleEngineConfig
import io.bluetape4k.rule.api.RuleEngineListener

/**
 * 기본 [RuleEngineListener] 구현체입니다. 로그를 기록합니다.
 */
class DefaultRuleEngineListener(
    val config: RuleEngineConfig = RuleEngineConfig.DEFAULT,
): RuleEngineListener {

    companion object: KLogging()

    override fun beforeEvaluate(rules: Iterable<Rule>, facts: Facts) {
        if (!rules.iterator().hasNext()) {
            log.warn { "No rules registered! Nothing to apply." }
        } else {
            log.debug { "config=$config, rules=$rules, facts=$facts, Ruleset evaluation started ..." }
        }
    }

    override fun afterExecute(rules: Iterable<Rule>, facts: Facts) {
        log.debug { "RuleSet executed. rules=$rules, facts=$facts" }
    }
}
