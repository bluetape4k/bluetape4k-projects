package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.Rule
import io.bluetape4k.rule.api.RuleListener

/**
 * 기본 [RuleListener] 구현체입니다. 로그를 기록합니다.
 *
 * ```kotlin
 * val engine = DefaultRuleEngine()
 * // DefaultRuleListener는 DefaultRuleEngine 생성 시 자동으로 등록됩니다.
 * // 추가 리스너가 필요하면 registerRuleListener()를 사용하세요.
 * val listener = DefaultRuleListener()
 * engine.registerRuleListener(listener)
 * ```
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
