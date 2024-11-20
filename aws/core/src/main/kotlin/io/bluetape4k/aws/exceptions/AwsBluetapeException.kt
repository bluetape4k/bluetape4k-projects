package io.bluetape4k.aws.exceptions

import io.bluetape4k.exceptions.BluetapeException

/**
 * AWS 관련 예외를 처리하기 위한 최상위 예외 클래스입니다.
 */
open class AwsBluetapeException: BluetapeException {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
    constructor(cause: Throwable): super(cause)
}
