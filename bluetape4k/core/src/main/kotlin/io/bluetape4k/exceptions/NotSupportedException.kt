package io.bluetape4k.exceptions

/**
 * 지원하지 않는 기능을 호출했을 때 발생하는 예외입니다.
 */
open class NotSupportedException: KommonsException {
    constructor(): super()
    constructor(msg: String): super(msg)
    constructor(msg: String, cause: Throwable?): super(msg, cause)
    constructor(cause: Throwable?): super(cause)
}
