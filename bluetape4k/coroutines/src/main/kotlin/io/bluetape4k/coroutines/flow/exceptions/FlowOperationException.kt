package io.bluetape4k.coroutines.flow.exceptions

import io.bluetape4k.exceptions.BluetapeException

/**
 * Flow 에서 발생하는 예외를 처리하기 위한 Exception 입니다.
 */
open class FlowOperationException: BluetapeException {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}
