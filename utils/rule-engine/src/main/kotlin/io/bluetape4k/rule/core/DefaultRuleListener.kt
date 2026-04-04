package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.Rule
import io.bluetape4k.rule.api.RuleListener

/**
 * 기본 [RuleListener] 구현체입니다. 로그를 기록합니다.
 */
class DefaultRuleListener: RuleListener {

    companion object: KLogging()

    override fun beforeEvaluate(rule: Rule, facts: Facts): Boolean {
        log.debug { "Before evaluate ... rule=${rule.name}" }
        return true
    }

    override fun afterEvaluate(rule: Rule, facts: Facts, evaluationResult: Boolean) {
        if (evaluationResult) {
            log.debug { "Rule '${rule.name}' triggered" }
        } else {
            log.debug { "Rule '${rule.name}' has been evaluated to false, it has not been executed." }
        }
    }

    override fun beforeExecute(rule: Rule, facts: Facts) {
        log.debug { "Before execute ... rule=${rule.name}, facts=$facts" }
    }

    override fun afterExecute(rule: Rule, facts: Facts, exception: Throwable?) {
        if (exception == null) {
            log.debug { "Rule '${rule.name}' performed successfully." }
        } else {
            log.warn(exception) { "Rule '${rule.name}' performed with exception." }
        }
    }
}
