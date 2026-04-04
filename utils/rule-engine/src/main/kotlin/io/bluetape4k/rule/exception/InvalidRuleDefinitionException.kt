package io.bluetape4k.rule.exception

/**
 * 잘못된 Rule 정의 시에 발생하는 예외
 *
 * ```kotlin
 * // @Condition 어노테이션이 없는 경우 등 Rule 정의가 잘못되면 발생합니다.
 * try {
 *     val rule = BadRule().asRule()
 * } catch (e: InvalidRuleDefinitionException) {
 *     println("잘못된 Rule 정의: ${e.message}")
 * }
 * ```
 */
class InvalidRuleDefinitionException: RuleException {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}
