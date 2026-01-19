package io.bluetape4k.coroutines.flow.exceptions

/**
 * [kotlinx.coroutines.flow.Flow] 작업 시 더 이상 발행된 요소가 없을 때 발생하는 예외입니다.
 */
open class FlowNoElementException: FlowOperationException {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}
