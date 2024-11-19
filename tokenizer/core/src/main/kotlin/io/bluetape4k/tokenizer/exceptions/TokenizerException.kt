package io.bluetape4k.tokenizer.exceptions

import io.bluetape4k.exceptions.BluetapeException

/**
 * Tokenizer 모듈의 기본 예외
 */
open class TokenizerException: BluetapeException {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}
