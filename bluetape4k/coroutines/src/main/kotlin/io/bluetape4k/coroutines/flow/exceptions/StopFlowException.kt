package io.bluetape4k.coroutines.flow.exceptions

import kotlinx.coroutines.CancellationException

/**
 * [kotlinx.coroutines.flow.Flow]를 강제로 중단시키는 예외입니다. ([CancellationException]을 상속 받습니다)
 *
 * @see kotlinx.coroutines.CancellationException
 */
@PublishedApi
internal class StopFlowException: CancellationException {
    constructor(): super()
    constructor(message: String?): super(message)
    constructor(message: String?, cause: Throwable?): super(message) {
        initCause(cause)
    }

    constructor(cause: Throwable?): super() {
        initCause(cause)
    }
}

/**
 * Flow를 강제로 중단시키는 예외입니다.
 */
@PublishedApi
@JvmField
internal val STOP = StopFlowException()
