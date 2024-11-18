package io.bluetape4k.exceptions

/**
 * Bluetape4k 라이브러리의 기본 예외 클래스입니다.
 */
open class BluetapeException: RuntimeException {
    constructor(): super()
    constructor(msg: String): super(msg)
    constructor(msg: String, cause: Throwable?): super(msg, cause)
    constructor(cause: Throwable?): super(cause)
}
