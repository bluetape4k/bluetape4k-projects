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
        log.debug { "Fire suspend rules, ${facts.toLogContext()}" }

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

            val startedAt = System.nanoTime()
            log.debug { "Evaluate suspend rule. rule=$name, ${facts.toLogContext()}" }

            try {
                val evaluationResult = rule.evaluate(facts)
                log.debug {
                    "Suspend rule '$name' evaluated. result=$evaluationResult, " +
                            "durationNanos=${System.nanoTime() - startedAt}, ${facts.toLogContext()}"
                }

                if (evaluationResult) {
                    rule.execute(facts)
                    log.debug {
                        "Suspend rule '$name' executed successfully. " +
                                "durationNanos=${System.nanoTime() - startedAt}, ${facts.toLogContext()}"
                    }

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
                log.debug {
                    "Suspend rule '$name' failed. exceptionType=${e.javaClass.name}, " +
                            "durationNanos=${System.nanoTime() - startedAt}, ${facts.toLogContext()}"
                }
                if (config.skipOnFirstFailedRule) {
                    log.debug { "Remaining suspend rules skipped. (skipOnFirstFailedRule=true)" }
                    return
                }
            }
        }
    }

    override suspend fun check(rules: Iterable<SuspendRule>, facts: Facts): Map<SuspendRule, Boolean> {
        log.debug { "Checking suspend rules ... ${facts.toLogContext()}" }
        return rules.associateWith { rule ->
            val startedAt = System.nanoTime()
            try {
                val result = rule.evaluate(facts)
                log.debug {
                    "Suspend rule '${rule.name}' checked. result=$result, " +
                            "durationNanos=${System.nanoTime() - startedAt}, ${facts.toLogContext()}"
                }
                result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.debug {
                    "Suspend rule '${rule.name}' check failed. exceptionType=${e.javaClass.name}, " +
                            "durationNanos=${System.nanoTime() - startedAt}, ${facts.toLogContext()}"
                }
                false
            }
        }
    }
}
