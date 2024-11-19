package io.bluetape4k.tokenizer.exceptions

/**
 * Tokenizing 관련 요청이 잘못되었을 때 발생하는 예외
 */
open class InvalidTokenizeRequestException: TokenizerException {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}
