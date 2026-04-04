package io.bluetape4k.rule.exception

/**
 * Rule Engine의 기본 예외 클래스입니다.
 */
open class RuleException: RuntimeException {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}
