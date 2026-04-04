package io.bluetape4k.rule.api

import java.io.Serializable

/**
 * Rule의 기본 인터페이스입니다.
 *
 * Rule은 이름, 설명, 우선순위를 가지며, 조건(evaluate)과 실행(execute) 메서드를 제공합니다.
 * 우선순위가 낮을수록(숫자가 작을수록) 먼저 실행됩니다.
 *
 * ```kotlin
 * class MyRule : Rule {
 *     override val name = "myRule"
 *     override val description = "나의 규칙"
 *     override val priority = 1
 *     override fun evaluate(facts: Facts) = facts.get<Int>("age")!! >= 18
 *     override fun execute(facts: Facts) { facts["allowed"] = true }
 * }
 * ```
 *
 * @see Facts
 * @see RuleEngine
 */
interface Rule: Comparable<Rule>, Serializable {

    /**
     * Rule 이름
     */
    val name: String

    /**
     * Rule 설명
     */
    val description: String

    /**
     * Rule 우선순위 (낮을수록 먼저 실행)
     */
    val priority: Int

    /**
     * 해당 룰이 실행되어야 하는지 판단합니다.
     * true를 반환하면 [execute]를 호출하고, false이면 실행하지 않습니다.
     *
     * @param facts Rule 실행에 필요한 데이터
     * @return 실행 가능 여부
     */
    fun evaluate(facts: Facts): Boolean

    /**
     * evaluate가 true일 때 실행되는 작업입니다.
     *
     * @param facts Rule 실행에 필요한 데이터
     */
    fun execute(facts: Facts)

    override fun compareTo(other: Rule): Int {
        val priorityComparison = this.priority.compareTo(other.priority)
        return if (priorityComparison != 0) priorityComparison else this.name.compareTo(other.name)
    }
}
