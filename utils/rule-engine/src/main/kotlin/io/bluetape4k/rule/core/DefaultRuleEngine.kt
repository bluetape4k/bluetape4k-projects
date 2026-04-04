package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.Rule
import io.bluetape4k.rule.api.RuleEngine
import io.bluetape4k.rule.api.RuleEngineConfig
import io.bluetape4k.rule.api.RuleEngineListener
import io.bluetape4k.rule.api.RuleListener
import io.bluetape4k.rule.api.RuleSet
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 기본 Rule Engine 구현체입니다.
 *
 * ```kotlin
 * val engine = DefaultRuleEngine(RuleEngineConfig(skipOnFirstAppliedRule = true))
 * engine.fire(ruleSet, facts)
 * ```
 */
open class DefaultRuleEngine(
    override val config: RuleEngineConfig = RuleEngineConfig.DEFAULT,
): RuleEngine {

    companion object: KLogging()

    private val _ruleListeners = CopyOnWriteArrayList<RuleListener>(listOf(DefaultRuleListener()))
    private val _ruleEngineListeners = CopyOnWriteArrayList<RuleEngineListener>(listOf(DefaultRuleEngineListener(config)))

    override val ruleListeners: List<RuleListener>
        get() = _ruleListeners.toList()

    override val ruleEngineListeners: List<RuleEngineListener>
        get() = _ruleEngineListeners.toList()

    /**
     * [RuleListener]를 등록합니다.
     */
    fun registerRuleListener(listener: RuleListener) {
        _ruleListeners.add(listener)
    }

    /**
     * [RuleListener]들을 등록합니다.
     */
    fun registerRuleListeners(listeners: Iterable<RuleListener>) {
        _ruleListeners.addAll(listeners)
    }

    /**
     * 모든 [RuleListener]를 제거합니다.
     */
    fun clearRuleListeners() {
        _ruleListeners.clear()
    }

    /**
     * [RuleEngineListener]를 등록합니다.
     */
    fun registerRuleEngineListener(listener: RuleEngineListener) {
        _ruleEngineListeners.add(listener)
    }

    /**
     * [RuleEngineListener]들을 등록합니다.
     */
    fun registerRuleEngineListeners(listeners: Iterable<RuleEngineListener>) {
        _ruleEngineListeners.addAll(listeners)
    }

    /**
     * 모든 [RuleEngineListener]를 제거합니다.
     */
    fun clearEngineListeners() {
        _ruleEngineListeners.clear()
    }

    override fun fire(rules: RuleSet, facts: Facts) {
        onBeforeRules(rules, facts)
        doFire(rules, facts)
        onAfterRules(rules, facts)
    }

    internal open fun doFire(rules: RuleSet, facts: Facts) {
        log.debug { "Fire rules=$rules, facts=$facts" }

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

            if (shouldBeEvaluated(rule, facts)) {
                log.debug { "Evaluate rule. rule=$rule, facts=$facts" }

                if (rule.evaluate(facts)) {
                    onAfterEvaluate(rule, facts, true)

                    try {
                        onBeforeExecute(rule, facts)
                        rule.execute(facts)
                        onAfterExecute(rule, facts)

                        if (config.skipOnFirstAppliedRule) {
                            log.debug { "나머지 Rule들은 무시됩니다. (skipOnFirstAppliedRule=true)" }
                            return
                        }
                    } catch (e: Exception) {
                        onAfterExecute(rule, facts, e)
                        if (config.skipOnFirstFailedRule) {
                            log.debug { "나머지 Rule들은 무시됩니다. (skipOnFirstFailedRule=true)" }
                            return
                        }
                    }
                } else {
                    onAfterEvaluate(rule, facts, false)
                    if (config.skipOnFirstNonTriggeredRule) {
                        log.debug { "나머지 Rule들은 무시됩니다. (skipOnFirstNonTriggeredRule=true)" }
                        return
                    }
                }
            } else {
                log.debug { "Rule '$name' has been skipped before evaluated." }
            }
        }
    }

    override fun check(rules: RuleSet, facts: Facts): Map<Rule, Boolean> {
        onBeforeRules(rules, facts)
        val result = doCheck(rules, facts)
        onAfterRules(rules, facts)
        return result
    }

    protected open fun doCheck(rules: RuleSet, facts: Facts): Map<Rule, Boolean> {
        log.debug { "Checking rules ..." }

        return rules
            .filter { shouldBeEvaluated(it, facts) }
            .associateWith { it.evaluate(facts) }
    }

    private fun shouldBeEvaluated(rule: Rule, facts: Facts): Boolean {
        return onBeforeEvaluate(rule, facts)
    }

    private fun onBeforeRules(rules: RuleSet, facts: Facts) {
        _ruleEngineListeners.forEach { listener ->
            runCatching { listener.beforeEvaluate(rules, facts) }
        }
    }

    private fun onAfterRules(rules: RuleSet, facts: Facts) {
        _ruleEngineListeners.forEach { listener ->
            runCatching { listener.afterExecute(rules, facts) }
        }
    }

    private fun onBeforeEvaluate(rule: Rule, facts: Facts): Boolean {
        return _ruleListeners.all { it.beforeEvaluate(rule, facts) }
    }

    private fun onAfterEvaluate(rule: Rule, facts: Facts, evaluationResult: Boolean) {
        _ruleListeners.forEach { it.afterEvaluate(rule, facts, evaluationResult) }
    }

    private fun onBeforeExecute(rule: Rule, facts: Facts) {
        _ruleListeners.forEach { it.beforeExecute(rule, facts) }
    }

    private fun onAfterExecute(rule: Rule, facts: Facts, exception: Throwable? = null) {
        _ruleListeners.forEach { it.afterExecute(rule, facts, exception) }
    }
}
