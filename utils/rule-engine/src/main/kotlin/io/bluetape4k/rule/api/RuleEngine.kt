package io.bluetape4k.rule.api

/**
 * 다양한 Rule을 실행하는 Rule Engine의 기본 인터페이스입니다.
 *
 * ```kotlin
 * val engine = DefaultRuleEngine()
 * val result = engine.check(ruleSet, facts)
 * engine.fire(ruleSet, facts)
 * ```
 *
 * @see Rule
 * @see RuleSet
 * @see Facts
 */
interface RuleEngine {

    /**
     * Rule Engine 환경 설정 정보
     */
    val config: RuleEngineConfig

    /**
     * 각 Rule의 실행 이벤트를 받는 [RuleListener] 목록
     */
    val ruleListeners: List<RuleListener>

    /**
     * Rule Engine의 실행 이벤트를 받는 [RuleEngineListener] 목록
     */
    val ruleEngineListeners: List<RuleEngineListener>

    /**
     * [rules]의 Rule들이 조건에 맞는 것인지 판단합니다.
     *
     * @param rules 검사할 Rule Set
     * @param facts 실행에 필요한 데이터
     * @return 각 Rule의 평가 결과 Map
     */
    fun check(rules: RuleSet, facts: Facts): Map<Rule, Boolean>

    /**
     * [rules]를 실행합니다.
     *
     * @param rules 실행할 Rule Set
     * @param facts 실행에 필요한 데이터
     */
    fun fire(rules: RuleSet, facts: Facts)
}
