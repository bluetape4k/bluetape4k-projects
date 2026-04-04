package io.bluetape4k.ranges

import io.bluetape4k.exceptions.BluetapeException

/**
 * Range가 유효하지 않을 때 발생하는 예외
 *
 * 예제:
 * ```kotlin
 * // 잘못된 범위 검사 시 직접 던지는 예시
 * fun requireValidRange(start: Int, end: Int) {
 *     if (start > end) throw InvalidRangeException("start($start) > end($end)")
 * }
 * // requireValidRange(5, 1) -> InvalidRangeException: start(5) > end(1)
 * ```
 */
open class InvalidRangeException: BluetapeException {
    constructor(): super()
    constructor(msg: String): super(msg)
    constructor(msg: String, cause: Throwable?): super(msg, cause)
    constructor(cause: Throwable?): super(cause)
}
