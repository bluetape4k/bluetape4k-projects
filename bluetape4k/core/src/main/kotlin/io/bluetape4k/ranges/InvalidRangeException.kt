package io.bluetape4k.ranges

import io.bluetape4k.exceptions.BluetapeException

/**
 * Range가 유효하지 않을 때 발생하는 예외
 */
open class InvalidRangeException: BluetapeException {
    constructor(): super()
    constructor(msg: String): super(msg)
    constructor(msg: String, cause: Throwable?): super(msg, cause)
    constructor(cause: Throwable?): super(cause)
}
