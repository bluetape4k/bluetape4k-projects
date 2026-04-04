package io.bluetape4k.rule.api

/**
 * Rule Engine의 실행 이벤트를 수신하는 리스너입니다.
 *
 * @see RuleEngine
 */
interface RuleEngineListener {

    /**
     * Rule 평가 시작 전에 호출됩니다.
     *
     * @param rules 평가할 Rule 집합
     * @param facts 데이터
     */
    fun beforeEvaluate(rules: Iterable<Rule>, facts: Facts) {}

    /**
     * 모든 Rule 실행 완료 후에 호출됩니다.
     *
     * @param rules 실행된 Rule 집합
     * @param facts 데이터
     */
    fun afterExecute(rules: Iterable<Rule>, facts: Facts) {}
}
