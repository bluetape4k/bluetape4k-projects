package io.bluetape4k.rule.api

import io.bluetape4k.rule.core.RuleProxy
import java.io.Serializable
import java.util.*

/**
 * 복수의 [Rule]이 우선순위 순서대로 실행될 수 있도록 정렬된 집합입니다.
 *
 * ```kotlin
 * val ruleSet = ruleSetOf(rule1, rule2, rule3)
 * engine.fire(ruleSet, facts)
 * ```
 *
 * @see Rule
 * @see RuleEngine
 */
open class RuleSet(
    val rules: TreeSet<Rule> = TreeSet(),
): Iterable<Rule>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

    constructor(rules: Set<Rule>): this(TreeSet<Rule>().apply { addAll(rules) })
    constructor(vararg rules: Rule): this(TreeSet<Rule>().apply { addAll(rules) })

    /**
     * 새로운 Rule을 등록합니다.
     *
     * ```kotlin
     * val ruleSet = RuleSet()
     * ruleSet.register(myRule)
     * ruleSet.size // 1
     * ```
     *
     * @param rule 등록할 Rule
     */
    fun register(rule: Rule) {
        rules.add(rule)
    }

    /**
     * 어노테이션 기반 객체를 Rule로 변환하여 등록합니다.
     *
     * ```kotlin
     * val ruleSet = RuleSet()
     * ruleSet.registerProxy(AgeCheckRule())
     * ruleSet.size // 1
     * ```
     *
     * @param ruleObject @RuleDefinition 어노테이션이 적용된 객체
     */
    fun registerProxy(ruleObject: Any) {
        rules.add(RuleProxy.asRule(ruleObject))
    }

    /**
     * Rule을 제거합니다.
     *
     * ```kotlin
     * val ruleSet = ruleSetOf(rule1, rule2)
     * ruleSet.unregister(rule1)
     * ruleSet.size // 1
     * ```
     *
     * @param rule 제거할 Rule
     */
    fun unregister(rule: Rule) {
        rules.remove(rule)
    }

    /**
     * 이름으로 Rule을 제거합니다.
     *
     * ```kotlin
     * val ruleSet = ruleSetOf(rule1, rule2)
     * ruleSet.unregister("myRule")
     * ```
     *
     * @param ruleName 제거할 Rule 이름 (대소문자 무시)
     */
    fun unregister(ruleName: String) {
        rules.find { ruleName.equals(it.name, ignoreCase = true) }?.let { rules.remove(it) }
    }

    /**
     * 모든 Rule을 제거합니다.
     *
     * ```kotlin
     * val ruleSet = ruleSetOf(rule1, rule2)
     * ruleSet.clear()
     * ruleSet.isEmpty() // true
     * ```
     */
    fun clear() {
        rules.clear()
    }

    /**
     * 포함된 Rule의 개수를 반환합니다.
     *
     * ```kotlin
     * val ruleSet = ruleSetOf(rule1, rule2)
     * ruleSet.size // 2
     * ```
     */
    val size: Int get() = rules.size

    /**
     * Rule이 비어있는지 확인합니다.
     *
     * ```kotlin
     * RuleSet().isEmpty() // true
     * ruleSetOf(rule1).isEmpty() // false
     * ```
     */
    fun isEmpty(): Boolean = rules.isEmpty()

    /**
     * Rule이 비어있지 않은지 확인합니다.
     *
     * ```kotlin
     * ruleSetOf(rule1).isNotEmpty() // true
     * RuleSet().isNotEmpty() // false
     * ```
     */
    fun isNotEmpty(): Boolean = rules.isNotEmpty()

    override fun iterator(): Iterator<Rule> = rules.iterator()

    override fun toString(): String = "RuleSet{${rules.joinToString { it.toString() }}}"
}

/**
 * [RuleSet]을 생성합니다.
 *
 * ```kotlin
 * val ruleSet = ruleSetOf(rule1, rule2, rule3)
 * engine.fire(ruleSet, facts)
 * ```
 */
fun ruleSetOf(vararg rules: Rule): RuleSet = RuleSet(*rules)

/**
 * Rule 컬렉션으로 [RuleSet]을 생성합니다.
 *
 * ```kotlin
 * val rules = listOf(rule1, rule2)
 * val ruleSet = ruleSetOf(rules)
 * engine.fire(ruleSet, facts)
 * ```
 */
fun ruleSetOf(rules: Collection<Rule>): RuleSet = RuleSet(rules.toSet())
