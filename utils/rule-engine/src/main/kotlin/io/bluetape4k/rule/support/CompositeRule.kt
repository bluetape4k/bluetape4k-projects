package io.bluetape4k.rule.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.Rule
import io.bluetape4k.rule.core.AbstractRule
import io.bluetape4k.rule.core.RuleProxy
import java.util.*

/**
 * 복수의 [Rule]을 조합하여 하나의 Rule로 표현할 수 있도록 하는 추상 클래스입니다.
 *
 * ```kotlin
 * val composite = UnitRuleGroup(name = "allMustPass")
 * composite.addRule(rule1)
 * composite.addRule(rule2)
 * // rule1과 rule2 모두 evaluate가 true여야 composite가 실행됩니다.
 * engine.fire(ruleSetOf(composite), facts)
 * ```
 */
abstract class CompositeRule(
    name: String = DEFAULT_RULE_NAME,
    description: String = DEFAULT_RULE_DESCRIPTION,
    priority: Int = DEFAULT_RULE_PRIORITY,
): AbstractRule(name, description, priority) {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    protected open val rules: MutableSet<Rule> = TreeSet()
    protected open val proxyRules: MutableMap<Any, Rule> = linkedMapOf()

    abstract override fun evaluate(facts: Facts): Boolean
    abstract override fun execute(facts: Facts)

    /**
     * Rule을 추가합니다. 어노테이션 기반 객체도 지원합니다.
     *
     * ```kotlin
     * val group = UnitRuleGroup("group")
     * group.addRule(rule1)
     * group.addRule(AnnotatedRule()) // 어노테이션 기반 객체도 지원
     * ```
     *
     * @param rule 추가할 Rule 또는 어노테이션 기반 객체
     */
    fun addRule(rule: Any) {
        log.debug { "Add rule to CompositeRule. $rule" }
        val proxy = RuleProxy.asRule(rule)
        rules.add(proxy)
        proxyRules[rule] = proxy
    }

    /**
     * Rule을 제거합니다.
     *
     * ```kotlin
     * val group = UnitRuleGroup("group")
     * group.addRule(rule1)
     * group.removeRule(rule1)
     * ```
     *
     * @param rule 제거할 Rule 또는 어노테이션 기반 객체
     */
    fun removeRule(rule: Any) {
        log.debug { "Remove rule from CompositeRule. $rule" }
        proxyRules[rule]?.let { proxy ->
            rules.remove(proxy)
            proxyRules.remove(rule)
        }
    }
}
