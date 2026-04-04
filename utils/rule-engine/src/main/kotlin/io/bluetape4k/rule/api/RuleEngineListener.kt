package io.bluetape4k.rule.api

/**
 * Rule Engine의 실행 이벤트를 수신하는 리스너입니다.
 *
 * ```kotlin
 * val listener = object : RuleEngineListener {
 *     override fun beforeEvaluate(rules: Iterable<Rule>, facts: Facts) {
 *         println("Engine starting with ${rules.count()} rules")
 *     }
 *     override fun afterExecute(rules: Iterable<Rule>, facts: Facts) {
 *         println("Engine finished")
 *     }
 * }
 * val engine = DefaultRuleEngine()
 * engine.registerRuleEngineListener(listener)
 * ```
 *
 * @see RuleEngine
 */
interface RuleEngineListener {

    /**
     * Rule 평가 시작 전에 호출됩니다.
     *
     * ```kotlin
     * override fun beforeEvaluate(rules: Iterable<Rule>, facts: Facts) {
     *     println("About to evaluate ${rules.count()} rules")
     * }
     * ```
     *
     * @param rules 평가할 Rule 집합
     * @param facts 데이터
     */
    fun beforeEvaluate(rules: Iterable<Rule>, facts: Facts) {}

    /**
     * 모든 Rule 실행 완료 후에 호출됩니다.
     *
     * ```kotlin
     * override fun afterExecute(rules: Iterable<Rule>, facts: Facts) {
     *     println("All rules executed")
     * }
     * ```
     *
     * @param rules 실행된 Rule 집합
     * @param facts 데이터
     */
    fun afterExecute(rules: Iterable<Rule>, facts: Facts) {}
}
