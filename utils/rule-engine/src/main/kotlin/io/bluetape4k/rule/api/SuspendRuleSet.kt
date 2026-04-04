package io.bluetape4k.rule.api

import java.util.*

/**
 * 복수의 [SuspendRule]이 우선순위 순서대로 실행될 수 있도록 정렬된 집합입니다.
 *
 * ```kotlin
 * val ruleSet = suspendRuleSetOf(rule1, rule2)
 * suspendEngine.fire(ruleSet, facts)
 * ```
 *
 * @see SuspendRule
 * @see SuspendRuleEngine
 */
class SuspendRuleSet(
    val rules: TreeSet<SuspendRule> = TreeSet(),
): Iterable<SuspendRule> {

    constructor(rules: Collection<SuspendRule>): this(TreeSet<SuspendRule>().apply { addAll(rules) })
    constructor(vararg rules: SuspendRule): this(TreeSet<SuspendRule>().apply { addAll(rules) })

    /**
     * 새로운 SuspendRule을 등록합니다.
     *
     * @param rule 등록할 SuspendRule
     */
    fun register(rule: SuspendRule) {
        rules.add(rule)
    }

    /**
     * SuspendRule을 제거합니다.
     *
     * @param rule 제거할 SuspendRule
     */
    fun unregister(rule: SuspendRule) {
        rules.remove(rule)
    }

    /**
     * 모든 SuspendRule을 제거합니다.
     */
    fun clear() {
        rules.clear()
    }

    val size: Int get() = rules.size

    fun isEmpty(): Boolean = rules.isEmpty()

    fun isNotEmpty(): Boolean = rules.isNotEmpty()

    override fun iterator(): Iterator<SuspendRule> = rules.iterator()

    override fun toString(): String = "SuspendRuleSet{${rules.joinToString { it.toString() }}}"
}

/**
 * [SuspendRuleSet]을 생성합니다.
 */
fun suspendRuleSetOf(vararg rules: SuspendRule): SuspendRuleSet = SuspendRuleSet(*rules)

/**
 * SuspendRule 컬렉션으로 [SuspendRuleSet]을 생성합니다.
 */
fun suspendRuleSetOf(rules: Collection<SuspendRule>): SuspendRuleSet = SuspendRuleSet(rules)
