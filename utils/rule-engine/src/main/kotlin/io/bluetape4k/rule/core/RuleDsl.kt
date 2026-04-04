package io.bluetape4k.rule.core

import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import io.bluetape4k.rule.api.Action
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.RuleEngineConfig
import io.bluetape4k.rule.api.SuspendAction
import io.bluetape4k.rule.api.SuspendCondition

/**
 * Rule DSL 마커 어노테이션
 */
@DslMarker
annotation class RuleDsl

/**
 * Rule DSL 빌더입니다.
 *
 * ```kotlin
 * val discountRule = rule {
 *     name = "discount"
 *     description = "1000원 이상 구매 시 할인 적용"
 *     priority = 1
 *     condition { facts -> facts.get<Int>("amount")!! > 1000 }
 *     action { facts -> facts["discount"] = true }
 * }
 * ```
 */
@RuleDsl
class RuleBuilder {
    var name: String = DEFAULT_RULE_NAME
    var description: String = DEFAULT_RULE_DESCRIPTION
    var priority: Int = DEFAULT_RULE_PRIORITY
    var condition: Condition = Condition.FALSE
    val actions: MutableList<Action> = mutableListOf()

    /**
     * [Condition]을 설정합니다.
     */
    fun condition(condition: Condition) {
        this.condition = condition
    }

    /**
     * 람다로 [Condition]을 설정합니다.
     */
    inline fun condition(crossinline evaluator: (Facts) -> Boolean) {
        this.condition = Condition { evaluator(it) }
    }

    /**
     * [Action]을 추가합니다.
     */
    fun action(action: Action) {
        this.actions.add(action)
    }

    /**
     * 람다로 [Action]을 추가합니다.
     */
    inline fun action(crossinline action: (Facts) -> Unit) {
        this.actions.add(Action { action(it) })
    }

    internal fun build(): DefaultRule =
        DefaultRule(name, description, priority, condition, actions)
}

/**
 * [DefaultRule]을 생성하는 DSL입니다.
 *
 * ```kotlin
 * val myRule = rule {
 *     name = "myRule"
 *     condition { facts -> true }
 *     action { facts -> facts["result"] = "done" }
 * }
 * ```
 */
fun rule(setup: RuleBuilder.() -> Unit): DefaultRule {
    return RuleBuilder().apply(setup).build()
}

/**
 * SuspendRule DSL 빌더입니다.
 *
 * ```kotlin
 * val asyncRule = suspendRule {
 *     name = "asyncRule"
 *     condition { facts -> facts.get<Int>("value")!! > 0 }
 *     action { facts -> facts["processed"] = true }
 * }
 * ```
 */
@RuleDsl
class SuspendRuleBuilder {
    var name: String = DEFAULT_RULE_NAME
    var description: String = DEFAULT_RULE_DESCRIPTION
    var priority: Int = DEFAULT_RULE_PRIORITY
    var condition: SuspendCondition = SuspendCondition.FALSE
    val actions: MutableList<SuspendAction> = mutableListOf()

    /**
     * [SuspendCondition]을 설정합니다.
     */
    fun condition(condition: SuspendCondition) {
        this.condition = condition
    }

    /**
     * 람다로 [SuspendCondition]을 설정합니다.
     */
    inline fun condition(crossinline evaluator: suspend (Facts) -> Boolean) {
        this.condition = SuspendCondition { evaluator(it) }
    }

    /**
     * [SuspendAction]을 추가합니다.
     */
    fun action(action: SuspendAction) {
        this.actions.add(action)
    }

    /**
     * 람다로 [SuspendAction]을 추가합니다.
     */
    inline fun action(crossinline action: suspend (Facts) -> Unit) {
        this.actions.add(SuspendAction { action(it) })
    }

    internal fun build(): DefaultSuspendRule =
        DefaultSuspendRule(name, description, priority, condition, actions)
}

/**
 * [DefaultSuspendRule]을 생성하는 DSL입니다.
 */
fun suspendRule(setup: SuspendRuleBuilder.() -> Unit): DefaultSuspendRule {
    return SuspendRuleBuilder().apply(setup).build()
}

/**
 * RuleEngine DSL 빌더입니다.
 *
 * ```kotlin
 * val engine = ruleEngine { skipOnFirstAppliedRule = true }
 * ```
 */
@RuleDsl
class RuleEngineBuilder {
    var skipOnFirstAppliedRule: Boolean = false
    var skipOnFirstFailedRule: Boolean = false
    var skipOnFirstNonTriggeredRule: Boolean = false
    var priorityThreshold: Int = RuleEngineConfig.DEFAULT_PRIORITY_THRESHOLD

    internal fun build(): DefaultRuleEngine {
        val config = RuleEngineConfig(
            skipOnFirstAppliedRule = skipOnFirstAppliedRule,
            skipOnFirstFailedRule = skipOnFirstFailedRule,
            skipOnFirstNonTriggeredRule = skipOnFirstNonTriggeredRule,
            priorityThreshold = priorityThreshold
        )
        return DefaultRuleEngine(config)
    }
}

/**
 * [DefaultRuleEngine]을 생성하는 DSL입니다.
 *
 * ```kotlin
 * val engine = ruleEngine {
 *     skipOnFirstAppliedRule = true
 *     priorityThreshold = 100
 * }
 * ```
 */
fun ruleEngine(setup: RuleEngineBuilder.() -> Unit): DefaultRuleEngine {
    return RuleEngineBuilder().apply(setup).build()
}
