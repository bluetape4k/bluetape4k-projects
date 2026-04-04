package io.bluetape4k.rule.exception

/**
 * Rule Engine의 기본 예외 클래스입니다.
 *
 * ```kotlin
 * try {
 *     engine.fire(ruleSet, facts)
 * } catch (e: RuleException) {
 *     println("Rule 실행 중 오류: ${e.message}")
 * }
 * ```
 */
open class RuleException: RuntimeException {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}
