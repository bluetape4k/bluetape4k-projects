package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.RuleEngineConfig
import io.bluetape4k.rule.api.SuspendRule
import io.bluetape4k.rule.api.SuspendRuleEngine
import kotlin.coroutines.cancellation.CancellationException

/**
 * 코루틴 기반의 기본 SuspendRuleEngine 구현체입니다.
 *
 * ```kotlin
 * val engine = DefaultSuspendRuleEngine(RuleEngineConfig(skipOnFirstAppliedRule = true))
 * engine.fire(suspendRuleSet, facts)
 * ```
 */
open class DefaultSuspendRuleEngine(
    override val config: RuleEngineConfig = RuleEngineConfig.DEFAULT,
): SuspendRuleEngine {

    companion object: KLogging()

    override suspend fun fire(rules: Iterable<SuspendRule>, facts: Facts) {
        log.debug { "Fire suspend rules, facts=$facts" }

        for (rule in rules) {
            val name = rule.name
            val priority = rule.priority

            if (priority > config.priorityThreshold) {
                log.info {
                    "Rule priority threshold[${config.priorityThreshold}] exceeded at rule '$name' " +
                            "with priority=$priority, next rules will be skipped."
                }
                return
            }

            log.debug { "Evaluate suspend rule. rule=$name, facts=$facts" }

            try {
                if (rule.evaluate(facts)) {
                    rule.execute(facts)
                    log.debug { "Suspend rule '$name' executed successfully." }

                    if (config.skipOnFirstAppliedRule) {
                        log.debug { "Remaining suspend rules skipped. (skipOnFirstAppliedRule=true)" }
                        return
                    }
                } else {
                    if (config.skipOnFirstNonTriggeredRule) {
                        log.debug { "Remaining suspend rules skipped. (skipOnFirstNonTriggeredRule=true)" }
                        return
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.debug { "Suspend rule '$name' failed with exception: ${e.message}" }
                if (config.skipOnFirstFailedRule) {
                    log.debug { "Remaining suspend rules skipped. (skipOnFirstFailedRule=true)" }
                    return
                }
            }
        }
    }

    override suspend fun check(rules: Iterable<SuspendRule>, facts: Facts): Map<SuspendRule, Boolean> {
        log.debug { "Checking suspend rules ..." }
        return rules.associateWith { rule ->
            try {
                rule.evaluate(facts)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.debug { "Suspend rule '${rule.name}' evaluate failed with exception: ${e.message}" }
                false
            }
        }
    }
}
