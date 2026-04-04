package io.bluetape4k.rule.api

/**
 * Rule 평가/실행 시 발생하는 이벤트를 수신하는 리스너입니다.
 *
 * ```kotlin
 * val listener = object : RuleListener {
 *     override fun beforeEvaluate(rule: Rule, facts: Facts): Boolean {
 *         println("Evaluating rule: ${rule.name}")
 *         return true
 *     }
 *     override fun afterExecute(rule: Rule, facts: Facts, exception: Throwable?) {
 *         if (exception == null) println("Rule ${rule.name} succeeded")
 *         else println("Rule ${rule.name} failed: ${exception.message}")
 *     }
 * }
 * val engine = DefaultRuleEngine()
 * engine.registerRuleListener(listener)
 * ```
 *
 * @see Rule
 * @see RuleEngine
 */
interface RuleListener {

    /**
     * Rule 평가 전에 호출됩니다.
     *
     * ```kotlin
     * override fun beforeEvaluate(rule: Rule, facts: Facts): Boolean {
     *     // 특정 Rule은 건너뜀
     *     return rule.name != "skipMe"
     * }
     * ```
     *
     * @param rule 평가할 Rule
     * @param facts 데이터
     * @return false를 반환하면 해당 Rule의 평가를 건너뜁니다.
     */
    fun beforeEvaluate(rule: Rule, facts: Facts): Boolean = true

    /**
     * Rule 평가 후에 호출됩니다.
     *
     * ```kotlin
     * override fun afterEvaluate(rule: Rule, facts: Facts, evaluationResult: Boolean) {
     *     println("Rule ${rule.name} evaluated to $evaluationResult")
     * }
     * ```
     *
     * @param rule 평가된 Rule
     * @param facts 데이터
     * @param evaluationResult 평가 결과
     */
    fun afterEvaluate(rule: Rule, facts: Facts, evaluationResult: Boolean) {}

    /**
     * Rule 실행 전에 호출됩니다.
     *
     * ```kotlin
     * override fun beforeExecute(rule: Rule, facts: Facts) {
     *     println("Executing rule: ${rule.name}")
     * }
     * ```
     *
     * @param rule 실행할 Rule
     * @param facts 데이터
     */
    fun beforeExecute(rule: Rule, facts: Facts) {}

    /**
     * Rule 실행 후에 호출됩니다.
     *
     * ```kotlin
     * override fun afterExecute(rule: Rule, facts: Facts, exception: Throwable?) {
     *     if (exception != null) println("Rule ${rule.name} threw: ${exception.message}")
     * }
     * ```
     *
     * @param rule 실행된 Rule
     * @param facts 데이터
     * @param exception 실행 중 발생한 예외 (없으면 null)
     */
    fun afterExecute(rule: Rule, facts: Facts, exception: Throwable? = null) {}
}
