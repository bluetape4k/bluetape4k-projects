package io.bluetape4k.rule.api

/**
 * 코루틴 기반의 Rule Engine 인터페이스입니다.
 *
 * [SuspendRule]을 비동기로 평가하고 실행합니다.
 *
 * ```kotlin
 * val engine = DefaultSuspendRuleEngine()
 * val result = engine.check(suspendRules, facts)
 * engine.fire(suspendRules, facts)
 * ```
 *
 * @see SuspendRule
 * @see SuspendRuleSet
 */
interface SuspendRuleEngine {

    /**
     * Rule Engine 환경 설정 정보
     */
    val config: RuleEngineConfig

    /**
     * [SuspendRule]들이 조건에 맞는 것인지 비동기로 판단합니다.
     *
     * @param rules 검사할 SuspendRule 컬렉션
     * @param facts 실행에 필요한 데이터
     * @return 각 Rule의 평가 결과 Map
     */
    suspend fun check(rules: Iterable<SuspendRule>, facts: Facts): Map<SuspendRule, Boolean>

    /**
     * [SuspendRule]들을 비동기로 실행합니다.
     *
     * @param rules 실행할 SuspendRule 컬렉션
     * @param facts 실행에 필요한 데이터
     */
    suspend fun fire(rules: Iterable<SuspendRule>, facts: Facts)
}
