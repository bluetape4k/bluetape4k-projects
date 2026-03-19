package io.bluetape4k.coroutines.flow.exceptions

import kotlinx.coroutines.CancellationException

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

@PublishedApi
@JvmField
internal val STOP = StopFlowException()
