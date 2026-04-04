package io.bluetape4k.rule.exception

/**
 * 잘못된 Rule 정의 시에 발생하는 예외
 */
class InvalidRuleDefinitionException: RuleException {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}
