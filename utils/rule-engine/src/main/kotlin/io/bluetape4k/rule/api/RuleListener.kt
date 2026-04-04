package io.bluetape4k.rule.api

/**
 * Rule 평가/실행 시 발생하는 이벤트를 수신하는 리스너입니다.
 *
 * @see Rule
 * @see RuleEngine
 */
interface RuleListener {

    /**
     * Rule 평가 전에 호출됩니다.
     *
     * @param rule 평가할 Rule
     * @param facts 데이터
     * @return false를 반환하면 해당 Rule의 평가를 건너뜁니다.
     */
    fun beforeEvaluate(rule: Rule, facts: Facts): Boolean = true

    /**
     * Rule 평가 후에 호출됩니다.
     *
     * @param rule 평가된 Rule
     * @param facts 데이터
     * @param evaluationResult 평가 결과
     */
    fun afterEvaluate(rule: Rule, facts: Facts, evaluationResult: Boolean) {}

    /**
     * Rule 실행 전에 호출됩니다.
     *
     * @param rule 실행할 Rule
     * @param facts 데이터
     */
    fun beforeExecute(rule: Rule, facts: Facts) {}

    /**
     * Rule 실행 후에 호출됩니다.
     *
     * @param rule 실행된 Rule
     * @param facts 데이터
     * @param exception 실행 중 발생한 예외 (없으면 null)
     */
    fun afterExecute(rule: Rule, facts: Facts, exception: Throwable? = null) {}
}
