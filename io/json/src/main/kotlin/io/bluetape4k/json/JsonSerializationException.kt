package io.bluetape4k.json

/**
 * JSON 직렬화/역직렬화 실패를 나타내는 예외입니다.
 */
open class JsonSerializationException: RuntimeException {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}
