package io.bluetape4k.rule.api

/**
 * 코루틴 기반의 Rule 인터페이스입니다.
 *
 * 비동기 조건 평가와 실행을 지원합니다.
 *
 * ```kotlin
 * class MySuspendRule : SuspendRule {
 *     override val name = "asyncRule"
 *     override val description = "비동기 규칙"
 *     override val priority = 1
 *     override suspend fun evaluate(facts: Facts) = true
 *     override suspend fun execute(facts: Facts) { delay(100) }
 * }
 * ```
 *
 * @see SuspendRuleEngine
 */
interface SuspendRule: Comparable<SuspendRule> {

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
     * 해당 룰이 실행되어야 하는지 비동기로 판단합니다.
     *
     * @param facts Rule 실행에 필요한 데이터
     * @return 실행 가능 여부
     */
    suspend fun evaluate(facts: Facts): Boolean

    /**
     * evaluate가 true일 때 비동기로 실행되는 작업입니다.
     *
     * @param facts Rule 실행에 필요한 데이터
     */
    suspend fun execute(facts: Facts)

    override fun compareTo(other: SuspendRule): Int {
        val priorityComparison = this.priority.compareTo(other.priority)
        return if (priorityComparison != 0) priorityComparison else this.name.compareTo(other.name)
    }
}
